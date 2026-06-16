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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcConditionalRowValidatorSpec extends SpecBase {

  private implicit val cols: ColumnIndexBuilder = new ColumnIndexBuilder(Seq.empty)

  private val validParsedRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 3,
      sellerName = Some("Seller Ltd"),
      sellerAddressInUK = Some(true),
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
      chargingPoint = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
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

  private val app = applicationBuilder().build()

  private val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]

  private val validator =
    new StcConditionalRowValidator(
      support = new StcValidationSupport,
      messagesApi = messagesApi
    )

  "StcConditionalRowValidator.validate STF" - {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.STF,affinityGroupKeyInd)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "reject invalid relief name when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = Some("Made Up Relief")
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "require type of shares when what type of securities is shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("shares"),
          typeOfShares = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "typeOfShares") mustBe true
    }

    "not require type of shares when security type is not shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("Loan notes"),
          typeOfShares = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.map(_.fieldName) must not contain "typeOfShares"
    }

    "validate UK seller address line 1 required" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerAddressLine1 = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerAddressLine1") mustBe true
    }

    "validate UK seller address lines invalid characters" in {

      val testCases = Seq(
        "sellerAddressLine1" -> ((row: ParsedStcRow) =>
          row.copy(sellerAddressInUK = Some(true), sellerAddressLine1 = Some("Address @@@"))),

        "sellerAddressLine2" -> ((row: ParsedStcRow) =>
          row.copy(sellerAddressInUK = Some(true), sellerAddressLine2 = Some("Address @@@"))),

        "sellerAddressLine3" -> ((row: ParsedStcRow) =>
          row.copy(sellerAddressInUK = Some(true), sellerAddressLine3 = Some("Address @@@"))),

        "sellerAddressLine4" -> ((row: ParsedStcRow) =>
          row.copy(sellerAddressInUK = Some(true), sellerAddressLine4 = Some("Address @@@")))
      )

      testCases.foreach { case (fieldName, modifyRow) =>
        val result = validator.validate(
          modifyRow(validParsedRow),
          StcTemplate.STF,
          affinityGroupKeyInd
        )

        result.exists(_.fieldName == fieldName) mustBe true
      }
    }

    "validate UK seller address lines max length" in {

      val over100Chars =
        "Flat 12B, Riverside Court, 45 High Street, London, Flat 12B, Riverside Court, 45 High Street, London, UK"

      val over40Chars =
        "Flat 12B, Riverside Court, 45 High Street, London"

      val testCases = Seq(
        (
          "sellerAddressLine1",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine1 = Some(over100Chars)
            ),
          "must be 100 characters or fewer"
        ),
        (
          "sellerAddressLine2",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine2 = Some(over40Chars)
            ),
          "must be fewer than 40 characters long"
        ),
        (
          "sellerAddressLine3",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine3 = Some(over40Chars)
            ),
          "must be fewer than 40 characters long"
        ),
        (
          "sellerAddressLine4",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine4 = Some(over40Chars)
            ),
          "must be fewer than 40 characters long"
        )
      )

      testCases.foreach { case (fieldName, modifyRow, expectedMessage) =>
        val result = validator.validate(
          modifyRow(validParsedRow),
          StcTemplate.STF,
          affinityGroupKeyInd
        )

        val error = result.find(_.fieldName == fieldName)

        error mustBe defined
        error.get.message must include(expectedMessage)
      }
    }

    "validate UK seller postcode required" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerPostcode = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate UK seller postcode invalid" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerPostcode = Some("not a postcode")
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate seller country when seller address is not in the UK" in {
      val longCountry = "a" * 51

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerCountry = Some(longCountry)
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "require total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = None
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject too large total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = Some(BigDecimal(1000000000))
        ), StcTemplate.STF,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }
  }

  "StcConditionalRowValidator.validate SH03" - {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.SH03,affinityGroupKeyInd)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.SH03,affinityGroupKeyInd
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }
  }
}