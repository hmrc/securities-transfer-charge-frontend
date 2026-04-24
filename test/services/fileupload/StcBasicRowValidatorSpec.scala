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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals._
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload._
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadColumn
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcBasicRowValidator
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcValidationSupport

import java.time.LocalDate

class StcBasicRowValidatorSpec extends AnyWordSpec with Matchers {

  private val messagesApi: MessagesApi = stubMessagesApi(
    Map(
      "en" -> Map(
        "nameOfSeller.error.required" -> "Enter the seller's full name",
        "nameOfSeller.error.length" -> "Seller's name must be 35 characters or less",
        "fileUpload.error.sellerAddressInUk.invalid" -> "Enter ‘yes’ if the seller lives in the UK, or ‘no’ if the seller does not live in the UK",
        "fileUpload.error.connectedPersons.invalid" -> "Enter ‘yes’ if you and the buyer are connected persons",
        "fileUpload.error.applyingForRelief.invalid" -> "Enter ‘yes’ if you are applying for a relief, or ‘no’ if you are not applying for a relief",
        "securitiesTarget.error.businessName.required" -> "Enter the name of the business you're buying securities in",
        "securitiesTarget.error.businessName.length" -> "Business name must be 160 characters or fewer",
        "securitiesTarget.error.crn.length" -> "Company Reference Number must be 8 characters or fewer",
        "chargingPoint.error.required.all" -> "Enter the date you bought the securities",
        "chargingPoint.error.invalid" -> "The date you bought the securities must be a real date",
        "chargingPoint.error.futureDate" -> "The day you bought the securities must be today's date or a date in the past",
        "fileUpload.error.chargingPoint.invalidCharacters" -> "The date you bought the securities can only contain numbers and letters",
        "fileUpload.error.taxRate.invalid" -> "Enter a tax rate of ‘0.5%’ or ‘1.5%’",
        "fileUpload.error.whatTypeOfSecurities.required" -> "Enter the type of securities you are buying",
        "fileUpload.error.typeOfShares.required" -> "If you are buying shares, enter the type of shares",
        "fileUpload.error.securitiesQuantity.required" -> "Enter the number of shares you are buying",
        "fileUpload.error.securitiesQuantity.nonNumeric" -> "The amount of shares you are buying must be a number",
        "fileUpload.error.securitiesQuantity.minimum" -> "The number of shares must be at least 1",
        "fileUpload.error.securitiesQuantity.maximum" -> "The number of shares you are buying must be below 999,999,999",
        "amountPaidForSecurities.error.required" -> "Enter the amount you paid for the securities",
        "fileUpload.error.amountPaidForSecurities.nonNumeric" -> "The amount you paid for the securities must be a number",
        "fileUpload.error.amountPaidForSecurities.maximum" -> "The amount you paid for the securities must be £999,999,999 or below"
      )
    )
  )

  private val validator =
    new StcBasicRowValidator(
      support = new StcValidationSupport,
      messagesApi = messagesApi,
      chargingPointFormProvider = new ChargingPointFormProvider,
      nameOfSellerFormProvider = new NameOfSellerFormProvider,
      securitiesTargetFormProvider = new SecuritiesTargetFormProvider
    )

  "StcBasicRowValidator.validate" must {

    "return no errors for a valid basic row" in {
      val result = validator.validate(validRawRow, validParsedRow)

      result mustBe Seq.empty
    }

    "return seller name required error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerName -> ""),
        validParsedRow.copy(sellerName = ParsedValue.Missing)
      )

