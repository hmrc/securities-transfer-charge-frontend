/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnector, UpscanDownloadException}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthorisedRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.BulkUploadProcessedAuditModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, StcFileValidationResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus.{Completed, EmptyFile, FormatingErrors, InvalidTemplate, Processing, RowLimitExceeded, TooManyErrors, UpscanDownloadError}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{ParsedStcRowsRepository, UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProcessingService @Inject()(
                                   stcUpscanProcessingService: StcUpscanProcessingService,
                                   validationErrorRepository: ValidationErrorRepository,
                                   upscanJourneyRepository: UpscanJourneyRepository,
                                   subscriptionConnector: SubscriptionConnector,
                                   parsedStcRowsRepository:ParsedStcRowsRepository,
                                   auditService: AuditService
                                 ) {

  def processReadyUpload(
                          reference: String,
                          fileUpload: FileUpload,
                          affinityKey: String,
                          journeyType: JourneyType
                        )(implicit request: StcAuthorisedRequest[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =

    val fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse("")
    val fileType = getFileType(fileUpload)
    val fileSize = fileUpload.uploadDetails.map(_.size.toString).getOrElse("")

    def auditBulkUploadFailure(fileValidationTime: Long, errorType: String, volume: String): BulkUploadProcessedAuditModel =
      buildBulkUploadAuditModel(status = "Failure", fileValidationTime = fileValidationTime, errorType = Some(errorType), volume = Some(volume))

    def auditBulkUploadSuccess(fileValidationTime: Long, numberOfEntries: Int): BulkUploadProcessedAuditModel =
      buildBulkUploadAuditModel(status = "Success", fileValidationTime = fileValidationTime, numberOfEntries = Some(numberOfEntries))

    def buildBulkUploadAuditModel(status: String, fileValidationTime: Long, errorType: Option[String] = None, volume: Option[String] = None, numberOfEntries: Option[Int] = None): BulkUploadProcessedAuditModel =
      BulkUploadProcessedAuditModel(
        uploadJourney = journeyType.toString.toUpperCase,
        affinityGroup = request.affinityGroup,
        subscriptionId = request.subscriptionId,
        credentialId = request.credentialId,
        fileType = fileType,
        fileUploadStatus = status,
        fileSize = fileSize,
        fileValidationTime = fileValidationTime,
        fileName = fileName,
        fileReference = reference,
        errorType = errorType,
        volume = volume,
        numberOfEntries = numberOfEntries
      )

    upscanJourneyRepository.updateStatus(reference, Processing).flatMap { _ =>

      stcUpscanProcessingService.process(fileUpload, affinityKey, journeyType).flatMap {

        case Left((FileParseError.RowLimitExceeded(actual, max), validationTime)) =>
          auditService.audit(auditBulkUploadFailure(
            fileValidationTime = validationTime,
            errorType = s"rowLimitExceeded - $actual total rows, but only $max allowed",
            volume = "0"))
          upscanJourneyRepository.updateStatus(reference, RowLimitExceeded)

        case Left((FileParseError.EmptyFile, validationTime)) =>
          auditService.audit(auditBulkUploadFailure(fileValidationTime = validationTime, errorType = "emptyFile", volume = "0"))
          upscanJourneyRepository.updateStatus(reference, EmptyFile)

        case Left((FileParseError.InvalidTemplate, validationTime)) =>
          auditService.audit(auditBulkUploadFailure(fileValidationTime = validationTime, errorType = "invalidFileTemplate", volume = "0"))
          upscanJourneyRepository.updateStatus(reference, InvalidTemplate)

        case Left((_: FileParseError, validationTime)) =>
          auditService.audit(auditBulkUploadFailure(fileValidationTime = validationTime, errorType = "fileParsingError", volume = "0"))
          upscanJourneyRepository.updateStatus(reference, UpscanJourneyStatus.FileParseError)

        case Right((validationResponse, validationTime)) if validationResponse.tooManyBlockingErrors =>
          auditService.audit(auditBulkUploadFailure(fileValidationTime = validationTime, errorType = "multiple", volume = "moreThan25"))
          upscanJourneyRepository.updateStatus(reference, TooManyErrors)

        case Right((validationResponse, validationTime)) if validationResponse.hasBlockingErrors =>
          val numOfBlockingErrors = validationResponse.blockingErrors.size
          val errorType = numOfBlockingErrors match {
            case 1 => constructErrorTypeString(validationResponse)
            case _ => "multiple"
          }

          for {
            _ <- validationErrorRepository.save(reference, validationResponse.blockingErrors)
            _ <- upscanJourneyRepository.updateStatus(reference, FormatingErrors)
          } yield auditService.audit(auditBulkUploadFailure(fileValidationTime = validationTime, errorType = errorType, volume = numOfBlockingErrors.toString))

        case Right((stcFileValidationResponse, validationTime)) =>
          for {
            _ <- parsedStcRowsRepository.save(reference, stcFileValidationResponse.validRows, fileName)
            _ <- subscriptionConnector.getAndStoreSubscription(request.subscriptionId)
            _ <- upscanJourneyRepository.updateStatus(reference, Completed)
          } yield auditService.audit(auditBulkUploadSuccess(fileValidationTime = validationTime, numberOfEntries = stcFileValidationResponse.rows.size))

      }.recoverWith {
        case _: UpscanDownloadException =>
          upscanJourneyRepository.updateStatus(reference, UpscanDownloadError)
      }
    }

  private def constructErrorTypeString(validationResponse: StcFileValidationResponse): String = {
    val fieldName = validationResponse.blockingErrors.head.fieldName
    val message = validationResponse.blockingErrors.head.message

    s"$fieldName - $message"
  }

  private def getFileType(fileUpload: FileUpload): String = {
    fileUpload.uploadDetails.map(_.fileMimeType).get match {
      case "text/csv" => "csv"
      case "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" => "excel"
      case _ => ""
    }
  }

  }