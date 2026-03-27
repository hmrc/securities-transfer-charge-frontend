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

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedStcRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowValidationService

import java.time.LocalDate

class StcRowValidationServiceSpec extends AnyWordSpec with Matchers {

  private val service = new StcRowValidationService()

  private val validRow = ParsedStcRow(
    rowNumber = 3,
    addressLine1 = ParsedValue.Valid("10 Downing Street"),
    addressLine2 = ParsedValue.Missing,
    addressLine3 = ParsedValue.Missing,
    addressLine4 = ParsedValue.Missing,
    postcode = ParsedValue.Valid("SW1A 2AA"),
    country = ParsedValue.Valid("United Kingdom"),
    sellerName = ParsedValue.Valid("Bob Seller"),
    sellerAddressInUk = ParsedValue.Valid(true),
    sellerAddressLine1 = ParsedValue.Valid("1 Seller Street"),
    sellerAddressLine2 = ParsedValue.Missing,
    sellerAddressLine3 = ParsedValue.Missing,
    sellerAddressLine4 = ParsedValue.Missing,
    sellerPostcode = ParsedValue.Valid("LS1 1AA"),
    sellerCountry = ParsedValue.Valid("United Kingdom"),
    connectedPersons = ParsedValue.Valid(false),
    applyingForRelief = ParsedValue.Valid(true),
    whatReliefAreYouApplyingFor = ParsedValue.Valid("Group relief"),
    securitiesTarget = ParsedValue.Valid("Example Holdings Ltd"),
    companyRegistrationNumber = ParsedValue.Valid("12345678"),
    chargingPoint = ParsedValue.Valid(LocalDate.of(2026, 3, 23)),
    taxRate = ParsedValue.Valid(BigDecimal("0.5")),
    whatTypeOfSecurities = ParsedValue.Valid("Shares"),
    otherSecuritiesType = ParsedValue.Valid("Ordinary"),
    securitiesQuantity = ParsedValue.Valid(BigDecimal("100")),
    amountPaidForSecurities = ParsedValue.Valid(BigDecimal("500")),
    totalMarketValue = ParsedValue.Valid(BigDecimal("600"))
  )

  private def messageFor(result: uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.ValidatedStcRow, fieldName: String): Option[String] =
    result.validationErrors.find(_.fieldName == fieldName).map(_.message)

  "validate" should {

    "return no validation errors for a valid row" in {
      val result = service.validate(validRow)

      result.parsedRow shouldBe validRow
      result.validationErrors shouldBe empty
      result.hasBlockingErrors shouldBe false
      result.hasErrors shouldBe false
    }

    "return blocking errors for missing required fields" in {
      val invalidRow = validRow.copy(
        addressLine1 = ParsedValue.Missing,
        sellerName = ParsedValue.Missing,
        chargingPoint = ParsedValue.Missing
      )

      val result = service.validate(invalidRow)

      result.hasBlockingErrors shouldBe true
      result.validationErrors.map(_.fieldName) should contain allOf (
        "addressLine1",
        "sellerName",
        "chargingPoint"
      )
    }

    "return invalid numeric errors when a number field contains non-numeric data" in {
      val invalidRow = validRow.copy(
        totalMarketValue = ParsedValue.Invalid("foo", "not a number"),
        amountPaidForSecurities = ParsedValue.Invalid("bar", "not a number")
      )

      val result = service.validate(invalidRow)

      messageFor(result, "totalMarketValue") shouldBe Some("totalMarketValue must be a number")
      messageFor(result, "amountPaidForSecurities") shouldBe Some("amountPaidForSecurities must be a number")
    }

    "return invalid boolean errors when a boolean field contains an unrecognised value" in {
      val invalidRow = validRow.copy(
        sellerAddressInUk = ParsedValue.Invalid("maybe", "not a recognised boolean"),
        connectedPersons = ParsedValue.Invalid("sometimes", "not a recognised boolean")
      )

      val result = service.validate(invalidRow)

      messageFor(result, "sellerAddressInUk") shouldBe Some("sellerAddressInUk must be yes or no")
      messageFor(result, "connectedPersons") shouldBe Some("connectedPersons must be yes or no")
    }

    "return invalid date errors when a date field contains an invalid value" in {
      val invalidRow = validRow.copy(
        chargingPoint = ParsedValue.Invalid("foo", "not a valid date")
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain("chargingPoint")
      messageFor(result, "chargingPoint") shouldBe Some("chargingPoint must be a valid date")
    }

    "require reliefType when applyingForRelief is true" in {
      val invalidRow = validRow.copy(
        applyingForRelief = ParsedValue.Valid(true),
        whatReliefAreYouApplyingFor = ParsedValue.Missing
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain("whatReliefAreYouApplyingFor")
    }

    "require otherSecuritiesType when whatTypeOfSecurities is Other" in {
      val invalidRow = validRow.copy(
        whatTypeOfSecurities = ParsedValue.Valid("Other"),
        otherSecuritiesType = ParsedValue.Missing
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain("otherSecuritiesType")
    }

    "require sellerAddressLine1 and sellerPostcode when sellerAddressInUk is true" in {
      val invalidRow = validRow.copy(
        sellerAddressInUk = ParsedValue.Valid(true),
        sellerAddressLine1 = ParsedValue.Missing,
        sellerPostcode = ParsedValue.Missing
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain allOf (
        "sellerAddressLine1",
        "sellerPostcode"
      )
    }

    "require sellerCountry when sellerAddressInUk is false" in {
      val invalidRow = validRow.copy(
        sellerAddressInUk = ParsedValue.Valid(false),
        sellerCountry = ParsedValue.Missing
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain("sellerCountry")
    }
  }
}