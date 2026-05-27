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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedCell, ParsedRow}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

class StcRowValidationServiceSpec extends AnyWordSpec with Matchers {

  private val messagesApi: MessagesApi = stubMessagesApi(Map(
    "en" -> Map(
      "nameOfSeller.error.required" -> "Enter the seller's full name",
      "fileUpload.error.sellerAddressInUK.invalid" -> "Enter ‘yes’ if the seller lives in the UK, or ‘no’ if the seller does not live in the UK",
      "fileUpload.error.connectedPersons.invalid" -> "Enter ‘yes’ if you and the buyer are connected persons",
      "fileUpload.error.applyingForRelief.invalid" -> "Enter ‘yes’ if you are applying for a relief, or ‘no’ if you are not applying for a relief",
      "securitiesTarget.error.businessName.required" -> "Enter the name of the business you're buying securities in",
      "chargingPoint.error.required.all" -> "Enter the date you bought the securities",
      "fileUpload.error.taxRate.invalid" -> "Enter a tax rate of ‘0.5%’ or ‘1.5%’",
      "fileUpload.error.whatTypeOfSecurities.required" -> "Enter the type of securities you are buying",
      "fileUpload.error.securitiesQuantity.required" -> "Enter the number of shares you are buying",
      "fileUpload.error.securitiesQuantity.minimum" -> "The number of shares must be at least 1",
      "fileUpload.error.securitiesQuantity.maximum" -> "The number of shares you are buying must be below 999,999,999",
      "amountPaidForSecurities.error.required" -> "Enter the amount you paid for the securities",
      "fileUpload.error.sellerAddressLine1.required" -> "Enter the first line of your address",
      "fileUpload.error.sellerPostcode.required" -> "Enter a postcode",
      "fileUpload.error.totalMarketValue.maximum" -> "The market value of the securities must be £999,999,999 or below"
    )
  ))

  private val support = new StcValidationSupport

  private val service =
    new StcRowValidationService(
      stcBasicRowValidator = new StcBasicRowValidator(
        support = support,
        messagesApi = messagesApi,
        nameOfSellerFormProvider = new NameOfSellerFormProvider,
        securitiesTargetFormProvider = new SecuritiesTargetFormProvider
      ),
      stcConditionalRowValidator = new StcConditionalRowValidator(
        support = support,
        messagesApi = messagesApi
      )
    )



  private val headers: Seq[String] =
    Seq(
      "STF",
      StcColumns.sellerName,
      StcColumns.sellerAddressInUK,
      StcColumns.sellerAddressLine1,
      StcColumns.sellerPostcode,
      StcColumns.connectedPersons,
      StcColumns.applyingForRelief,
      StcColumns.whatRelief,
      StcColumns.securitiesTarget,
      StcColumns.companyRegistrationNumber,
      StcColumns.chargingPoint,
      StcColumns.taxRate,
      StcColumns.whatTypeOfSecurities,
      StcColumns.securitiesQuantity,
      StcColumns.amountPaidForSecurities,
      StcColumns.totalMarketValue,
      StcColumns.typeOfShares,
      StcColumns.typeOfShares
    )
  implicit val columnIndex: ColumnIndexBuilder = new ColumnIndexBuilder(headers)


  "StcRowValidationService.validateAll" must {

    "return no errors for a valid row" in {

      val row = ParsedRow(
        rowNumber = 4,
        cells = Seq(
          ParsedCell(columnIndex.find(StcColumns.sellerName).getOrElse(-1), "Seller Ltd"),
          ParsedCell(columnIndex.find(StcColumns.sellerAddressInUK).getOrElse(-1), "yes"),
          ParsedCell(columnIndex.find(StcColumns.sellerAddressLine1).getOrElse(-1), "1 Seller Street"),
          ParsedCell(columnIndex.find(StcColumns.sellerPostcode).getOrElse(-1), "AA1 1AA"),
          ParsedCell(columnIndex.find(StcColumns.connectedPersons).getOrElse(-1), "no"),
          ParsedCell(columnIndex.find(StcColumns.applyingForRelief).getOrElse(-1), "no"),
          ParsedCell(columnIndex.find(StcColumns.whatRelief).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.securitiesTarget).getOrElse(-1), "Target Ltd"),
          ParsedCell(columnIndex.find(StcColumns.companyRegistrationNumber).getOrElse(-1), "12345678"),
          ParsedCell(columnIndex.find(StcColumns.chargingPoint).getOrElse(-1), "20/11/2025"),
          ParsedCell(columnIndex.find(StcColumns.taxRate).getOrElse(-1), "0.5%"),
          ParsedCell(columnIndex.find(StcColumns.whatTypeOfSecurities).getOrElse(-1), "shares"),
          ParsedCell(columnIndex.find(StcColumns.typeOfShares).getOrElse(-1), "ordinary"),
          ParsedCell(columnIndex.find(StcColumns.securitiesQuantity).getOrElse(-1), "100"),
          ParsedCell(columnIndex.find(StcColumns.amountPaidForSecurities).getOrElse(-1), "1000"),
          ParsedCell(columnIndex.find(StcColumns.totalMarketValue).getOrElse(-1), "1500")
        )
      )

      val result = service.validateAll(Seq(row), headers)

      result.head.validationErrors mustBe Seq.empty
    }

    "combine basic and conditional validation errors" in {

      val row = ParsedRow(
        rowNumber = 4,
        cells = Seq(
          ParsedCell(columnIndex.find(StcColumns.sellerName).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.sellerAddressInUK).getOrElse(-1), "yes"),
          ParsedCell(columnIndex.find(StcColumns.sellerAddressLine1).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.sellerPostcode).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.connectedPersons).getOrElse(-1), "yes"),
          ParsedCell(columnIndex.find(StcColumns.applyingForRelief).getOrElse(-1), "yes"),
          ParsedCell(columnIndex.find(StcColumns.whatRelief).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.securitiesTarget).getOrElse(-1), "Target Ltd"),
          ParsedCell(columnIndex.find(StcColumns.companyRegistrationNumber).getOrElse(-1), "12345678"),
          ParsedCell(columnIndex.find(StcColumns.chargingPoint).getOrElse(-1), "20/11/2025"),
          ParsedCell(columnIndex.find(StcColumns.taxRate).getOrElse(-1), "0.5%"),
          ParsedCell(columnIndex.find(StcColumns.whatTypeOfSecurities).getOrElse(-1), "shares"),
          ParsedCell(columnIndex.find(StcColumns.typeOfShares).getOrElse(-1), ""),
          ParsedCell(columnIndex.find(StcColumns.securitiesQuantity).getOrElse(-1), "100"),
          ParsedCell(columnIndex.find(StcColumns.amountPaidForSecurities).getOrElse(-1), "1000"),
          ParsedCell(columnIndex.find(StcColumns.totalMarketValue).getOrElse(-1), ""))
        )


      val result = service.validateAll(Seq(row), headers)

      val errors = result.head.validationErrors.map(_.fieldName)

      errors must contain allOf(
        "sellerName",
        "sellerAddressLine1",
        "sellerPostcode",
        "whatReliefAreYouApplyingFor",
        "totalMarketValue",
        "typeOfShares"
      )
    }
  }
}