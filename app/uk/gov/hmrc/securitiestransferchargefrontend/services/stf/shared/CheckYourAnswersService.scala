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

package uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.individuals.*

import javax.inject.{Inject, Singleton}

@Singleton
class CheckYourAnswersService @Inject() {

  def buildYourDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val addressRow = userAnswers.get(ConfirmAddressPage)
      .map(_ => ConfirmAddressSummary.row(userAnswers))
      .orElse(userAnswers.get(StfBuyersAddressPage).map(_ => StfBuyersAddressSummary.row(userAnswers)))
      .getOrElse(ConfirmAddressSummary.row(userAnswers))

    Seq(addressRow)
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
      ApplyingForReliefSummary.row(userAnswers)
    )

    val reliefRow = if (applyingForRelief) {
      WhatReliefAreYouApplyingForSummary.row(userAnswers).toSeq
    } else {
      Seq.empty
    }

    val additionalRows = Seq(
      ChargingPointSummary.row(userAnswers),
      TaxRateSummary.row(userAnswers)
    )

    baseRows ++ reliefRow ++ SecuritiesTargetSummary.row(userAnswers) ++ additionalRows
  }

  def buildSecuritiesDetailsRows(userAnswers: UserAnswers)(implicit messages: Messages): Seq[SummaryListRow] = {
    val purchasingShares = userAnswers.get(PurchasingSharesPage).getOrElse(false)
    val isConnectedPersons = userAnswers.get(ConnectedPersonsPage).contains(true)
    val whatTypeRow = PurchasingSharesSummary.row(userAnswers)

    if (purchasingShares)
      buildSharesDetailsRows(userAnswers, whatTypeRow, isConnectedPersons)
    else
      buildOtherSecuritiesDetailsRows(userAnswers, whatTypeRow, isConnectedPersons)
  }


  private def buildSharesDetailsRows(
                                      userAnswers: UserAnswers,
                                      whatTypeRow: SummaryListRow,
                                      isConnectedPersons: Boolean
                                    )(implicit messages: Messages): Seq[SummaryListRow] = {
    val shareDetailsRows = DetailsOfThisTransferSummary.rows(userAnswers, showMarketValue = isConnectedPersons)
    Seq(whatTypeRow) ++ shareDetailsRows
  }

  private def buildOtherSecuritiesDetailsRows(
                                               userAnswers: UserAnswers,
                                               whatTypeRow: SummaryListRow,
                                               isConnectedPersons: Boolean
                                             )(implicit messages: Messages): Seq[SummaryListRow] = {
    val otherTypeRow = OtherSecuritiesTypeSummary.row(userAnswers).toSeq
    val amountPaidRow = Seq(AmountPaidForSecuritiesSummary.row(userAnswers))
    val marketValueRow = if (isConnectedPersons) {
      Seq(TotalMarketValueSummary.row(userAnswers))
    } else {
      Seq.empty
    }

    Seq(whatTypeRow) ++ otherTypeRow ++ amountPaidRow ++ marketValueRow
  }
}