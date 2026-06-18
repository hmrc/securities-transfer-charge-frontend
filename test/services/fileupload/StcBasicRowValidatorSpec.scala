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

import base.SpecBase
import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.{AmountPaidForSecuritiesFormProvider, NameOfBuyerFormProvider, NameOfSellerFormProvider, SecuritiesTargetFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedValue.{Invalid, Missing, Valid}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcBasicRowValidatorSpec extends SpecBase {

  private val app = applicationBuilder().build()

  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))


  private val validRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      buyerName = Some("Bob buyer"),
      buyerAddressInUK= Some(true),
      buyerAddressLine1= Some("1 Seller Street"),
      buyerAddressLine2= Some("Seller District"),
      buyerAddressLine3= Some("Seller City"),
      buyerAddressLine4= None,
      buyerPostcode= Some("AA1 1AA"),
      buyerCountry= Some("United Kingdom"),
      sellerName = Some("Seller Ltd"),
      sellerAddressInUK = Some(true),
      sellerAddressLine1 = Some("1 Test"),
      sellerAddressLine2 = Some("Test Region"),
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = Some("AA1 1AA"),
      sellerCountry = None,
      connectedPersons = Some(true),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = None,
      securitiesTarget = Some("Target Ltd"),
      companyRegistrationNumber = Some("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
      taxRate = Some(BigDecimal("0.5")),
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = Some("Ordinary"),
      securitiesQuantity = Some("10000"),
      amountPaidForSecurities = Some("15000"),
      totalMarketValue = Some("20000"),
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = None,
      purchaseForCancellation = None
    )

  private val validator =
    new StcBasicRowValidator(
      support = new StcValidationSupport,
      messagesApi = messagesApi,
      nameOfSellerFormProvider = new NameOfSellerFormProvider,
      securitiesTargetFormProvider = new SecuritiesTargetFormProvider,
      nameOfBuyerFormProvider = new NameOfBuyerFormProvider,
      amountPaidForSecuritiesFormProvider = new AmountPaidForSecuritiesFormProvider
    )

  private implicit val columnIndex: ColumnIndexBuilder =
    new ColumnIndexBuilder(Seq(
      "sellerName",
      "sellerAddressInUK",
      "connectedPersons",
      "applyingForRelief",
      "chargingPoint",
      "taxRate",
      "whatTypeOfSecurities",
      "securitiesQuantity",
      "amountPaidForSecurities"
    ))

  "StcBasicRowValidator.validate" - {

    "return no errors for a valid row" in {
      val result = validator.validate(validRow, StcTemplate.STF, affinityGroupKeyInd)

      result mustBe Seq.empty
    }

    "return charging point required error when missing" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = Missing),
        StcTemplate.STF, "org"
      )

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("org.chargingPoint.error.required.all")) mustBe true
    }

    "return charging point invalid error when date is unparsable" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = Invalid("32/13/2026", "not a valid date")),
        StcTemplate.STF, "org"
      )

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("org.chargingPoint.error.invalid")) mustBe true
    }

    "return charging point future date error when date is in the future" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = Valid(LocalDate.now().plusDays(5))),
        StcTemplate.STF, "org"
      )

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("org.chargingPoint.error.futureDate")) mustBe true
    }

    "drop the prefix for charging point invalid error when affinityKey is 'individual'" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = Invalid("32/13/2026", "not a valid date")),
        StcTemplate.STF, "individual"
      )

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("chargingPoint.error.invalid")) mustBe true
    }

    "return securities quantity whole number error when a decimal is provided" in {
      val result = validator.validate(
        validRow.copy(securitiesQuantity = Some("10.5")),
        StcTemplate.STF, "org"
      )

      result.exists(e =>
        e.fieldName == "securitiesQuantity" &&
          e.message == "The number of shares must be a whole number between 1 and 999,999,999"
      ) mustBe true
    }

    "return seller name required error" in {
      val result = validator.validate(
        validRow.copy(sellerName = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerName") mustBe true
    }

    "return seller address in uk missing error" in {
      val result = validator.validate(
        validRow.copy(sellerAddressInUK = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerAddressInUK") mustBe true
    }

    "return connected persons missing error" in {
      val result = validator.validate(
        validRow.copy(connectedPersons = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "connectedPersons") mustBe true
    }

    "return applying for relief missing error" in {
      val result = validator.validate(
        validRow.copy(applyingForRelief = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "applyingForRelief") mustBe true
    }

    "return tax rate invalid when missing" in {
      val result = validator.validate(
        validRow.copy(taxRate = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "taxRate") mustBe true
    }

    "return what type of securities required error" in {
      val result = validator.validate(
        validRow.copy(whatTypeOfSecurities = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "whatTypeOfSecurities") mustBe true
    }

    "return securities quantity required error" in {
      val result = validator.validate(
        validRow.copy(securitiesQuantity = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity minimum error" in {
      val result = validator.validate(
        validRow.copy(securitiesQuantity = Some("0")),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity maximum error" in {
      val result = validator.validate(
        validRow.copy(securitiesQuantity = Some("999999999")),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return amount paid required error" in {
      val result = validator.validate(
        validRow.copy(amountPaidForSecurities = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }

    "return amount paid maximum error" in {
      val result = validator.validate(
        validRow.copy(amountPaidForSecurities = Some("1000000000")),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }
  }

}