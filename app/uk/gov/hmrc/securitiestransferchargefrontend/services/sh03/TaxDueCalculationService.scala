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

package uk.gov.hmrc.securitiestransferchargefrontend.services.sh03

import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ReliefsDataSource, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.{ApplyingForReliefPage, ChargingPointPage, DetailsOfThisSharePurchasePage, WhatReliefAreYouApplyingForPage}

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.math.BigDecimal.RoundingMode

@Singleton
class TaxDueCalculationService @Inject()(
                                          reliefsDataSource: ReliefsDataSource,
                                          appConfig: FrontendAppConfig
                                        ) {
  
  private val taxRate = appConfig.taxRateSH03

  def calculateTaxDue(userAnswers: UserAnswers): BigDecimal = {
    val amountPaidValue = getAmountPaid(userAnswers)
    val marketValue = getMarketValue(userAnswers)

    val higherValue = amountPaidValue.max(marketValue)
    val taxBeforeRelief = higherValue * taxRate
    val reliefPercentage = getReliefPercentage(userAnswers).getOrElse(BigDecimal(0))
    val reliefAmount = taxBeforeRelief * reliefPercentage
    val taxAfterRelief = (taxBeforeRelief - reliefAmount).max(BigDecimal(0))

    taxAfterRelief.setScale(2, RoundingMode.HALF_UP)
  }

  private def getAmountPaid(userAnswers: UserAnswers): BigDecimal = {
    userAnswers.get(DetailsOfThisSharePurchasePage).map(_.amountPaid).getOrElse(BigDecimal(0))
  }

  private def getMarketValue(userAnswers: UserAnswers): BigDecimal = {
    userAnswers.get(DetailsOfThisSharePurchasePage).flatMap(_.marketValue).getOrElse(BigDecimal(0))
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
}