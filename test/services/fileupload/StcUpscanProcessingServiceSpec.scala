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
import org.scalatest.EitherValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.upscan.{FileUpload, UpscanCallbackRequest, UpscanJourneyStatus}
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
    rowNumber = 3,
    addressLine1 = ParsedValue.Valid("10 Downing Street"),
    addressLine2 = ParsedValue.Missing,
    addressLine3 = ParsedValue.Missing,
    addressLine4 = ParsedValue.Missing,
    postcode = ParsedValue.Valid("SW1A 2AA"),
    country = ParsedValue.Valid("United Kingdom"),
    sellerName = ParsedValue.Valid("Bob Seller"),
    sellerAddressInUk = ParsedValue.Valid(true),
    sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
    sellerAddressLine2 = ParsedValue.Missing,
    sellerAddressLine3 = ParsedValue.Missing,
    sellerAddressLine4 = ParsedValue.Missing,
    sellerPostcode = ParsedValue.Valid("LS1 1AA"),
    sellerCountry = ParsedValue.Valid("United Kingdom"),
    connectedPersons = ParsedValue.Valid(false),
    applyingForRelief = ParsedValue.Valid(false),
    whatReliefAreYouApplyingFor = ParsedValue.Missing,
    securitiesTarget = ParsedValue.Missing,
    companyRegistrationNumber = ParsedValue.Missing,
    chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
    taxRate = ParsedValue.Valid(BigDecimal("0.5")),
    whatTypeOfSecurities = ParsedValue.Valid("Stock"),
    otherSecuritiesType = ParsedValue.Missing,
    securitiesQuantity = ParsedValue.Valid(BigDecimal("100")),
    amountPaidForSecurities = ParsedValue.Valid(BigDecimal("500")),
    totalMarketValue = ParsedValue.Valid(BigDecimal("600"))
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

      exception shouldBe a[IllegalStateException]
      exception.getMessage shouldBe "Cannot process upload unless status is Ready. Current status: Initiated"
    }

    "fail when the upload status is Failed" in {
      val failedUpload = fileUpload.copy(status = UpscanJourneyStatus.Failed)

      val exception = service.process(failedUpload).failed.futureValue

      exception shouldBe a[IllegalStateException]
      exception.getMessage shouldBe "Cannot process upload unless status is Ready. Current status: Failed"
    }
  }
}