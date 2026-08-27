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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents

import uk.gov.hmrc.securitiestransferchargefrontend.models.ReliefsDataSource
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.agents.bulk.TransferRow

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.math.BigDecimal.RoundingMode

@Singleton
class TaxDueCalculationService @Inject()(reliefsDataSource: ReliefsDataSource) {

  def buildTransferRows(rows: Seq[ParsedStcRow]): Seq[TransferRow] = {
    rows.map { row =>
      TransferRow(
        buyerName = row.buyerName.getOrElse(throw new IllegalStateException(s"buyerName missing on row ${row.rowNumber}")),
        sellerName = row.sellerName.getOrElse(throw new IllegalStateException(s"sellerName missing on row ${row.rowNumber}")),
        consideration = BigDecimal(row.amountPaidForSecurities.getOrElse(throw new IllegalStateException(s"amountPaidForSecurities missing on row ${row.rowNumber}"))),
        taxDue = calculateTaxDueForRow(row).getOrElse(throw new IllegalStateException(s"Unable to calculate tax due for row ${row.rowNumber}"))
      )
    }
  }

  def calculateTaxDueForRow(row: ParsedStcRow): Option[BigDecimal] = {
    for {
      amountPaid <- row.amountPaidForSecurities.map(BigDecimal(_))
      marketValue <- row.totalMarketValue.map(BigDecimal(_))
      taxRate <- row.taxRate.map(_ / 100)
    } yield {
      val higherValue = amountPaid.max(marketValue)
      val taxBeforeRelief = higherValue * taxRate 
      val reliefPercentage = getReliefPercentage(row).getOrElse(BigDecimal(0))
      val reliefAmount = taxBeforeRelief * reliefPercentage
      val taxAfterRelief = (taxBeforeRelief - reliefAmount).max(BigDecimal(0))

      taxAfterRelief.setScale(2, RoundingMode.HALF_UP)
    }
  }

  def calculatePaymentDueDate(rows: Seq[ParsedStcRow]): LocalDate = {
    val chargingDates: Seq[LocalDate] = rows.flatMap(_.chargingPoint.toOption)
    chargingDates.min.plusDays(30)
  }

  def formatCurrency(amount: BigDecimal): String = {
    f"£$amount%.2f"
  }

  def formatDate(date: LocalDate): String = {
    val day = date.getDayOfMonth
    val month = date.getMonth.toString.toLowerCase.capitalize
    val year = date.getYear
    s"$day $month $year"
  }

  private def getReliefPercentage(row: ParsedStcRow): Option[BigDecimal] = {
    for {
      applyingForRelief <- row.applyingForRelief
      if applyingForRelief
      reliefName <- row.whatReliefAreYouApplyingFor
      reliefData <- reliefsDataSource.reliefs.find(_.name == reliefName)
    } yield BigDecimal(reliefData.rate) / 100
  }

}