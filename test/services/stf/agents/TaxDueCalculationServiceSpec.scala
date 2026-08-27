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

package services.stf.agents

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito.when
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedValue}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Relief, ReliefsDataSource}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents.TaxDueCalculationService

import java.time.LocalDate

class TaxDueCalculationServiceSpec extends SpecBase with MockitoSugar {

  val mockReliefsDataSource: ReliefsDataSource = mock[ReliefsDataSource]
  val service = new TaxDueCalculationService(mockReliefsDataSource)
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
      securitiesQuantity = Some("100"),
      amountPaidForSecurities = Some("10000"),
      totalMarketValue = Some("8000"),
      minSharePrice = None,
      maxSharePrice = None,
      sharePurchaseReason = None,
      purchaseForCancellation = None
    )

  "TaxDueCalculationService" - {

    "calculateTaxDueForRow" - {
      "must calculate tax at 0.05% for shares when amount paid is higher" in {

        val result = service.calculateTaxDueForRow(validRow)
        result mustBe Some(BigDecimal("50.00"))
      }

      "must calculate tax at 0.5% for shares when market value is higher" in {
        val anotherValidRow = validRow.copy(amountPaidForSecurities = Some("8000"), totalMarketValue = Some("10000"))
        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("50.00"))
      }

      "must calculate tax at 1.5% for shares" in {
        val anotherValidRow = validRow.copy(taxRate = Some(BigDecimal("1.5")), amountPaidForSecurities = Some("10000"), totalMarketValue = Some("10000"))
        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("150.00"))
      }

      "must calculate tax for other securities when amount paid is higher" in {
        val anotherValidRow = validRow.copy(amountPaidForSecurities = Some("5000"), totalMarketValue = Some("4000"))

        val result = service.calculateTaxDueForRow(anotherValidRow)
        result mustBe Some(BigDecimal("25.00"))
      }


      "must apply relief when relief is selected" in {
        val anotherValidRow = validRow.copy(applyingForRelief = Some(true), whatReliefAreYouApplyingFor = Some("Test Relief"))
        val testRelief = Relief("Test Relief", 50)
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))
        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("25.00"))
      }

      "must apply 100% relief correctly" in {
        val anotherValidRow = validRow.copy(applyingForRelief = Some(true), whatReliefAreYouApplyingFor = Some("Full Relief"))
        val testRelief = Relief("Full Relief", 100)
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))
        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("0.00"))
      }

      "must not apply negative tax" in {
        val testRelief = Relief("Over Relief", 150)
        val anotherValidRow = validRow.copy(applyingForRelief = Some(true), whatReliefAreYouApplyingFor = Some("Over Relief"))
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))

        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("0.00"))
      }

      "must return None when required data is missing" in {
        val invalidRow = validRow.copy(amountPaidForSecurities = None, totalMarketValue = None, taxRate = None)
        val result = service.calculateTaxDueForRow(invalidRow)

        result mustBe None
      }

      "must round to 2 decimal places correctly" in {
        val anotherValidRow = validRow.copy(amountPaidForSecurities = Some("10001"), totalMarketValue = Some("10001"))
        val result = service.calculateTaxDueForRow(anotherValidRow)

        result mustBe Some(BigDecimal("50.01"))
      }
    }

    "calculatePaymentDueDate" - {
      "must calculate payment due date as 30 days after charging point" in {
        val chargingPoint1 = ParsedValue.Valid(LocalDate.of(2024, 1, 15))
        val chargingPoint2 = ParsedValue.Valid(LocalDate.of(2025, 1, 30))
        val row1 = validRow.copy(chargingPoint = chargingPoint1)
        val row2 = validRow.copy(chargingPoint = chargingPoint2)
        val rows = Seq(validRow, row1, row2)

        val result = service.calculatePaymentDueDate(rows)

        result mustBe LocalDate.of(2024, 2, 14)
      }

      "must handle month end correctly" in {
        val chargingPoint1 = ParsedValue.Valid(LocalDate.of(2024, 1, 31))
        val chargingPoint2 = ParsedValue.Valid(LocalDate.of(2025, 1, 30))
        val row1 = validRow.copy(chargingPoint = chargingPoint1)
        val row2 = validRow.copy(chargingPoint = chargingPoint2)
        val rows = Seq(validRow, row1, row2)

        val result = service.calculatePaymentDueDate(rows)

        result mustBe LocalDate.of(2024, 3, 1)
      }

      "must handle leap year correctly" in {
        val chargingPoint1 = ParsedValue.Valid(LocalDate.of(2024, 1, 30))
        val chargingPoint2 = ParsedValue.Valid(LocalDate.of(2025, 1, 30))
        val row1 = validRow.copy(chargingPoint = chargingPoint1)
        val row2 = validRow.copy(chargingPoint = chargingPoint2)
        val rows = Seq(validRow, row1, row2)
        val result = service.calculatePaymentDueDate(rows)

        result mustBe LocalDate.of(2024, 2, 29)
      }

      "must handle year end correctly" in {
        val chargingPoint1 = ParsedValue.Valid(LocalDate.of(2024, 12, 15))
        val chargingPoint2 = ParsedValue.Valid(LocalDate.of(2025, 1, 30))
        val row1 = validRow.copy(chargingPoint = chargingPoint1)
        val row2 = validRow.copy(chargingPoint = chargingPoint2)
        val rows = Seq(validRow, row1, row2)

        val result = service.calculatePaymentDueDate(rows)

        result mustBe LocalDate.of(2025, 1, 14)
      }
    }
  }
}
