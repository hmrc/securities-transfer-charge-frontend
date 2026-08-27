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

package services.stf.bulk

import base.FileUploadFixtures
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Relief, ReliefsDataSource}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.TaxDueCalculationService

class TaxDueCalculationServiceSpec extends AnyWordSpec with Matchers with FileUploadFixtures {

  val mockReliefsDataSource: ReliefsDataSource = mock[ReliefsDataSource]
  
  private val service = new TaxDueCalculationService(mockReliefsDataSource)

  "TaxDueCalculationService.calculateTaxDue" should {

    "calculate tax using the higher of the amount paid and market value" in {
      val row = parsedStcRow(1).copy(
        amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None)

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("60.00"))
    }

    "calculate tax using the amount paid when it is higher than the market value" in {
      val row = parsedStcRow(2).copy(
        amountPaidForSecurities = Some("12000"),
        totalMarketValue = Some("10000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None
      )

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("60.00"))
    }

    "calculate tax without relief when relief is not being applied for" in {
      val row = parsedStcRow(3).copy(amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = Some("Charity"))

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("60.00"))
    }

    "apply the correct relief percentage when relief is being applied for" in {

      val relief = Relief(name = "Charity", rate = 50)
      when(mockReliefsDataSource.reliefs).thenReturn(Seq(relief))
      val row = parsedStcRow(4).copy(amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = Some("Charity"))
      
      service.calculateTaxDue(row) shouldBe Some(BigDecimal("30.00"))
    }

    "return the full tax amount when the relief name is not found" in {
      val relief = Relief(name = "test relief", rate = 50)
      when(mockReliefsDataSource.reliefs).thenReturn(Seq(relief))

      val row = parsedStcRow(5).copy(amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = Some("Unknown Relief"))
      service.calculateTaxDue(row) shouldBe Some(BigDecimal("60.00"))
    }

    "return the full tax amount when not applying for a relief" in {
      val row = parsedStcRow(6).copy(amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(true),
        whatReliefAreYouApplyingFor = None)

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("60.00"))
    }

    "round the calculated tax to two decimal places" in {
      val row = parsedStcRow(7).copy(amountPaidForSecurities = Some("10001"),
        totalMarketValue = Some("10001"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None)

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("50.01"))
    }

    "round half up when the third decimal place is 5 or greater" in {
      val row = parsedStcRow(8).copy(amountPaidForSecurities = Some("10005"),
        totalMarketValue = Some("10005"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None)

      service.calculateTaxDue(row) shouldBe Some(BigDecimal("50.03"))
    }

    "return None when the amount paid is missing" in {
      val row = parsedStcRow(9).copy(amountPaidForSecurities = None,
        totalMarketValue = Some("12000"),
        taxRate = Some(BigDecimal("0.5")),
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None)

      service.calculateTaxDue(row) shouldBe None
    }
    
    "return None when the tax rate is missing" in {
      val row = parsedStcRow(11).copy(amountPaidForSecurities = Some("10000"),
        totalMarketValue = Some("12000"),
        taxRate = None,
        applyingForRelief = Some(false),
        whatReliefAreYouApplyingFor = None)
      service.calculateTaxDue(row) shouldBe None
    }

  }
}

