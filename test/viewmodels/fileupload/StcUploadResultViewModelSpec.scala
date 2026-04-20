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

package viewmodels.fileupload

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedValue, StcFileValidationResponse, StcRowValidationError, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload.{StcUploadResultViewModel, UploadErrorDisplay}

import java.time.LocalDate

class StcUploadResultViewModelSpec extends AnyWordSpec with Matchers {

  private val parsedRow = ParsedStcRow(
    rowNumber = 6,
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
    connectedPersons = ParsedValue.Valid(true),
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

  "from" should {

    "map blocking and non-blocking errors into display errors with spreadsheet cell references" in {
      val response = StcFileValidationResponse(
        rows = Seq(
          ValidatedStcRow(
            parsedRow = parsedRow,
            validationErrors = Seq(
              StcRowValidationError(
                rowNumber = 6,
                fieldName = "sellerName",
                message = "Enter the seller's name",
                blocking = true
              ),
              StcRowValidationError(
                rowNumber = 6,
                fieldName = "taxRate",
                message = "Enter a tax rate",
                blocking = false
              )
            )
          )
        )
      )

      val result = StcUploadResultViewModel.from(response)

      result.totalRows shouldBe 1
      result.hasErrors shouldBe true
      result.hasBlockingErrors shouldBe true
      result.validatedRows shouldBe response.rows

      result.blockingErrorDisplays shouldBe Seq(
        UploadErrorDisplay(
          cellReference = "H6",
          questionLabel = "Seller's name",
          message = "Enter the seller's name"
        )
      )

      result.nonBlockingErrorDisplays shouldBe Seq(
        UploadErrorDisplay(
          cellReference = "V6",
          questionLabel = "What is the tax rate for this transfer?",
          message = "Enter a tax rate"
        )
      )
    }

    "fall back gracefully when field metadata cannot be found" in {
      val response = StcFileValidationResponse(
        rows = Seq(
          ValidatedStcRow(
            parsedRow = parsedRow,
            validationErrors = Seq(
              StcRowValidationError(
                rowNumber = 12,
                fieldName = "unknownField",
                message = "Something went wrong",
                blocking = true
              )
            )
          )
        )
      )

      val result = StcUploadResultViewModel.from(response)

      result.blockingErrorDisplays shouldBe Seq(
        UploadErrorDisplay(
          cellReference = "Row 12",
          questionLabel = "unknownField",
          message = "Something went wrong"
        )
      )
    }

    "return empty error displays when there are no validation errors" in {
      val response = StcFileValidationResponse(
        rows = Seq(
          ValidatedStcRow(parsedRow, Seq.empty)
        )
      )

      val result = StcUploadResultViewModel.from(response)

      result.totalRows shouldBe 1
      result.hasErrors shouldBe false
      result.hasBlockingErrors shouldBe false
      result.blockingErrorDisplays shouldBe empty
      result.nonBlockingErrorDisplays shouldBe empty
    }
  }
}