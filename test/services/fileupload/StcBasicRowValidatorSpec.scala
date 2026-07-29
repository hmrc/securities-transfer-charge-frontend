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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.fileUpload.{SecuritiesTargetFormProvider,AmountPaidForSecuritiesFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.{NameOfBuyerFormProvider, NameOfSellerFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedValue.{Invalid, Missing, Valid}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig

import java.time.LocalDate

class StcBasicRowValidatorSpec extends SpecBase {

  private val app = applicationBuilder().build()

  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  private implicit val messages: Messages = messagesApi.preferred(Seq(Lang("en")))

  private val appConfig: FrontendAppConfig = app.injector.instanceOf[FrontendAppConfig]

  private val validRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      buyerName = Some("Bob buyer"),
      buyerAddressInUK = Some(true),
      buyerAddressLine1 = Some("1 Seller Street"),
      buyerAddressLine2 = Some("Seller District"),
      buyerAddressLine3 = Some("Seller City"),
      buyerAddressLine4 = None,
      buyerPostcode = Some("AA1 1AA"),
      buyerCountry = Some("United Kingdom"),
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
      chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 2, 20)),
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
      appConfig = appConfig,
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
      "amountPaidForSecurities",
      "sharePurchaseReason",
      "purchasedForCancellation",
      "minSharePrice",
      "maxSharePrice"
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

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("fileUpload.org.chargingPoint.error.futureDate")) mustBe true
    }

    "return charging point too early error when date is before the earliest allowed date" in {
      val result = validator.validate(
        validRow.copy(chargingPoint = Valid(LocalDate.of(2025, 12, 31))),
        StcTemplate.STF, "org"
      )

      result.exists(e => e.fieldName == "chargingPoint" && e.message == messages("fileUpload.org.chargingPoint.error.beforeFirstDate")) mustBe true
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
        validRow.copy(securitiesQuantity = Some("1000000000")),
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

    "Agent STF" - {

      "return type of shares required error" in {
        val result = validator.validate(
          validRow.copy(typeOfShares = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "typeOfShares") mustBe true
      }

      "return seller name required error" in {
        val result = validator.validate(
          validRow.copy(sellerName = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "sellerName") mustBe true
      }

      "return buyer name required error" in {
        val result = validator.validate(
          validRow.copy(buyerName = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "buyerName") mustBe true
      }

      "return seller address in uk missing error" in {
        val result = validator.validate(
          validRow.copy(sellerAddressInUK = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "sellerAddressInUK") mustBe true
      }

      "return buyer address in uk missing error" in {
        val result = validator.validate(
          validRow.copy(buyerAddressInUK = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "buyerAddressInUK") mustBe true
      }

      "return connected persons missing error" in {
        val result = validator.validate(
          validRow.copy(connectedPersons = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "connectedPersons") mustBe true
      }

      "return applying for relief missing error" in {
        val result = validator.validate(
          validRow.copy(applyingForRelief = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "applyingForRelief") mustBe true
      }

      "return tax rate required error" in {
        val result = validator.validate(
          validRow.copy(taxRate = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "taxRate") mustBe true
      }

      "return securities quantity required error" in {
        val result = validator.validate(
          validRow.copy(securitiesQuantity = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "securitiesQuantity") mustBe true
      }

      "return charging point required error" in {
        val result = validator.validate(
          validRow.copy(chargingPoint = ParsedValue.Missing),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "chargingPoint") mustBe true
      }

      "return amount paid for securities required error" in {
        val result = validator.validate(
          validRow.copy(amountPaidForSecurities = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "amountPaidForSecurities") mustBe true
      }

      "return securities target required error" in {
        val result = validator.validate(
          validRow.copy(securitiesTarget = None),
          StcTemplate.STFAgent, affinityGroupKeyAgent
        )

        result.exists(_.fieldName == "securitiesTarget") mustBe true
      }
    }

    "SH03 Validation" - {

      "return no errors for a valid SH03 row" in {
        val validSh03Row = validRow.copy(
          whatTypeOfSecurities = Some("shares"),
          securitiesQuantity = Some("100"),
          amountPaidForSecurities = Some("1000"),
          chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 2, 20)),
          sharePurchaseReason = Some("cancellation"),
          purchaseForCancellation = Some(true),
          connectedPersons = Some(true),
          applyingForRelief = Some(false)
        )
        val result = validator.validate(validSh03Row, StcTemplate.SH03, "agent")
        result mustBe Seq.empty
      }

      "return share purchase reason missing error" in {
        val result = validator.validate(
          validRow.copy(sharePurchaseReason = None),
          StcTemplate.SH03, "agent"
        )
        result.exists(e => e.fieldName == "sharePurchaseReason" && e.message == messages("sharePurchaseReason.required")) mustBe true
      }

      "return share purchase reason invalid error" in {
        val result = validator.validate(
          validRow.copy(sharePurchaseReason = Some("invalid reason")),
          StcTemplate.SH03, "agent"
        )
        result.exists(e => e.fieldName == "sharePurchaseReason" && e.message == messages("sharePurchaseReason.invalid")) mustBe true
      }

      "return purchase for cancellation missing error" in {
        val result = validator.validate(
          validRow.copy(purchaseForCancellation = None),
          StcTemplate.SH03, "agent"
        )
        result.exists(e => e.fieldName == "purchasedForCancellation" && e.message == messages("purchasedForCancellation.invalid")) mustBe true
      }

      "Min Share Price" - {
        "allow empty minSharePrice (optional for non-PLCs)" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = None),
            StcTemplate.SH03, "agent"
          )
          result.exists(_.fieldName == "minSharePrice") mustBe false
        }

        "allow valid minSharePrice with commas" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = Some("1,000.50")),
            StcTemplate.SH03, "agent"
          )
          result.exists(_.fieldName == "minSharePrice") mustBe false
        }

        "reject non-numeric minSharePrice" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = Some("abc")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "minSharePrice" && e.message == "The minimum amount paid for the shares must be a number and can include up to two decimal places, like £30 or £28.60") mustBe true
        }

        "reject minSharePrice with more than two decimal places" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = Some("10.123")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "minSharePrice" && e.message == "The minimum amount paid for the shares must be a number and can include up to two decimal places, like £30 or £28.60") mustBe true
        }

        "reject minSharePrice that is too high" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = Some("1000000000")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "minSharePrice" && e.message == "The minimum amount paid for the shares must be £999,999,999 or less") mustBe true
        }

        "reject minSharePrice that is too low" in {
          val result = validator.validate(
            validRow.copy(minSharePrice = Some("0")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "minSharePrice" && e.message == "The minimum amount paid for the shares must be £0.01 or more") mustBe true
        }
      }

      "Max Share Price" - {
        "allow empty maxSharePrice (optional for non-PLCs)" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = None),
            StcTemplate.SH03, "agent"
          )
          result.exists(_.fieldName == "maxSharePrice") mustBe false
        }

        "allow valid maxSharePrice with commas" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = Some("2,500,000")),
            StcTemplate.SH03, "agent"
          )
          result.exists(_.fieldName == "maxSharePrice") mustBe false
        }

        "reject non-numeric maxSharePrice" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = Some("abc")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "maxSharePrice" && e.message == "The maximum amount paid for the shares must be a number and can include up to two decimal places, like £30 or £28.60") mustBe true
        }

        "reject maxSharePrice with more than two decimal places" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = Some("10.123")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "maxSharePrice" && e.message == "The maximum amount paid for the shares must be a number and can include up to two decimal places, like £30 or £28.60") mustBe true
        }

        "reject maxSharePrice that is too high" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = Some("1000000000")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "maxSharePrice" && e.message == "The maximum amount paid for the shares must be £999,999,999 or less") mustBe true
        }

        "reject maxSharePrice that is too low" in {
          val result = validator.validate(
            validRow.copy(maxSharePrice = Some("0")),
            StcTemplate.SH03, "agent"
          )
          result.exists(e => e.fieldName == "maxSharePrice" && e.message == "The maximum amount paid for the shares must be £0.01 or more") mustBe true
        }

        "allow securitiesQuantity with commas" in {
          val result = validator.validate(
            validRow.copy(securitiesQuantity = Some("1,000,000")),
            StcTemplate.STF, "agent"
          )
          result.exists(_.fieldName == "securitiesQuantity") mustBe false
        }
      }
    }
  }
}