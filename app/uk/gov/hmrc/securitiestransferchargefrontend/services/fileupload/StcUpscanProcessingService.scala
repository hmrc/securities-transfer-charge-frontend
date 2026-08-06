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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanFileDownloadConnector
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, StcFileValidationResponse, UploadedFile}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StcUpscanProcessingService {
  def process(fileUpload: FileUpload, affinityKey: String, journeyType: JourneyType)(implicit hc: HeaderCarrier): Future[Either[FileParseError, StcFileValidationResponse]]
}

@Singleton
class StcUpscanProcessingServiceImpl @Inject()(
                                                upscanFileDownloadConnector: UpscanFileDownloadConnector,
                                                stcUploadProcessingService: StcUploadProcessingService,
                                                fileParserSelector: FileParserSelector
                                              )(implicit ec: ExecutionContext) extends StcUpscanProcessingService {

  override def process(fileUpload: FileUpload, affinityKey: String, journeyType: JourneyType)(implicit hc: HeaderCarrier): Future[Either[FileParseError, StcFileValidationResponse]] = {
    if (fileUpload.status != UpscanJourneyStatus.Ready) {
      Future.failed(
        new IllegalArgumentException(
          s"Cannot process upload unless status is Ready. Current status: ${fileUpload.status}"
        )
      )
    } else {
      val mimeType = fileUpload.uploadDetails.map(_.fileMimeType).getOrElse("")

      fileParserSelector.select(mimeType) match {

        case Left(error) =>
          Future.successful(Left(error))

        case Right(_) =>
          val downloadUrl = fileUpload.downloadUrl.getOrElse(throw new RuntimeException("Missing download URL from Upscan payload"))
          val fileName = fileUpload.uploadDetails.map(_.fileName).getOrElse("unknown.xlsx")

          upscanFileDownloadConnector.download(downloadUrl).map { inputStream =>
            val uploadedFile = UploadedFile(
              fileName = fileName,
              mimeType = mimeType,
              inputStream = inputStream
            )

            stcUploadProcessingService.process(uploadedFile, affinityKey, journeyType)
          }
      }
    }
  }
}