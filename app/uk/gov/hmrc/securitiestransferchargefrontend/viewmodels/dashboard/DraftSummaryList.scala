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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard

import play.api.i18n.{Lang, Messages}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.Aliases.{HtmlContent, Key, SummaryListRow, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats.dateTimeFormat

object DraftSummaryList {

  def build(summary: DraftSummary, affinityKey: String, journeyType: JourneyType)(implicit messages: Messages): Seq[(String, SummaryList)] = {
    implicit val lang: Lang = messages.lang

    def row(key: String, value: String): SummaryListRow = SummaryListRow(key = Key(Text(messages(key))), value = Value(Text(value)))

    def yesNo(value: Boolean): String = if (value) messages("site.yes") else messages("site.no")

    def addressRow(key: String, lines: Seq[String]): SummaryListRow =
      SummaryListRow(
        key = Key(Text(messages(key))),
        value = Value(HtmlContent(lines.map(HtmlFormat.escape(_).toString).mkString("<br>")))
      )

    def agentReferenceRows(agentReference: Option[AgentReference]): Seq[SummaryListRow] = {
      val reference = agentReference.flatMap(_.agentReference)
      if (affinityKey == "agent")
        Seq(row("agentReference.checkYourAnswersLabel", reference.getOrElse(messages("site.notProvided"))))
      else
        reference.map(row("agentReference.checkYourAnswersLabel", _)).toSeq
    }

    lazy val sh03Summary = {
      def sh03Key(base: String): String = s"$affinityKey.sh03.$base"

      val yourDetailsRows = agentReferenceRows(summary.sh03AgentReference)

      val buyerDetailsRows =
        summary.sh03CompanyDetails.toSeq.flatMap { details =>
          Seq(
            row(sh03Key("companyDetails.companyName.label"), details.companyName),
            row(sh03Key("companyDetails.crn.label"), details.companyRegistrationNumber),
            row(sh03Key("companyDetails.isPlc.label"), yesNo(details.isPlc))
          )
        }

      val transferDetailsRows =
        summary.sh03ReasonForPurchase.map(v => row(sh03Key("reasonForPurchase.checkYourAnswersLabel"), messages(s"reasonForPurchase.$v"))).toSeq ++
          summary.sh03TreasuryShares.map(v => row(sh03Key("treasuryShares.checkYourAnswersLabel"), yesNo(v))).toSeq ++
          summary.sh03ConnectedPersons.map(v => row(sh03Key("connectedPersons.checkYourAnswersLabel"), yesNo(v))).toSeq ++
          summary.sh03ApplyingForRelief.map(v => row(sh03Key("applyingForRelief.checkYourAnswersLabel"), yesNo(v))).toSeq ++
          summary.sh03WhatReliefAreYouApplyingFor.map(row(sh03Key("whatReliefAreYouApplyingFor.checkYourAnswersLabel"), _)).toSeq ++
          summary.sh03DetailsOfThisSharePurchase.toSeq.flatMap { details =>
            Seq(
              row(sh03Key("detailsOfSharePurchase.numberOfShares"), details.numberOfShares.toString),
              row(sh03Key("detailsOfSharePurchase.typeOfShares"), details.typeOfShares),
              row(sh03Key("detailsOfSharePurchase.amountPaid"), currencyFormat(details.amountPaid))
            ) ++ details.marketValue.map(v => row(sh03Key("detailsOfSharePurchase.marketValue"), currencyFormat(v))).toSeq
          } ++
          summary.sh03MaximumAmountPaid.map(v => row(sh03Key("maximumAmountPaid.checkYourAnswersLabel"), currencyFormat(v))).toSeq ++
          summary.sh03MinimumAmountPaid.map(v => row(sh03Key("minimumAmountPaid.checkYourAnswersLabel"), currencyFormat(v))).toSeq ++
          summary.sh03ChargingPoint.map(v => row(sh03Key("chargingPoint.checkYourAnswersLabel"), v.format(dateTimeFormat()))).toSeq ++
          summary.sh03RoleAtPurchasingCompany.toSeq.flatMap { role =>
            Seq(row(sh03Key("roleAtPurchasingCompany.checkYourAnswersLabel"), messages(sh03Key(s"roleAtPurchasingCompany.${role.role}")))) ++
              role.uksOrgan.map(row(sh03Key("roleAtPurchasingCompany.uksOrgan.label"), _)).toSeq
          }

      Seq(
        messages("draft.yourDetails.heading") -> SummaryList(rows = yourDetailsRows),
        messages("draft.buyerDetails.heading") -> SummaryList(rows = buyerDetailsRows),
        messages("draft.transferDetails.heading") -> SummaryList(rows = transferDetailsRows)
      )
    }

    lazy val stfSummary = {

      def stfKey(base: String): String = if (affinityKey == "individual") base else s"$affinityKey.$base"

      def agentOr(agentKey: String, otherKey: String): String = if (affinityKey == "agent") agentKey else otherKey

      val yourDetailsRows = agentReferenceRows(summary.stfAgentReference)

      val buyerAddress: Option[Seq[String]] =
        summary.stfConfirmedAddress
          .map(a => a.lines ++ Seq(a.postcode) ++ a.country.map(_.name).toSeq)
          .orElse(summary.stfBuyerAddress.map(a => a.address.lines ++ Seq(a.address.postcode, a.address.country.name)))

      val buyerDetailsRows =
        summary.stfNameOfBuyer.map(row("agent.nameOfBuyer.checkYourAnswersLabel", _)).toSeq ++
          buyerAddress.map(addressRow(agentOr("agent.checkYourAnswers.buyerAddress", "checkYourAnswers.confirmAddress"), _)).toSeq

      val sellerDetailsRows =
        summary.stfNameOfSeller.map(row(agentOr("agent.nameOfSeller.checkYourAnswersLabel", "nameOfSeller.checkYourAnswersLabel"), _)).toSeq ++
          summary.stfSellerAddress.map { a =>
            addressRow(
              agentOr("agent.checkYourAnswers.sellerAddress", "checkYourAnswers.sellerAddress"),
              a.address.lines ++ Seq(a.address.postcode, a.address.country.name)
            )
          }.toSeq

      val transferDetailsRows =
        summary.stfConnectedPersons.map(v => row(stfKey("connectedPersons.checkYourAnswersLabel"), yesNo(v))).toSeq ++
          summary.stfApplyingForRelief.map(v => row(stfKey("applyingForRelief.checkYourAnswersLabel"), yesNo(v))).toSeq ++
          summary.stfWhatReliefAreYouApplyingFor.map(row(agentOr("agent.whatReliefAreYouApplyingFor.checkYourAnswersLabel", "whatReliefAreYouApplyingFor.checkYourAnswersLabel"), _)).toSeq ++
          summary.stfSecuritiesTarget.toSeq.flatMap { target =>
            val businessNameKey = if (affinityKey == "org") "org.checkYourAnswers.businessName" else "checkYourAnswers.businessName"
            Seq(row(businessNameKey, target.businessName)) ++
              target.crn.map(row("checkYourAnswers.companyCrn", _)).toSeq
          } ++
          summary.stfChargingPoint.map(v => row(stfKey("chargingPoint.checkYourAnswersLabel"), v.format(dateTimeFormat()))).toSeq ++
          summary.stfTaxRate.map(v => row("taxRate.checkYourAnswersLabel", messages(s"taxRate.$v"))).toSeq ++
          summary.stfPurchasingShares.map(v => row(s"$affinityKey.purchasingShares.checkYourAnswersLabel", yesNo(v))).toSeq ++
          summary.stfDetailsOfThisTransfer.toSeq.flatMap { details =>
            val amountPaidKey = if (affinityKey == "org") "org.detailsOfThisTransfer.amountPaid.checkYourAnswersLabel" else "checkYourAnswers.amountPaidForShares"
            Seq(
              row("checkYourAnswers.numberOfShares", details.numberOfShares.toString),
              row("checkYourAnswers.typeOfShares", details.typeOfShares),
              row(amountPaidKey, currencyFormat(details.amountPaid))
            ) ++ details.marketValue.map(v => row("checkYourAnswers.marketValueOfShares", currencyFormat(v))).toSeq
          } ++
          summary.stfOtherSecuritiesType.map(row(stfKey("otherSecuritiesType.checkYourAnswersLabel"), _)).toSeq ++
          summary.stfAmountPaidForSecurities.map(v => row(stfKey("amountPaidForSecurities.checkYourAnswersLabel"), currencyFormat(v))).toSeq ++
          summary.stfTotalMarketValue.map(v => row(stfKey("totalMarketValue.checkYourAnswersLabel"), currencyFormat(v))).toSeq

      val buyerSections =
        if (affinityKey == "individual")
          Seq(messages("draft.yourDetails.heading") -> SummaryList(rows = yourDetailsRows ++ buyerDetailsRows))
        else
          Seq(
            messages("draft.yourDetails.heading") -> SummaryList(rows = yourDetailsRows),
            messages("draft.buyerDetails.heading") -> SummaryList(rows = buyerDetailsRows)
          )

      buyerSections ++ Seq(
        messages("draft.sellerDetails.heading") -> SummaryList(rows = sellerDetailsRows),
        messages("draft.transferDetails.heading") -> SummaryList(rows = transferDetailsRows)
      )
    }

    journeyType match {
      case JourneyType.SH03 => sh03Summary
      case JourneyType.STF => stfSummary
    }
  }
}