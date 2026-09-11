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

package uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.bulk

import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.ReliefsDataSource
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow

import java.text.NumberFormat
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.math.BigDecimal.RoundingMode
import scala.util.Try

@Singleton
class TaxDueCalculationService @Inject()(
                                          reliefsDataSource: ReliefsDataSource,
                                          appConfig: FrontendAppConfig
                                        ) {

  private val taxRate = appConfig.taxRateSH03

  def calculateTaxDue(row: ParsedStcRow): BigDecimal = {
    val amountPaid = row.amountPaidForSecurities.flatMap(s => Try(BigDecimal(s)).toOption).getOrElse(BigDecimal(0))
    val marketValue = row.totalMarketValue.flatMap(s => Try(BigDecimal(s)).toOption).getOrElse(BigDecimal(0))
    
    val higherValue = amountPaid.max(marketValue)
    val taxBeforeRelief = higherValue * taxRate
    
    val reliefPercentage = getReliefPercentage(row).getOrElse(BigDecimal(0))
    val reliefAmount = taxBeforeRelief * reliefPercentage
    val taxAfterRelief = (taxBeforeRelief - reliefAmount).max(BigDecimal(0))
    
    taxAfterRelief.setScale(2, RoundingMode.HALF_UP)
  }

  private def getReliefPercentage(row: ParsedStcRow): Option[BigDecimal] = {
    for {
      applyingForRelief <- row.applyingForRelief
      if applyingForRelief
      reliefName <- row.whatReliefAreYouApplyingFor
      reliefData <- reliefsDataSource.reliefs.find(_.name == reliefName)
    } yield BigDecimal(reliefData.rate) / 100
  }
  
  def formatCurrency(amount: BigDecimal): String = {
    val formatter = NumberFormat.getCurrencyInstance(Locale.UK)
    formatter.format(amount)
  }
}