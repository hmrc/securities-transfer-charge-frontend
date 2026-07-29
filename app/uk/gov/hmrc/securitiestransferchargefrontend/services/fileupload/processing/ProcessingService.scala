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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.FileParseError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanJourneyStatus.{Completed, EmptyFile, FormatingErrors, InvalidTemplate, Processing, RowLimitExceeded, TooManyErrors, UpscanDownloadError}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{UpscanJourneyRepository, ValidationErrorRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ProcessingService @Inject()(
                                   stcUpscanProcessingService: StcUpscanProcessingService,
                                   validationErrorRepository: ValidationErrorRepository,
                                   upscanJourneyRepository: UpscanJourneyRepository,
                                   subscriptionConnector: SubscriptionConnector
                                 ) {

  def processReadyUpload(
                          reference: String,
                          fileUpload: FileUpload,
                          affinityKey: String,
                          journeyType: JourneyType
                        )(implicit request: StcAuthorisedRequest[_], hc: HeaderCarrier, ec: ExecutionContext): Future[Unit] =
    upscanJourneyRepository.updateStatus(reference, Processing).flatMap { _ =>

      stcUpscanProcessingService.process(fileUpload, affinityKey, journeyType).flatMap {

        case Left(_: FileParseError.RowLimitExceeded) =>
          upscanJourneyRepository.updateStatus(reference, RowLimitExceeded)

        case Left(FileParseError.EmptyFile) =>
          upscanJourneyRepository.updateStatus(reference, EmptyFile)

        case Left(FileParseError.InvalidTemplate) =>
          upscanJourneyRepository.updateStatus(reference, InvalidTemplate)

        case Left(_: FileParseError) =>
          upscanJourneyRepository.updateStatus(reference, UpscanJourneyStatus.FileParseError)

        case Right(validationResponse) if validationResponse.tooManyBlockingErrors =>
          upscanJourneyRepository.updateStatus(reference, TooManyErrors)

        case Right(validationResponse) if validationResponse.hasBlockingErrors =>

          for {
            _ <- validationErrorRepository.save(reference, validationResponse.blockingErrors)
            _ <- upscanJourneyRepository.updateStatus(reference, FormatingErrors)
          } yield ()

        case Right(_) =>
          for {
            _ <- subscriptionConnector.getAndStoreSubscription(request.subscriptionId)
            _ <- upscanJourneyRepository.updateStatus(reference, Completed)
          } yield ()

      }.recoverWith {
        case _: UpscanDownloadException =>
          upscanJourneyRepository.updateStatus(reference, UpscanDownloadError)
      }
    }
}