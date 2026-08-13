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

package uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.shared

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.{AgentReferenceSummary, ApplyingForReliefSummary, ChargingPointSummary, CompanyDetailsSummary, ConnectedPersonsSummary, DetailsOfThisSharePurchaseSummary, MaximumAmountPaidSummary, MinimumAmountPaidSummary, RoleAtPurchasingCompanySummary, TreasurySharesSummary, WhatReliefAreYouApplyingForSummary}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.shared.*

import javax.inject.{Inject, Singleton}

@Singleton
class CheckYourAnswersService @Inject() {

  def buildYourDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    Seq(AgentReferenceSummary.row(userAnswers)).flatten
  }

  def buildBuyerDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    CompanyDetailsSummary.rows(userAnswers)
  }

  def buildTransferDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val isForCancellation = userAnswers.get(ReasonForPurchasePage).contains(ReasonForPurchase.ForCancellation)
    val isConnected = userAnswers.get(ConnectedPersonsPage).contains(true)
    val applyingForRelief = userAnswers.get(ApplyingForReliefPage).contains(true)
    val isPlc = userAnswers.get(CompanyDetailsPage).exists(_.isPlc)

    val treasurySharesRow = if (isForCancellation) {
      Seq(TreasurySharesSummary.row(userAnswers)).flatten
    } else Seq.empty

    val reliefTypeRow = if (applyingForRelief) {
      Seq(WhatReliefAreYouApplyingForSummary.row(userAnswers)).flatten
    } else Seq.empty

    val maxMinAmountRows = if (isPlc) {
      Seq(
        MaximumAmountPaidSummary.row(userAnswers),
        MinimumAmountPaidSummary.row(userAnswers)
      ).flatten
    } else Seq.empty

    Seq(ReasonForPurchaseSummary.row(userAnswers)).flatten ++
      treasurySharesRow ++
      Seq(
        ConnectedPersonsSummary.row(userAnswers),
        ApplyingForReliefSummary.row(userAnswers)
      ).flatten ++
      reliefTypeRow ++
      DetailsOfThisSharePurchaseSummary.rows(userAnswers, showMarketValue = isConnected) ++
      maxMinAmountRows ++
      Seq(ChargingPointSummary.row(userAnswers)).flatten
  }

  def buildDeclarationRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    RoleAtPurchasingCompanySummary.rows(userAnswers)
  }
}