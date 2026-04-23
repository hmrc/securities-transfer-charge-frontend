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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedCell, ParsedRow}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadColumn
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowValidationService
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowMapper
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcBasicRowValidator
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcConditionalRowValidator
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcValidationSupport

class StcRowValidationServiceSpec extends AnyWordSpec with Matchers {

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
        "fileUpload.error.amountPaidForSecurities.maximum" -> "The amount you paid for the securities must be £999,999,999 or below",
        "fileUpload.error.whatReliefAreYouApplyingFor.invalid" -> "Enter the name of the relief you are applying for. See a full list of reliefs (opens in new tab).",
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

  private val support = new StcValidationSupport
  private val service =
    new StcRowValidationService(
      stcRowMapper = new StcRowMapper,
      stcBasicRowValidator = new StcBasicRowValidator(
        support = support,
        messagesApi = messagesApi,
        chargingPointFormProvider = new ChargingPointFormProvider,
        nameOfSellerFormProvider = new NameOfSellerFormProvider,
        securitiesTargetFormProvider = new SecuritiesTargetFormProvider
      ),
      stcConditionalRowValidator = new StcConditionalRowValidator(
        support = support,
        messagesApi = messagesApi
      )
    )

  "StcRowValidationService.validate" must {

    "return no errors for a valid row" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(StcUploadColumn.sellerName, "Seller Ltd"),
          ParsedCell(StcUploadColumn.sellerAddressInUK, "yes"),
          ParsedCell(StcUploadColumn.sellerAddressLine1, "1 Seller Street"),
          ParsedCell(StcUploadColumn.sellerPostcode, "AA1 1AA"),
          ParsedCell(StcUploadColumn.connectedPersons, "yes"),
          ParsedCell(StcUploadColumn.applyingForRelief, "yes"),
          ParsedCell(StcUploadColumn.whatReliefAreYouApplyingFor, "Charities Relief"),
          ParsedCell(StcUploadColumn.securitiesTarget, "Target Ltd"),
          ParsedCell(StcUploadColumn.whatIsCRN, "12345678"),
          ParsedCell(StcUploadColumn.chargingPoint, "20/11/2025"),
          ParsedCell(StcUploadColumn.taxRate, "0.5%"),
          ParsedCell(StcUploadColumn.whatTypeOfSecurities, "shares"),
          ParsedCell(StcUploadColumn.typeOfShares, "Ordinary Shares"),
          ParsedCell(StcUploadColumn.securitiesQuantity, "100"),
          ParsedCell(StcUploadColumn.amountPaidForSecurities, "1000"),
          ParsedCell(StcUploadColumn.totalMarketValue, "1500")
        )
      )

      val result = service.validate(row)

      result.validationErrors mustBe Seq.empty
    }

    "combine basic and conditional validation errors" in {
      val row = ParsedRow(
        rowNumber = 3,
        cells = Seq(
          ParsedCell(StcUploadColumn.sellerName, ""),
          ParsedCell(StcUploadColumn.sellerAddressInUK, "yes"),
          ParsedCell(StcUploadColumn.connectedPersons, "yes"),
          ParsedCell(StcUploadColumn.applyingForRelief, "yes"),
          ParsedCell(StcUploadColumn.whatReliefAreYouApplyingFor, ""),
          ParsedCell(StcUploadColumn.securitiesTarget, "Target Ltd"),
          ParsedCell(StcUploadColumn.whatIsCRN, "12345678"),
          ParsedCell(StcUploadColumn.chargingPoint, "20/11/2025"),
          ParsedCell(StcUploadColumn.taxRate, "0.5%"),
          ParsedCell(StcUploadColumn.whatTypeOfSecurities, "shares"),
          ParsedCell(StcUploadColumn.typeOfShares, ""),
          ParsedCell(StcUploadColumn.securitiesQuantity, "100"),
          ParsedCell(StcUploadColumn.amountPaidForSecurities, "1000"),
          ParsedCell(StcUploadColumn.totalMarketValue, "")
        )
      )

      val result = service.validate(row)

      result.validationErrors.map(_.fieldName) must contain allOf (
        "sellerName",
        "whatReliefAreYouApplyingFor",
        "typeOfShares",
        "sellerAddressLine1",
        "sellerPostcode",
        "totalMarketValue"
      )
    }
  }
}