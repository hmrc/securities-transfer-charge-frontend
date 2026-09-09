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

package uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.bulk

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Card, CardTitle, SummaryList, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.bulk.*


object Sh03AgentRowBuilder  {

  def buildYourDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(AgentReferenceSummary.row(userAnswers))
  }

  def buildBuyerDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(CompanyDetailsSummary.rows(userAnswers)).flatten
  }
  

  def buildFileDetailsCard(fileName: String, rows: Int)(implicit messages: Messages): SummaryList = {
    val formattedRows = CommonHelpers.formatWithCommas(rows)
    SummaryList(
      card = Some(Card(
        title = Some(CardTitle(content = Text(messages("agent.checkYourAnswers.fileDetails.heading"))))
      )),
      rows = Seq(
        FileDetailsSummary.row(fileName),
        NumberOfRowsSummary.row(formattedRows)
      ).flatten
    )
  }
  
  def buildDeclarationRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(RoleAtPurchasingCompanySummary.row(userAnswers),
      UksOrganSummary.row(userAnswers)
      
    ).flatten
  }
}