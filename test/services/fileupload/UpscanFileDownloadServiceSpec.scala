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

import org.mockito.ArgumentMatchers.{any, eq => eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.UpscanFileDownloadConnector
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.UploadedFile
import uk.gov.hmrc.securitiestransferchargefrontend.models.upscan.{FileUpload, UpscanCallbackRequest, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.UpscanFileDownloadServiceImpl

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.Instant
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpscanFileDownloadServiceSpec extends AnyWordSpec with Matchers with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val upscanFileDownloadConnector = mock[UpscanFileDownloadConnector]

  private val service = new UpscanFileDownloadServiceImpl(
    upscanFileDownloadConnector
  )

  private val uploadDetails = UpscanCallbackRequest.UploadDetails(
    uploadTimestamp = Instant.parse("2026-03-24T10:15:30Z"),
    checksum = "abc123",
    fileMimeType = "text/csv",
    fileName = "bulk-upload.csv",
    size = 1234L
  )

  "toUploadedFile" should {

    "download the file and adapt it into an UploadedFile" in {
      val inputStream = new ByteArrayInputStream("header\nvalue".getBytes(StandardCharsets.UTF_8))
      val downloadUrl = "https://example.com/download/ref-123"

      val fileUpload = FileUpload(
        reference = "ref-123",
        status = UpscanJourneyStatus.Ready,
        downloadUrl = Some(downloadUrl),
        uploadDetails = Some(uploadDetails)
      )

      when(upscanFileDownloadConnector.download(eqTo(downloadUrl))(any[HeaderCarrier]))
        .thenReturn(Future.successful(inputStream))

      val result = service.toUploadedFile(fileUpload).futureValue

      result shouldBe UploadedFile(
        fileName = "bulk-upload.csv",
        mimeType = "text/csv",
        inputStream = inputStream
      )

      verify(upscanFileDownloadConnector).download(eqTo(downloadUrl))(any[HeaderCarrier])
    }
  }
}