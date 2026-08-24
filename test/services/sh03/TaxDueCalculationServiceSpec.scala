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

package services.sh03

import base.SpecBase
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Relief, ReliefsDataSource}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.DetailsOfThisSharePurchase
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03._
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.TaxDueCalculationService

import java.time.LocalDate

class TaxDueCalculationServiceSpec extends SpecBase with MockitoSugar {

  val mockReliefsDataSource: ReliefsDataSource = mock[ReliefsDataSource]
  val service = new TaxDueCalculationService(mockReliefsDataSource)

  "TaxDueCalculationService" - {

    "calculateTaxDue" - {
      "must calculate tax at fixed 0.5% when amount paid is higher" in {
        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("8000")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, false).success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("50.00"))
      }

      "must calculate tax at fixed 0.5% when market value is higher" in {
        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("8000"), Some(BigDecimal("10000")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, false).success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("50.00"))
      }

      "must apply relief when relief is selected" in {
        val testRelief = Relief("Test Relief", 50)
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))

        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("10000")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, true).success.value
          .set(WhatReliefAreYouApplyingForPage, "Test Relief").success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("25.00"))
      }

      "must apply 100% relief correctly" in {
        val testRelief = Relief("Full Relief", 100)
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))

        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("10000")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, true).success.value
          .set(WhatReliefAreYouApplyingForPage, "Full Relief").success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("0.00"))
      }

      "must not apply negative tax" in {
        val testRelief = Relief("Over Relief", 150)
        when(mockReliefsDataSource.reliefs).thenReturn(Seq(testRelief))

        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("10000"), Some(BigDecimal("10000")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, true).success.value
          .set(WhatReliefAreYouApplyingForPage, "Over Relief").success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("0.00"))
      }

      "must return Some(0.00) when required data is missing (defaults to 0)" in {
        val userAnswers = emptyUserAnswers

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("0.00"))
      }

      "must round to 2 decimal places correctly" in {
        val detailsOfPurchase = DetailsOfThisSharePurchase(100, "Ordinary", BigDecimal("10001"), Some(BigDecimal("10001")))
        val userAnswers = emptyUserAnswers
          .set(DetailsOfThisSharePurchasePage, detailsOfPurchase).success.value
          .set(ApplyingForReliefPage, false).success.value

        val result = service.calculateTaxDue(userAnswers)

        result mustBe Some(BigDecimal("50.01"))
      }
    }

    "calculatePaymentDueDate" - {
      "must calculate payment due date as 30 days after charging point" in {
        val chargingPoint = LocalDate.of(2024, 1, 15)
        val userAnswers = emptyUserAnswers
          .set(ChargingPointPage, chargingPoint).success.value

        val result = service.calculatePaymentDueDate(userAnswers)

        result mustBe Some(LocalDate.of(2024, 2, 14))
      }

      "must handle month end correctly" in {
        val chargingPoint = LocalDate.of(2024, 1, 31)
        val userAnswers = emptyUserAnswers
          .set(ChargingPointPage, chargingPoint).success.value

        val result = service.calculatePaymentDueDate(userAnswers)

        result mustBe Some(LocalDate.of(2024, 3, 1))
      }

      "must handle leap year correctly" in {
        val chargingPoint = LocalDate.of(2024, 1, 30)
        val userAnswers = emptyUserAnswers
          .set(ChargingPointPage, chargingPoint).success.value

        val result = service.calculatePaymentDueDate(userAnswers)

        result mustBe Some(LocalDate.of(2024, 2, 29))
      }

      "must handle year end correctly" in {
        val chargingPoint = LocalDate.of(2024, 12, 15)
        val userAnswers = emptyUserAnswers
          .set(ChargingPointPage, chargingPoint).success.value

        val result = service.calculatePaymentDueDate(userAnswers)

        result mustBe Some(LocalDate.of(2025, 1, 14))
      }

      "must return None when charging point is missing" in {
        val userAnswers = emptyUserAnswers

        val result = service.calculatePaymentDueDate(userAnswers)

        result mustBe None
      }
    }
  }
}