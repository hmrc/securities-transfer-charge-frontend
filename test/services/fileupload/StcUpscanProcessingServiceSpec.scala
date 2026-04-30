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

import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{verify, when}
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanCallbackRequest, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.{Instant, LocalDate}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class StcUpscanProcessingServiceSpec extends AnyWordSpec with Matchers with EitherValues with ScalaFutures with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val upscanFileDownloadService = mock[UpscanFileDownloadService]
  private val stcUploadProcessingService = mock[StcUploadProcessingService]

  private val service = new StcUpscanProcessingServiceImpl(
    upscanFileDownloadService,
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

  private val uploadedFile = UploadedFile(
    fileName = "bulk-upload.xlsx",
    mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    inputStream = new ByteArrayInputStream("irrelevant".getBytes(StandardCharsets.UTF_8))
  )

  private val parsedRow = ParsedStcRow(
    rowNumber = 1,
    sellerName = Some("Acme Ltd"),
    sellerAddressInUk = Some(true),
    sellerAddressLine1 = Some("123 High Street"),
    sellerAddressLine2 = Some("Suite 1"),
    sellerAddressLine3 = None,
    sellerAddressLine4 = None,
    sellerPostcode = Some("SW1A 1AA"),
    sellerCountry = None,
    connectedPersons = Some(false),
    applyingForRelief = Some(false),
    whatReliefAreYouApplyingFor = None,
    securitiesTarget = Some("Target Corp"),
    companyRegistrationNumber = Some("12345678"),
    chargingPoint = Some(LocalDate.of(2024, 1, 1)),
    taxRate = Some(BigDecimal("0.5")),
    whatTypeOfSecurities = Some("shares"),
    typeOfShares = Some("ordinary"),
    securitiesQuantity = Some(BigDecimal(100)),
    amountPaidForSecurities = Some(BigDecimal(1000)),
    totalMarketValue = Some(BigDecimal(1000)),
    minSharePrice = Some(BigDecimal(10)),
    maxSharePrice = Some(BigDecimal(20)),
    sharePurchaseReason = Some("Investment"),
    purchaseForCancellation = Some(true)
  )

  private val validationResponse = StcFileValidationResponse(
    rows = Seq(ValidatedStcRow(parsedRow, Seq.empty))
  )

  "process" should {

    "download and process a ready upload successfully" in {
      when(upscanFileDownloadService.toUploadedFile(eqTo(fileUpload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(uploadedFile))
      when(stcUploadProcessingService.process(eqTo(uploadedFile)))
        .thenReturn(Right(validationResponse))

      val result = service.process(fileUpload).futureValue

      result.value shouldBe validationResponse

      verify(upscanFileDownloadService).toUploadedFile(eqTo(fileUpload))(any[HeaderCarrier])
      verify(stcUploadProcessingService).process(eqTo(uploadedFile))
    }

    "return the parse error when processing fails" in {
      val parseError = FileParseError.UnsupportedMimeType("application/pdf")

      when(upscanFileDownloadService.toUploadedFile(eqTo(fileUpload))(any[HeaderCarrier]))
        .thenReturn(Future.successful(uploadedFile))
      when(stcUploadProcessingService.process(eqTo(uploadedFile)))
        .thenReturn(Left(parseError))

      val result = service.process(fileUpload).futureValue

      result.left.value shouldBe parseError
    }

    "fail when the upload status is not Ready" in {
      val initiatedUpload = fileUpload.copy(status = UpscanJourneyStatus.Initiated)

      val exception = service.process(initiatedUpload).failed.futureValue

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage shouldBe "Cannot process upload unless status is Ready. Current status: Initiated"
    }

    "fail when the upload status is Failed" in {
      val failedUpload = fileUpload.copy(status = UpscanJourneyStatus.Failed)

      val exception = service.process(failedUpload).failed.futureValue

      exception shouldBe a[IllegalArgumentException]
      exception.getMessage shouldBe "Cannot process upload unless status is Ready. Current status: Failed"
    }
  }
}