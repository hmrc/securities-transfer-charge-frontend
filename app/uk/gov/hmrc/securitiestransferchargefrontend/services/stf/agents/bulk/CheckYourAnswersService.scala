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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents.bulk

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.models. UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload. ParsedStcRowsDocument
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.cya.StfAgentRowBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.agents.TaxDueCalculationService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.agents.bulk.CheckYourAnswersViewModel

import javax.inject.{Inject, Singleton}

@Singleton
class CheckYourAnswersService @Inject()(
                                         taxDueCalculationService: TaxDueCalculationService
                                       ) {

  def buildViewModel(userAnswers: UserAnswers, doc: ParsedStcRowsDocument)(implicit messages: Messages): CheckYourAnswersViewModel = {
    val taxDueSummaryRows = taxDueCalculationService.buildTransferRows(doc.rows)
    val taxDue = taxDueSummaryRows.map(_.taxDue).sum
    val formattedTaxDue = taxDueCalculationService.formatCurrency(taxDue)
    val taxDueDate = taxDueCalculationService.calculatePaymentDueDate(doc.rows)
    val formattedTaxDueDate = taxDueCalculationService.formatDate(taxDueDate)

    val yourDetailsList = SummaryList(rows = StfAgentRowBuilder.buildYourDetailsRows(userAnswers))
    val fileDetailsCard = StfAgentRowBuilder.buildFileDetailsCard(doc.fileName, doc.rows.size)

    val summaryLists = Seq(yourDetailsList, fileDetailsCard)
    
    CheckYourAnswersViewModel.agentBulkSummaryLists(summaryLists, taxDueSummaryRows, formattedTaxDue, formattedTaxDueDate)
    
  }

}