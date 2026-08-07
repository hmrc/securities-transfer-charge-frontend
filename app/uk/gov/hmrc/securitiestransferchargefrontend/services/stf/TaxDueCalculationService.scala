/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf

import uk.gov.hmrc.securitiestransferchargefrontend.models.{ReliefsDataSource, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.TaxRate
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single._

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.math.BigDecimal.RoundingMode

@Singleton
class TaxDueCalculationService @Inject()(reliefsDataSource: ReliefsDataSource) {

  private def getTaxRateValue(taxRate: TaxRate): BigDecimal = taxRate match {
    case TaxRate.HalfPercent => BigDecimal("0.005")
    case TaxRate.OneAndHalfPercent => BigDecimal("0.015")
  }

  def calculateTaxDue(userAnswers: UserAnswers): Option[BigDecimal] = {
    for {
      amountPaidValue <- getAmountPaid(userAnswers)
      marketValue <- getMarketValue(userAnswers)
      taxRate <- userAnswers.get(TaxRatePage)
    } yield {
      val higherValue = amountPaidValue.max(marketValue)
      val taxBeforeRelief = higherValue * getTaxRateValue(taxRate)
      val reliefPercentage = getReliefPercentage(userAnswers).getOrElse(BigDecimal(0))
      val reliefAmount = taxBeforeRelief * reliefPercentage
      val taxAfterRelief = (taxBeforeRelief - reliefAmount).max(BigDecimal(0))
      
      taxAfterRelief.setScale(2, RoundingMode.HALF_UP)
    }
  }

  private def getAmountPaid(userAnswers: UserAnswers): Option[BigDecimal] = {
    val amountPaidForShares = userAnswers.get(DetailsOfThisTransferPage).map(_.amountPaid).getOrElse(BigDecimal(0))
    val amountPaidForSecurities = userAnswers.get(AmountPaidForSecuritiesPage).getOrElse(BigDecimal(0))
    
    Some(amountPaidForShares.max(amountPaidForSecurities))
  }

  private def getMarketValue(userAnswers: UserAnswers): Option[BigDecimal] = {
    val marketValueOfShares = userAnswers.get(DetailsOfThisTransferPage).flatMap(_.marketValue).getOrElse(BigDecimal(0))
    val totalMarketValueOfSecurities = userAnswers.get(TotalMarketValuePage).getOrElse(BigDecimal(0))
    
    Some(marketValueOfShares.max(totalMarketValueOfSecurities))
  }

  private def getReliefPercentage(userAnswers: UserAnswers): Option[BigDecimal] = {
    for {
      applyingForRelief <- userAnswers.get(ApplyingForReliefPage)
      if applyingForRelief
      reliefName <- userAnswers.get(WhatReliefAreYouApplyingForPage)
      reliefData <- reliefsDataSource.reliefs.find(_.name == reliefName)
    } yield BigDecimal(reliefData.rate) / 100
  }

  def calculatePaymentDueDate(userAnswers: UserAnswers): Option[LocalDate] = {
    userAnswers.get(ChargingPointPage).map(_.plusDays(30))
  }

  def formatTaxDue(taxDue: BigDecimal): String = {
    f"£$taxDue%.2f"
  }

  def formatDate(date: LocalDate): String = {
    val day = date.getDayOfMonth
    val month = date.getMonth.toString.toLowerCase.capitalize
    val year = date.getYear
    s"$day $month $year"
  }
}