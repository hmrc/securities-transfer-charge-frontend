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

package services.sh03.organisations.bulk

import base.FileUploadFixtures
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Relief, ReliefsDataSource}
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.bulk.TaxDueCalculationService

class TaxDueCalculationServiceSpec extends AnyWordSpec with Matchers with FileUploadFixtures {

  private val mockReliefsDataSource: ReliefsDataSource = mock[ReliefsDataSource]
  private val mockAppConfig: FrontendAppConfig = mock[FrontendAppConfig]

  when(mockAppConfig.taxRateSH03).thenReturn(BigDecimal("0.005"))

  private val service = new TaxDueCalculationService(mockReliefsDataSource, mockAppConfig)

  "TaxDueCalculationService.calculateTaxDue" should {

    "calculate tax using the higher of the amount paid and market value" in {
      val row = parsedStcRow(1).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("60.00")
    }

    "calculate tax using the amount paid when it is higher than the market value" in {
      val row = parsedStcRow(2).copy(
        amountPaidForSecurities = Some("12000"),
        totalMarketValue = Some("10000"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("60.00")
    }

    "calculate tax without relief when relief is not being applied for" in {
      val row = parsedStcRow(3).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = Some("Charity")
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("60.00")
    }

    "apply the correct relief percentage when relief is being applied for" in {
      val relief = Relief(name = "Charity", rate = 50)
      when(mockReliefsDataSource.reliefs).thenReturn(Seq(relief))

      val row = parsedStcRow(4).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = Some("Charity")
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("30.00")
    }

    "return the full tax amount when the relief name is not found" in {
      val relief = Relief(name = "test relief", rate = 50)
      when(mockReliefsDataSource.reliefs).thenReturn(Seq(relief))

      val row = parsedStcRow(5).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = Some("Unknown Relief")
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("60.00")
    }

    "return the full tax amount when not applying for a specific relief (whatReliefAreYouApplyingFor is None)" in {
      val row = parsedStcRow(6).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("60.00")
    }

    "round the calculated tax to two decimal places" in {
      val row = parsedStcRow(7).copy(
        amountPaidForSecurities = Some("10001"),
        totalMarketValue = Some("10001"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("50.01")
    }

    "round half up when the third decimal place is 5 or greater" in {
      val row = parsedStcRow(8).copy(
        amountPaidForSecurities = Some("10005"),
        totalMarketValue = Some("10005"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("50.03")
    }

    "return 0.00 when both amount paid and total market value are missing" in {
      val row = parsedStcRow(9).copy(
        amountPaidForSecurities = None,
        totalMarketValue = None,
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("0.00")
    }

    "return 0.00 when invalid alphanumeric characters are passed in the amount fields" in {
      val row = parsedStcRow(10).copy(
        amountPaidForSecurities = Some("invalid-amount"),
        totalMarketValue = Some("12000xyz"),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe BigDecimal("0.00")
    }
  }

  "TaxDueCalculationService.formatCurrency" should {

    "format a standard amount correctly to two decimal places" in {
      service.formatCurrency(BigDecimal("100.00")) shouldBe "£100.00"
    }

    "format a large amount with commas" in {
      service.formatCurrency(BigDecimal("1234567.89")) shouldBe "£1,234,567.89"
    }

    "format zero correctly" in {
      service.formatCurrency(BigDecimal("0")) shouldBe "£0.00"
    }
  }
}