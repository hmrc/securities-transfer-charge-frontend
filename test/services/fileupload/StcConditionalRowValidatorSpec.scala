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

import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.MessagesApi
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload._
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadColumn
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcConditionalRowValidator
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcValidationSupport

import java.time.LocalDate

class StcConditionalRowValidatorSpec extends AnyWordSpec with Matchers {

  private val messagesApi: MessagesApi = stubMessagesApi(
    Map(
      "en" -> Map(
        "fileUpload.error.whatReliefAreYouApplyingFor.invalid" -> "Enter the name of the relief you are applying for. See a full list of reliefs (opens in new tab).",
        "fileUpload.error.typeOfShares.required" -> "If you are buying shares, enter the type of shares",
        "fileUpload.error.sellerAddressLine1.required" -> "Enter the first line of your address",
        "fileUpload.error.sellerAddressLine1.length" -> "Address line 1 must be 50 characters or fewer",
        "fileUpload.error.sellerAddressLine1.invalidCharacters" -> "Address line 1 can only include letters, numbers and the following characters: , . - '",
        "fileUpload.error.sellerAddressLine2.length" -> "Address line 2 must be fewer than 50 characters long",
        "fileUpload.error.sellerAddressLine2.invalidCharacters" -> "Address line 2 can only include letters, numbers and the following characters: , . - '",
        "fileUpload.error.sellerPostcode.required" -> "Enter a postcode",
        "fileUpload.error.sellerPostcode.invalid" -> "Enter a real postcode, like AA1 1AA",
        "fileUpload.error.sellerCountry.length" -> "Country must be 50 characters or fewer",
        "fileUpload.error.sellerCountry.invalidCharacters" -> "Country can only include letters, numbers and the following characters: , . - '",
        "totalMarketValue.error.required" -> "Enter the total market value of the securities",
        "fileUpload.error.totalMarketValue.nonNumeric" -> "The market value of the securities must be a number",
        "fileUpload.error.totalMarketValue.maximum" -> "The market value of the securities must be £999,999,999 or below"
      )
    )
  )

  private val validator =
    new StcConditionalRowValidator(
      support = new StcValidationSupport,
      messagesApi = messagesApi
    )

  "StcConditionalRowValidator.validate" must {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validRawRow, validParsedRow)

      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.whatReliefAreYouApplyingFor -> ""),
        validParsedRow.copy(
          applyingForRelief = ParsedValue.Valid(true),
          whatReliefAreYouApplyingFor = ParsedValue.Missing
        )
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "reject invalid relief name when applying for relief is yes" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.whatReliefAreYouApplyingFor -> "Made Up Relief"),
        validParsedRow.copy(
          applyingForRelief = ParsedValue.Valid(true),
          whatReliefAreYouApplyingFor = ParsedValue.Valid("Made Up Relief")
        )
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "require type of shares when what type of securities is shares" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.typeOfShares -> ""),
        validParsedRow.copy(
          whatTypeOfSecurities = ParsedValue.Valid("shares"),
          typeOfShares = ParsedValue.Missing
        )
      )

      result.exists(_.fieldName == "typeOfShares") mustBe true
    }

    "not require type of shares when security type is not shares" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.typeOfShares -> ""),
        validParsedRow.copy(
          whatTypeOfSecurities = ParsedValue.Valid("Loan notes or other debt securities"),
          typeOfShares = ParsedValue.Missing
        )
      )

      result.map(_.fieldName) must not contain "typeOfShares"
    }

    "validate UK seller address line 1" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerAddressLine1 -> "", StcUploadColumn.sellerPostcode -> "AA1 1AA"),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Valid(true),
          sellerAddressLine1 = ParsedValue.Missing,
          sellerPostcode = ParsedValue.Valid("AA1 1AA")
        )
      )

      result.exists(_.fieldName == "sellerAddressLine1") mustBe true
    }

    "validate UK seller address line 1 invalid characters" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerAddressLine1 -> "Address @@@", StcUploadColumn.sellerPostcode -> "AA1 1AA"),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Valid(true),
          sellerAddressLine1 = ParsedValue.Valid("Address @@@"),
          sellerPostcode = ParsedValue.Valid("AA1 1AA")
        )
      )

      result.exists(_.fieldName == "sellerAddressLine1") mustBe true
    }

    "validate UK seller postcode required" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerAddressLine1 -> "1 Seller Street", StcUploadColumn.sellerPostcode -> ""),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Valid(true),
          sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
          sellerPostcode = ParsedValue.Missing
        )
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate UK seller postcode invalid" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerAddressLine1 -> "1 Seller Street", StcUploadColumn.sellerPostcode -> "not a postcode"),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Valid(true),
          sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
          sellerPostcode = ParsedValue.Valid("not a postcode")
        )
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate seller country when seller address is not in the UK" in {
      val longCountry = "a" * 51

      val result = validator.validate(
        rawRow(StcUploadColumn.sellerCountry -> longCountry),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Valid(false),
          sellerCountry = ParsedValue.Valid(longCountry)
        )
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "require total market value when connected persons is yes" in {
      val result = validator.validate(
        rawRow(),
        validParsedRow.copy(
          connectedPersons = ParsedValue.Valid(true),
          totalMarketValue = ParsedValue.Missing
        )
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject non-numeric total market value when connected persons is yes" in {
      val result = validator.validate(
        rawRow(),
        validParsedRow.copy(
          connectedPersons = ParsedValue.Valid(true),
          totalMarketValue = ParsedValue.Invalid("abc", "not a number")
        )
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject too large total market value when connected persons is yes" in {
      val result = validator.validate(
        rawRow(),
        validParsedRow.copy(
          connectedPersons = ParsedValue.Valid(true),
          totalMarketValue = ParsedValue.Valid(BigDecimal(1000000000))
        )
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }
  }

  private def rawRow(values: (Int, String)*): ParsedRow =
    ParsedRow(
      rowNumber = 3,
      cells = values.map { case (index, value) => ParsedCell(index, value) }
    )

  private val validRawRow: ParsedRow =
    rawRow(
      StcUploadColumn.sellerAddressLine1 -> "1 Seller Street",
      StcUploadColumn.sellerPostcode -> "AA1 1AA",
      StcUploadColumn.typeOfShares -> "Ordinary Shares"
    )

  private val validParsedRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      sellerName = ParsedValue.Valid("Seller Ltd"),
      sellerAddressInUk = ParsedValue.Valid(true),
      sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
      sellerAddressLine2 = ParsedValue.Missing,
      sellerAddressLine3 = ParsedValue.Missing,
      sellerAddressLine4 = ParsedValue.Missing,
      sellerPostcode = ParsedValue.Valid("AA1 1AA"),
      sellerCountry = ParsedValue.Missing,
      connectedPersons = ParsedValue.Valid(false),
      applyingForRelief = ParsedValue.Valid(false),
      whatReliefAreYouApplyingFor = ParsedValue.Missing,
      securitiesTarget = ParsedValue.Valid("Target Ltd"),
      companyRegistrationNumber = ParsedValue.Valid("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
      taxRate = ParsedValue.Valid(BigDecimal("0.5")),
      whatTypeOfSecurities = ParsedValue.Valid("shares"),
      typeOfShares = ParsedValue.Valid("Ordinary Shares"),
      securitiesQuantity = ParsedValue.Valid(BigDecimal(100)),
      amountPaidForSecurities = ParsedValue.Valid(BigDecimal("1000")),
      totalMarketValue = ParsedValue.Missing
    )
}