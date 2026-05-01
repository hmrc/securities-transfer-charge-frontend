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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcConditionalRowValidatorSpec extends AnyWordSpec with Matchers {

  private implicit val cols: ColumnIndexBuilder = new ColumnIndexBuilder(Seq.empty)

  private val validParsedRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      sellerName = Some("Seller Ltd"),
      sellerAddressInUk = Some(true),
      sellerAddressLine1 = Some("1 Seller Street"),
      sellerAddressLine2 = None,
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = Some("AA1 1AA"),
      sellerCountry = None,
      connectedPersons = Some(false),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = None,
      securitiesTarget = Some("Target Ltd"),
      companyRegistrationNumber = Some("12345678"),
      chargingPoint = Some(LocalDate.of(2025, 11, 20)),
      taxRate = Some(BigDecimal("0.5")),
      whatTypeOfSecurities = Some("shares"),
      typeOfShares = Some("Ordinary Shares"),
      securitiesQuantity = Some(BigDecimal(100)),
      amountPaidForSecurities = Some(BigDecimal(1000)),
      totalMarketValue = None,
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = None,
      purchaseForCancellation = None
    )

  private val messagesApi: MessagesApi = stubMessagesApi(
    Map(
      "en" -> Map(
        "fileUpload.error.whatReliefAreYouApplyingFor.invalid" ->
          "Enter the name of the relief you are applying for. See a full list of reliefs (opens in new tab).",
        "fileUpload.error.typeOfShares.required" ->
          "If you are buying shares, enter the type of shares",
        "fileUpload.error.sellerAddressLine1.required" ->
          "Enter the first line of your address",
        "fileUpload.error.sellerAddressLine1.length" ->
          "Address line 1 must be 50 characters or fewer",
        "fileUpload.error.sellerAddressLine1.invalidCharacters" ->
          "Address line 1 can only include letters, numbers and the following characters: , . - '",
        "fileUpload.error.sellerAddressLine2.length" ->
          "Address line 2 must be fewer than 50 characters long",
        "fileUpload.error.sellerAddressLine2.invalidCharacters" ->
          "Address line 2 can only include letters, numbers and the following characters: , . - '",
        "fileUpload.error.sellerPostcode.required" ->
          "Enter a postcode",
        "fileUpload.error.sellerPostcode.invalid" ->
          "Enter a real postcode, like AA1 1AA",
        "fileUpload.error.sellerCountry.length" ->
          "Country must be 50 characters or fewer",
        "fileUpload.error.sellerCountry.invalidCharacters" ->
          "Country can only include letters, numbers and the following characters: , . - '",
        "totalMarketValue.error.required" ->
          "Enter the total market value of the securities",
        "fileUpload.error.totalMarketValue.maximum" ->
          "The market value of the securities must be £999,999,999 or below"
      )
    )
  )

  private val validator =
    new StcConditionalRowValidator(
      support = new StcValidationSupport,
      messagesApi = messagesApi
    )

  "StcConditionalRowValidator.validate STF" must {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.STF)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "reject invalid relief name when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = Some("Made Up Relief")
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "require type of shares when what type of securities is shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("shares"),
          typeOfShares = None
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "typeOfShares") mustBe true
    }

    "not require type of shares when security type is not shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("Loan notes"),
          typeOfShares = None
        ), StcTemplate.STF
      )

      result.map(_.fieldName) must not contain "typeOfShares"
    }

    "validate UK seller address line 1 required" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUk = Some(true),
          sellerAddressLine1 = None
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "sellerAddressLine1") mustBe true
    }

    "validate UK seller address line 1 invalid characters" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUk = Some(true),
          sellerAddressLine1 = Some("Address @@@")
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "sellerAddressLine1") mustBe true
    }

    "validate UK seller postcode required" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUk = Some(true),
          sellerPostcode = None
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate UK seller postcode invalid" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUk = Some(true),
          sellerPostcode = Some("not a postcode")
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate seller country when seller address is not in the UK" in {
      val longCountry = "a" * 51

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUk = Some(false),
          sellerCountry = Some(longCountry)
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "require total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = None
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject too large total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = Some(BigDecimal(1000000000))
        ), StcTemplate.STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }
  }

  "StcConditionalRowValidator.validate SH03" must {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.SH03)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.SH03
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }
  }
}