      result.exists(_.fieldName == "sellerName") mustBe true
    }

    "return seller address in uk invalid boolean error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.sellerAddressInUK -> "maybe"),
        validParsedRow.copy(
          sellerAddressInUk = ParsedValue.Invalid("maybe", "not a recognised boolean")
        )
      )

      result.exists(error =>
        error.fieldName == "sellerAddressInUk" &&
          error.message == "Enter ‘yes’ if the seller lives in the UK, or ‘no’ if the seller does not live in the UK"
      ) mustBe true
    }

    "return connected persons invalid boolean error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.connectedPersons -> "maybe"),
        validParsedRow.copy(
          connectedPersons = ParsedValue.Invalid("maybe", "not a recognised boolean")
        )
      )

      result.exists(error =>
        error.fieldName == "connectedPersons" &&
          error.message == "Enter ‘yes’ if you and the buyer are connected persons"
      ) mustBe true
    }

    "return applying for relief invalid boolean error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.applyingForRelief -> "maybe"),
        validParsedRow.copy(
          applyingForRelief = ParsedValue.Invalid("maybe", "not a recognised boolean")
        )
      )

      result.exists(error =>
        error.fieldName == "applyingForRelief" &&
          error.message == "Enter ‘yes’ if you are applying for a relief, or ‘no’ if you are not applying for a relief"
      ) mustBe true
    }

    "return charging point invalid characters error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.chargingPoint -> "20@11@2025"),
        validParsedRow
      )

      result.exists(error =>
        error.fieldName == "chargingPoint" &&
          error.message == "The date you bought the securities can only contain numbers and letters"
      ) mustBe true
    }

    "accept yyyy-MM-dd dates" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.chargingPoint -> "2025-11-20"),
        validParsedRow
      )

      result.map(_.fieldName) must not contain "chargingPoint"
    }

    "return invalid tax rate error" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.taxRate -> "2%"),
        validParsedRow
      )

      result.exists(_.fieldName == "taxRate") mustBe true
    }

    "return what type of securities required error when blank" in {
      val result = validator.validate(
        rawRow(StcUploadColumn.whatTypeOfSecurities -> ""),
        validParsedRow
      )

      result.exists(_.fieldName == "whatTypeOfSecurities") mustBe true
    }

    "return securities quantity required error when missing" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(securitiesQuantity = ParsedValue.Missing))

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity non-numeric error when invalid" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(securitiesQuantity = ParsedValue.Invalid("abc", "not a number")))

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity minimum error when zero" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(securitiesQuantity = ParsedValue.Valid(BigDecimal(0))))

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity maximum error when at or above max" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(securitiesQuantity = ParsedValue.Valid(BigDecimal(999999999))))

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return amount paid required error when missing" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(amountPaidForSecurities = ParsedValue.Missing))

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }

    "return amount paid non-numeric error when invalid" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(amountPaidForSecurities = ParsedValue.Invalid("abc", "not a number")))

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }

    "return amount paid maximum error when too large" in {
      val result = validator.validate(validRawRow, validParsedRow.copy(amountPaidForSecurities = ParsedValue.Valid(BigDecimal(1000000000))))

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }
  }

  private def rawRow(values: (Int, String)*): ParsedRow =
    ParsedRow(
      rowNumber = 3,
      cells = values.map { case (index, value) => ParsedCell(index, value) }
    )

  private val validRawRow: ParsedRow =
    rawRow(
      StcUploadColumn.sellerName -> "Seller Ltd",
      StcUploadColumn.sellerAddressInUK -> "yes",
      StcUploadColumn.sellerAddressLine1 -> "1 Test",
      StcUploadColumn.sellerAddressLine2 -> "Test Region",
      StcUploadColumn.sellerPostcode -> "AA1 1AA",
      StcUploadColumn.sellerCountry -> "UK",
      StcUploadColumn.connectedPersons -> "yes",
      StcUploadColumn.applyingForRelief -> "no",
      StcUploadColumn.securitiesTarget -> "Target Ltd",
      StcUploadColumn.whatIsCRN -> "12345678",
      StcUploadColumn.chargingPoint -> "20/11/2025",
      StcUploadColumn.taxRate -> "0.5%",
      StcUploadColumn.whatTypeOfSecurities -> "Shares",
      StcUploadColumn.typeOfShares -> "Ordinary",
      StcUploadColumn.securitiesQuantity -> "10000",
      StcUploadColumn.amountPaidForSecurities -> "£15000",
      StcUploadColumn.totalMarketValue -> "£20000"
    )

  private val validParsedRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      sellerName = ParsedValue.Valid("Seller Ltd"),
      sellerAddressInUk = ParsedValue.Valid(true),
      sellerAddressLine1 = ParsedValue.Valid("1 Test"),
      sellerAddressLine2 = ParsedValue.Valid("Test Region"),
      sellerAddressLine3 = ParsedValue.Missing,
      sellerAddressLine4 = ParsedValue.Missing,
      sellerPostcode = ParsedValue.Valid("AA1 1AA"),
      sellerCountry = ParsedValue.Missing,
      connectedPersons = ParsedValue.Valid(true),
      applyingForRelief = ParsedValue.Valid(false),
      whatReliefAreYouApplyingFor = ParsedValue.Missing,
      securitiesTarget = ParsedValue.Valid("Target Ltd"),
      companyRegistrationNumber = ParsedValue.Valid("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
      taxRate = ParsedValue.Valid(BigDecimal("0.5")),
      whatTypeOfSecurities = ParsedValue.Valid("Shares"),
      typeOfShares = ParsedValue.Valid("Ordinary"),
      securitiesQuantity = ParsedValue.Valid(BigDecimal(10000)),
      amountPaidForSecurities = ParsedValue.Valid(BigDecimal(15000)),
      totalMarketValue = ParsedValue.Valid(BigDecimal(20000))
    )
}