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

package services.fileupload

import base.SpecBase
import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanFileDownloadConnector
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanCallbackRequest, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.concurrent.Future

class StcUpscanProcessingServiceSpec extends SpecBase with EitherValues with MockitoSugar {

  private val upscanFileDownloadConnector = mock[UpscanFileDownloadConnector]
  private val stcUploadProcessingService = mock[StcUploadProcessingService]

  private val service = new StcUpscanProcessingServiceImpl(
    upscanFileDownloadConnector,
    stcUploadProcessingService
  )

  private val uploadDetails = UpscanCallbackRequest.UploadDetails(
    uploadTimestamp = Instant.parse("2026-03-24T10:15:30Z"),
    checksum = "abc123",
    fileMimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    fileName = "bulk-upload.xlsx",
    size = 1234L
  )

  private val fileUpload = FileUpload(
    reference = "ref-123",
    status = UpscanJourneyStatus.Ready,
    downloadUrl = Some("https://example.com/download/ref-123"),
    uploadDetails = Some(uploadDetails)
  )

  private val validationResponse = StcFileValidationResponse(
    rows = Seq.empty,
    maxErrorsAllowed = 25
  )

  "process" - {

    "download and process a ready upload successfully via the stream" in {
      val inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))

      when(upscanFileDownloadConnector.download(eqTo("https://example.com/download/ref-123"))(any[HeaderCarrier]))
        .thenReturn(Future.successful(inputStream))

      when(stcUploadProcessingService.process(any[UploadedFile], eqTo(affinityGroupKeyInd)))
        .thenReturn(Right(validationResponse))

      val result = service.process(fileUpload, affinityGroupKeyInd).futureValue

      result.value mustBe validationResponse

      verify(upscanFileDownloadConnector).download(eqTo("https://example.com/download/ref-123"))(any[HeaderCarrier])
      verify(stcUploadProcessingService).process(any[UploadedFile], eqTo(affinityGroupKeyInd))
    }

    "return the parse error when the stream parsing fails" in {
      val parseError = FileParseError.UnsupportedMimeType("application/pdf")
      val inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))

      when(upscanFileDownloadConnector.download(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(inputStream))

      when(stcUploadProcessingService.process(any[UploadedFile], eqTo(affinityGroupKeyInd)))
        .thenReturn(Left(parseError))

      val result = service.process(fileUpload, affinityGroupKeyInd).futureValue

      result.left.value mustBe parseError
    }

    "fail when the upload status is not Ready" in {
      val initiatedUpload = fileUpload.copy(status = UpscanJourneyStatus.Initiated)

      val exception = service.process(initiatedUpload, affinityGroupKeyInd).failed.futureValue

      exception mustBe a[IllegalArgumentException]
      exception.getMessage mustBe "Cannot process upload unless status is Ready. Current status: Initiated"
    }

    "fail when the upload status is Failed" in {
      val failedUpload = fileUpload.copy(status = UpscanJourneyStatus.Failed)

      val exception = service.process(failedUpload, affinityGroupKeyInd).failed.futureValue

      exception mustBe a[IllegalArgumentException]
      exception.getMessage mustBe "Cannot process upload unless status is Ready. Current status: Failed"
    }
  }
}