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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.shared.BulkTotalMarketValueFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.*
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.*

import java.time.LocalDate

class StcConditionalRowValidatorSpec extends SpecBase {

  private implicit val cols: ColumnIndexBuilder = new ColumnIndexBuilder(Seq.empty)

  private val validParsedRow: ParsedStcRow =
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
      securitiesQuantity = Some("100"),
      amountPaidForSecurities = Some("1000"),
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
      messagesApi = messagesApi,
      totalMarketValueFormProvider = new BulkTotalMarketValueFormProvider
    )

  "StcConditionalRowValidator.validate STF" - {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.STF,affinityGroupKeyInd, STF)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "reject invalid relief name when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = Some("Made Up Relief")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "ignore relief type errors when applying for relief is no" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(false),
          whatReliefAreYouApplyingFor = Some("Made Up Relief")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe false
    }

    "require type of shares when what type of securities is shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("shares"),
          typeOfShares = None
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "typeOfShares") mustBe true
    }

    "not require type of shares when security type is not shares" in {
      val result = validator.validate(
        validParsedRow.copy(
          whatTypeOfSecurities = Some("Loan notes"),
          typeOfShares = None
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.map(_.fieldName) must not contain "typeOfShares"
    }

    "validate UK seller address line 1 required" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerAddressLine1 = None
        ), StcTemplate.STF,affinityGroupKeyInd, STF
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
          affinityGroupKeyInd, STF
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
          "must be 40 characters or fewer"
        ),
        (
          "sellerAddressLine3",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine3 = Some(over40Chars)
            ),
          "must be 40 characters or fewer"
        ),
        (
          "sellerAddressLine4",
          (row: ParsedStcRow) =>
            row.copy(
              sellerAddressInUK = Some(true),
              sellerAddressLine4 = Some(over40Chars)
            ),
          "must be 40 characters or fewer"
        )
      )

      testCases.foreach { case (fieldName, modifyRow, expectedMessage) =>
        val result = validator.validate(
          modifyRow(validParsedRow),
          StcTemplate.STF,
          affinityGroupKeyInd, STF
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
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate UK seller postcode invalid" in {
      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerPostcode = Some("not a postcode")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "validate seller country when seller address is not in the UK" in {
      val longCountry = "a" * 101

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerCountry = Some(longCountry)
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "require total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = None
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject too large total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = Some("1000000000")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject total market value below minimum when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = Some("0")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "reject invalid numeric total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = Some("100.123")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "ignore total market value errors when connected persons is no" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(false),
          totalMarketValue = Some("NOT A NUMBER")
        ), StcTemplate.STF,affinityGroupKeyInd, STF
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe false
    }
  }

  "Agent STF"- {
    "reject country max length for seller address" in {
      val longCountry = "a" * 101

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerCountry = Some(longCountry)
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "reject an invalid country for seller address" in {
      val invalidCountry = "%%%%%%"

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerCountry = Some(invalidCountry)
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "reject missing country for seller address" in {

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerCountry = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerCountry") mustBe true
    }

    "reject country max length for buyer address" in {
      val longCountry = "a" * 101

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(false),
          buyerCountry = Some(longCountry)
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerCountry") mustBe true
    }

    "reject an invalid country for buyer address" in {
      val invalidCountry = "%%%%%%"

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(false),
          buyerCountry = Some(invalidCountry)
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerCountry") mustBe true
    }

    "reject missing country for buyer address" in {

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(false),
          buyerCountry = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerCountry") mustBe true
    }

    "reject missing postcode for buyer address when address in the UK is false" in {

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(false),
          buyerPostcode = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerPostcode") mustBe true
    }

    "reject missing postcode for buyer address when address in the UK is true" in {

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(true),
          buyerPostcode = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerPostcode") mustBe true
    }

    "reject missing postcode for seller address when address in the UK is false" in {

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerPostcode = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "reject missing postcode for seller address when address in the UK is true" in {

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerPostcode = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "reject invalid postcode when seller address in the UK is true" in {

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(true),
          sellerPostcode = Some("%%%%")
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "reject invalid postcode when seller address in the UK is false" in {

      val result = validator.validate(
        validParsedRow.copy(
          sellerAddressInUK = Some(false),
          sellerPostcode = Some("%%%%")
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "sellerPostcode") mustBe true
    }

    "reject invalid postcode when buyer address in the UK is true" in {

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(true),
          buyerPostcode = Some("%%%%")
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerPostcode") mustBe true
    }

    "reject invalid postcode when buyer address in the UK is false" in {

      val result = validator.validate(
        validParsedRow.copy(
          buyerAddressInUK = Some(false),
          buyerPostcode = Some("%%%%")
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "buyerPostcode") mustBe true
    }

    "reject invalid relief type" in {

      val result = validator.validate(
        validParsedRow.copy(applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = Some("invalid relief")
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "reject type of securities for invalid length" in {

      val invalidSecurityTypeLength = "a" * 270

      val result = validator.validate(
        validParsedRow.copy(typeOfShares = Some("shares"),
          whatTypeOfSecurities = Some(invalidSecurityTypeLength)

        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "whatTypeOfSecurities") mustBe true
    }

    "require type of securities when share type is shares" in {

      val result = validator.validate(
        validParsedRow.copy(
          typeOfShares = Some("shares"),
          whatTypeOfSecurities = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )
      result.exists(_.fieldName == "whatTypeOfSecurities") mustBe true
    }

    "require total market value when connected person is true" in {

      val result = validator.validate(
        validParsedRow.copy( connectedPersons = Some(true),
          totalMarketValue = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )
      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.STFAgent, affinityGroupKeyAgent, STF
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }
  }

  "StcConditionalRowValidator.validate SH03" - {

    "return no errors for a valid conditional row" in {
      val result = validator.validate(validParsedRow, StcTemplate.SH03,affinityGroupKeyInd, SH03)
      result mustBe Seq.empty
    }

    "require relief type when applying for relief is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          applyingForRelief = Some(true),
          whatReliefAreYouApplyingFor = None
        ), StcTemplate.SH03,affinityGroupKeyInd, SH03
      )

      result.exists(_.fieldName == "whatReliefAreYouApplyingFor") mustBe true
    }

    "require total market value when connected persons is yes" in {
      val result = validator.validate(
        validParsedRow.copy(
          connectedPersons = Some(true),
          totalMarketValue = None
        ), StcTemplate.SH03, affinityGroupKeyInd, SH03
      )

      result.exists(_.fieldName == "totalMarketValue") mustBe true
    }
  }
}