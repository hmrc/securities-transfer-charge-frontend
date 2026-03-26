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
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.ParsedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcRowValidationService

import java.time.LocalDate

class StcRowValidationServiceSpec extends AnyWordSpec with Matchers {

  private val service = new StcRowValidationService()

  private val validRow = ParsedStcRow(
    rowNumber = 3,
    addressLine1 = Some("10 Downing Street"),
    addressLine2 = None,
    addressLine3 = None,
    addressLine4 = None,
    postcode = Some("SW1A 2AA"),
    country = Some("United Kingdom"),
    sellerName = Some("Bob Seller"),
    sellerAddressInUk = Some(true),
    sellerAddressLine1 = Some("1 Seller Street"),
    sellerAddressLine2 = None,
    sellerAddressLine3 = None,
    sellerAddressLine4 = None,
    sellerPostcode = Some("LS1 1AA"),
    sellerCountry = Some("United Kingdom"),
    connectedPersons = Some(false),
    applyingForRelief = Some(true),
    whatReliefAreYouApplyingFor = Some("Group relief"),
    securitiesTarget = Some("Example Holdings Ltd"),
    companyRegistrationNumber = Some("12345678"),
    chargingPoint = Some(LocalDate.of(2026, 3, 23)),
    taxRate = Some(BigDecimal("0.5")),
    whatTypeOfSecurities = Some("Shares"),
    otherSecuritiesType = Some("Ordinary"),
    securitiesQuantity = Some(BigDecimal("100")),
    amountPaidForSecurities = Some(BigDecimal("500")),
    totalMarketValue = Some(BigDecimal("600"))
  )

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
        addressLine1 = None,
        sellerName = None,
        chargingPoint = None
      )

      val result = service.validate(invalidRow)

      result.hasBlockingErrors shouldBe true
      result.validationErrors.map(_.fieldName) should contain allOf (
        "addressLine1",
        "sellerName",
        "chargingPoint"
      )
    }

    "require reliefType when applyingForRelief is true" in {
      val invalidRow = validRow.copy(
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = None
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain ("whatReliefAreYouApplyingFor")
    }
    
    // checking  with UCD tem as there is a discrepancy between template and stf journey in frontend

    "require otherSecuritiesType when whatTypeOfSecurities is Shares" in {
      val invalidRow = validRow.copy(
        whatTypeOfSecurities = Some("Shares"),
        otherSecuritiesType = None
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain ("otherSecuritiesType")
    }

    "require sellerAddressLine1 and sellerPostcode when sellerAddressInUk is true" in {
      val invalidRow = validRow.copy(
        sellerAddressInUk = Some(true),
        sellerAddressLine1 = None,
        sellerPostcode = None
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain allOf (
        "sellerAddressLine1",
        "sellerPostcode"
      )
    }

    "require sellerCountry when sellerAddressInUk is false" in {
      val invalidRow = validRow.copy(
        sellerAddressInUk = Some(false),
        sellerCountry = None
      )

      val result = service.validate(invalidRow)

      result.validationErrors.map(_.fieldName) should contain ("sellerCountry")
    }
  }
}