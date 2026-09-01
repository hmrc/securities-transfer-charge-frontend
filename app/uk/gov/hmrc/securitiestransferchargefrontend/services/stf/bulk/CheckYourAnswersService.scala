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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk

import com.google.inject.Inject
import play.api.i18n.Lang
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRowsDocument
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.{CheckYourAnswersViewModel, Transfer}

class CheckYourAnswersService @Inject()(
                                         taxDueCalculationService: TaxDueCalculationService,
                                         formattingService: FormattingService
                                       ) {

  def buildViewModel(parsedStcRowsDocument:  ParsedStcRowsDocument)(implicit lang: Lang): CheckYourAnswersViewModel = {

    val transfers: Seq[Transfer] =
      parsedStcRowsDocument.rows.flatMap { row =>
        for {
          seller             <- row.sellerName
          securitiesBoughtIn <- row.securitiesTarget
          amountPaid         <- row.amountPaidForSecurities
          taxDue             <- taxDueCalculationService.calculateTaxDue(row)
        } yield Transfer(
          seller = seller,
          securitiesBoughtIn = securitiesBoughtIn,
          consideration = BigDecimal(amountPaid),
          taxDue = taxDue
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
      fileName = parsedStcRowsDocument.fileName,
      numberOfTransfers = transfers.size,
      taxDue = taxDueCalculationService.formatCurrency(totalTaxDue),
      paymentDueBy = paymentDueBy,
      transfers = transfers
    )
  }
}