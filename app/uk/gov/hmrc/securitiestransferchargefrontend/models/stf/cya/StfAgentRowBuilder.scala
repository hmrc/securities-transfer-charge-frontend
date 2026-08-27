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

package uk.gov.hmrc.securitiestransferchargefrontend.models.stf.cya

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{Card, CardTitle, SummaryList, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.agents.*


object StfAgentRowBuilder  {

  def buildYourDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(AgentReferenceSummary.row(userAnswers)).flatten
  }

  def buildBuyerDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      NameofBuyerSummary.row(userAnswers),
      StfBuyersAddressSummary.row(userAnswers)
    ).flatten
  }

  def buildSellerDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(
      NameOfSellerSummary.row(userAnswers),
      StfSellerAddressSummary.row(userAnswers)
    ).flatten
  }


  def buildTransferDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val applyingForRelief = userAnswers.get(ApplyingForReliefPage).contains(true)

    val baseRows = Seq(
      ConnectedPersonsSummary.row(userAnswers),
      ApplyingForReliefSummary.row(userAnswers),
    ).flatten

    val reliefRow = if (applyingForRelief) {
      WhatReliefAreYouApplyingForSummary.row(userAnswers).toSeq
    } else {
      Seq.empty
    }

    val additionalRows = Seq(
      ChargingPointSummary.row(userAnswers),
      TaxRateSummary.row(userAnswers),
      PurchasingSharesSummary.row(userAnswers)
    ).flatten

    val securitiesTarget = SecuritiesTargetSummary.row(userAnswers).getOrElse(Seq.empty)

    baseRows ++ reliefRow ++ securitiesTarget ++ additionalRows ++ buildSecuritiesDetailsRows(userAnswers)(messages)
  }

  def buildSecuritiesDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val purchasingShares = userAnswers.get(PurchasingSharesPage).getOrElse(false)
    val isConnectedPersons = userAnswers.get(ConnectedPersonsPage).contains(true)
    if (purchasingShares)
      buildSharesDetailsRows(userAnswers, isConnectedPersons)
    else
      buildOtherSecuritiesDetailsRows(userAnswers, isConnectedPersons)
  }

  def buildFileDetailsCard(fileName: String, rows: Int)(implicit messages: Messages): SummaryList = {
    SummaryList(
      card = Some(Card(
        title = Some(CardTitle(content = Text(messages("agent.checkYourAnswers.fileDetails.heading"))))
      )),
      rows = Seq(
        FileDetailsSummary.row(fileName),
        NumberOfRowsSummary.row(rows)
      ).flatten
    )
  }

  private def buildSharesDetailsRows(
                                      userAnswers: UserAnswers,
                                      isConnectedPersons: Boolean
                                    )(implicit messages: Messages): Seq[SummaryListRow] = {
    DetailsOfThisTransferSummary.rows(userAnswers, showMarketValue = isConnectedPersons).getOrElse(Seq.empty)
  }

  private def buildOtherSecuritiesDetailsRows(
                                               userAnswers: UserAnswers,
                                               isConnectedPersons: Boolean
                                             )(implicit messages: Messages): Seq[SummaryListRow] = {
    val otherTypeRow = OtherSecuritiesTypeSummary.row(userAnswers)
    val amountPaidRow = AmountPaidForSecuritiesSummary.row(userAnswers)
    val marketValueRow = if (isConnectedPersons) TotalMarketValueSummary.row(userAnswers) else Seq.empty
    Seq(otherTypeRow, amountPaidRow, marketValueRow).flatten
  }
}