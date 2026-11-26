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
import uk.gov.hmrc.govukfrontend.views.Aliases.{Key, SummaryListRow, Text, Value}
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.RoleAtPurchasingCompany
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats.dateTimeFormat

object DraftSummaryList {

  def build(summary: DraftSummary, affinityKey: String, journeyType: JourneyType)(implicit messages: Messages): Seq[(String, SummaryList)] = {
    implicit val lang: Lang = messages.lang

    def row(key: String, value: String): SummaryListRow = SummaryListRow(key = Key(Text(messages(key))), value = Value(Text(value)))

    def yesNo(value: Boolean): String = if (value) messages("site.yes") else messages("site.no")

    def agentReferenceRows(agentReference: Option[AgentReference]): Seq[SummaryListRow] = {
      val reference = agentReference.flatMap(_.agentReference)
      if (affinityKey == "agent")
        Seq(row("agentReference.checkYourAnswersLabel", reference.getOrElse(messages("site.notProvided"))))
      else
        reference.map(row("agentReference.checkYourAnswersLabel", _)).toSeq
    }

    lazy val sh03Summary = {
      def sh03Key(base: String): String = s"$affinityKey.sh03.$base"

      def companyRows(companyName: String, crn: String): Seq[SummaryListRow] =
        Seq(
          row(sh03Key("companyDetails.companyName.label"), companyName),
          row(sh03Key("companyDetails.crn.label"), crn)
        )

      def roleRows(role: Option[RoleAtPurchasingCompany]): Seq[SummaryListRow] =
        role.toSeq.flatMap { role =>
          Seq(row(sh03Key("roleAtPurchasingCompany.checkYourAnswersLabel"), messages(sh03Key(s"roleAtPurchasingCompany.${role.role}")))) ++
            role.uksOrgan.map(row(sh03Key("roleAtPurchasingCompany.uksOrgan.label"), _)).toSeq
        }

      // An organisation is the buyer, so its company details sit under "Your details" (with any agent reference)
      // rather than a separate "Buyer details" section.
      def detailsSections(agentReference: Option[AgentReference], companyDetailsRows: Seq[SummaryListRow]): Seq[(String, SummaryList)] =
        if (affinityKey == "org")
          Seq(messages("draft.yourDetails.heading") -> SummaryList(rows = agentReferenceRows(agentReference) ++ companyDetailsRows))
        else
          Seq(
            messages("draft.yourDetails.heading") -> SummaryList(rows = agentReferenceRows(agentReference)),
            messages("draft.buyerDetails.heading") -> SummaryList(rows = companyDetailsRows)
          )

      val isBulk = summary.sh03BulkCompanyDetails.isDefined || summary.sh03BulkAgentReference.isDefined
      val fileNotUploaded = messages("draft.fileDetails.fileName.value")

      if (isBulk) {
        // The file details are only shown once the user has reached the declaration (role) page.
        val fileDetailsRows =
          if (summary.sh03BulkRoleAtPurchasingCompany.isDefined) Seq(row("draft.fileDetails.fileName.key", fileNotUploaded))
          else Seq.empty

        detailsSections(
          summary.sh03BulkAgentReference,
          summary.sh03BulkCompanyDetails.toSeq.flatMap(details => companyRows(details.companyName, details.companyRegistrationNumber))
        ) ++ Seq(
          messages("draft.fileDetails.heading") -> SummaryList(rows = fileDetailsRows),
          messages("draft.declaration.heading") -> SummaryList(rows = roleRows(summary.sh03BulkRoleAtPurchasingCompany))
        )
      } else {
        val buyerDetailsRows =
          summary.sh03CompanyDetails.toSeq.flatMap { details =>
            companyRows(details.companyName, details.companyRegistrationNumber) :+
              row(sh03Key("companyDetails.isPlc.label"), yesNo(details.isPlc))
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
            roleRows(summary.sh03RoleAtPurchasingCompany)

        detailsSections(summary.sh03AgentReference, buyerDetailsRows) ++ Seq(
          messages("draft.transferDetails.heading") -> SummaryList(rows = transferDetailsRows)
        )
      }
    }

    journeyType match {
      case JourneyType.SH03 => sh03Summary
      case JourneyType.STF => ???
    }
  }
}