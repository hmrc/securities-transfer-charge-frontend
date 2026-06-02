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
import play.api.i18n.MessagesApi
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.{NameOfSellerFormProvider,SecuritiesTargetFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcBasicRowValidatorSpec extends SpecBase {

  private val app = applicationBuilder().build()

  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val validRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
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
      chargingPoint = Some(LocalDate.of(2025, 11, 20)),
      taxRate = Some(BigDecimal("0.5")),
      whatTypeOfSecurities = Some("Shares"),
      typeOfShares = Some("Ordinary"),
      securitiesQuantity = Some(BigDecimal(10000)),
      amountPaidForSecurities = Some(BigDecimal(15000)),
      totalMarketValue = Some(BigDecimal(20000)),
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
      securitiesTargetFormProvider = new SecuritiesTargetFormProvider
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

    "return charging point required error" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = None),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "chargingPoint") mustBe true
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
        validRow.copy(securitiesQuantity = Some(BigDecimal(0))),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "securitiesQuantity") mustBe true
    }

    "return securities quantity maximum error" in {
      val result = validator.validate(
        validRow.copy(securitiesQuantity = Some(BigDecimal(999999999))),
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
        validRow.copy(amountPaidForSecurities = Some(BigDecimal(1000000000))),
        StcTemplate.STF, affinityGroupKeyInd
      )

      result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
    }
  }

}