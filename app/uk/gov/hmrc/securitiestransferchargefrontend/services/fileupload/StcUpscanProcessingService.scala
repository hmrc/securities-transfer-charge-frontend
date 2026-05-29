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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, StcFileValidationResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

trait StcUpscanProcessingService {
  def process(fileUpload: FileUpload, affinityKey:String)(implicit hc: HeaderCarrier): Future[Either[FileParseError, StcFileValidationResponse]]
}

@Singleton
class StcUpscanProcessingServiceImpl @Inject()(
                                                upscanFileDownloadService: UpscanFileDownloadService,
                                                stcUploadProcessingService: StcUploadProcessingService
                                              )(implicit ec: ExecutionContext) extends StcUpscanProcessingService {

  override def process(fileUpload: FileUpload,affinityKey:String)(implicit hc: HeaderCarrier): Future[Either[FileParseError, StcFileValidationResponse]] =
    if (fileUpload.status != UpscanJourneyStatus.Ready) {
      Future.failed(
        new IllegalArgumentException(
          s"Cannot process upload unless status is Ready. Current status: ${fileUpload.status}"
        )
      )
    } else {
      upscanFileDownloadService
        .toUploadedFile(fileUpload)
        .map(file => stcUploadProcessingService.process(file,affinityKey))
    }
}