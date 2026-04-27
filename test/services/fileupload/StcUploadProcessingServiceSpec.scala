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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedStcRow, ParsedValue, StcFileValidationResponse, UploadedFile, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.time.LocalDate

class StcUploadProcessingServiceSpec extends AnyWordSpec with Matchers with EitherValues with MockitoSugar {

  private val stcUploadParsingService = mock[StcUploadParsingService]
  private val stcFileValidationService = mock[StcFileValidationService]

  private val service = new StcUploadProcessingService(
    stcUploadParsingService,
    stcFileValidationService
  )

  private val uploadedFile = UploadedFile(
    fileName = "test.xlsx",
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
    typeOfShares = ParsedValue.Missing,
    securitiesQuantity = ParsedValue.Valid(BigDecimal("100")),
    amountPaidForSecurities = ParsedValue.Valid(BigDecimal("500")),
    totalMarketValue = ParsedValue.Valid(BigDecimal("600"))
  )

  "process" should {

    "parse the file and validate the parsed rows" in {
      val validationResponse = StcFileValidationResponse(
        rows = Seq(ValidatedStcRow(parsedRow, Seq.empty))
      )

      when(stcUploadParsingService.parse(any[UploadedFile])).thenReturn(Right(Seq(parsedRow)))
      when(stcFileValidationService.validate(Seq(parsedRow))).thenReturn(validationResponse)

      service.process(uploadedFile).value shouldBe validationResponse
    }

    "return the parse error if parsing fails" in {
      val parseError = FileParseError.UnsupportedMimeType("application/pdf")

      when(stcUploadParsingService.parse(any[UploadedFile])).thenReturn(Left(parseError))

      service.process(uploadedFile).left.value shouldBe parseError
    }
  }
}