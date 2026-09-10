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

package uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.organisations.bulk

import com.google.inject.Inject
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRowsDocument
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.bulk.TaxDueCalculationService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations.bulk.{CheckYourAnswersViewModel, CompanyDetailsSummary, RoleAtPurchasingCompanySummary, Sh03Transfer}

import scala.util.Try

class CheckYourAnswersService @Inject()(
                                         taxDueCalculationService: TaxDueCalculationService,
                                         formattingService: FormattingService
                                       ) {

  def buildViewModel(parsedStcRowsDocument: ParsedStcRowsDocument, userAnswers: UserAnswers)(implicit lang: Lang, messages: Messages): CheckYourAnswersViewModel = {

    val transfers: Seq[Sh03Transfer] =
      parsedStcRowsDocument.rows.map { row =>

        val amountOfShares = row.securitiesQuantity.getOrElse("0")

        val isCancellation = row.purchaseForCancellation.getOrElse(true)

        val amountPaidStr = row.amountPaidForSecurities
          .orElse(row.totalMarketValue)
          .orElse(row.maxSharePrice)
          .getOrElse("0")

        val consideration = Try(BigDecimal(amountPaidStr)).getOrElse(BigDecimal(0))
        
        val taxDue = taxDueCalculationService.calculateTaxDue(row)

        Sh03Transfer(
          amountOfShares = formatAmountOfShares(amountOfShares),
          reasonFor      = if (isCancellation) "Cancellation" else "Treasury",
          consideration  = consideration,
          taxDue         = taxDue
        )
      }

    val paymentDueBy: String =
      parsedStcRowsDocument.rows
        .flatMap(_.chargingPoint.toOption)
        .minOption
        .map(_.plusDays(30))
        .map(formattingService.formatPaymentDueDate)
        .getOrElse("")

    val totalTaxDue: BigDecimal = transfers.map(_.taxDue).sum

    CheckYourAnswersViewModel(
      companyDetailsRows = CompanyDetailsSummary.rows(userAnswers),
      fileName = parsedStcRowsDocument.fileName,
      numberOfTransfers = transfers.size,
      taxDue = taxDueCalculationService.formatCurrency(totalTaxDue),
      paymentDueBy = paymentDueBy,
      transfers = transfers,
      declarationRows = RoleAtPurchasingCompanySummary.rows(userAnswers)
    )
  }

  private def formatAmountOfShares(quantity: String): String =
    Try(quantity.toInt).toOption.map(CommonHelpers.formatWithCommas).getOrElse(quantity)
}