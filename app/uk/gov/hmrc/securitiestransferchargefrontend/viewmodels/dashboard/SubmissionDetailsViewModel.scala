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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, TableRow}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.Table
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SttSubmission, SttTransfer, SubmissionStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.SubmissionDetailsSummary
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService

case class TransferTab(id: String, label: String, heading: String, emptyText: String, table: Option[Table])

case class SubmissionDetailsViewModel(submissionId: String, summary: SubmissionDetailsSummary, tabs: Seq[TransferTab])

object SubmissionDetailsViewModel {

  private val maxRecords = 10

  private def viewLink(transferRef: String)(implicit messages: Messages): HtmlContent = {
    val view = messages("submissionDetails.transfers.action.view")
    val hiddenText = messages("submissionDetails.transfers.action.view.hidden", transferRef)

    HtmlContent(
      s"""<a class="govuk-link" href="#">$view <span class="govuk-visually-hidden">$hiddenText</span></a>"""
    )
  }

  private def transfersTable(transfers: Seq[SttTransfer], formatter: FormattingService, isAgent: Boolean, submissionType: TransferType)(implicit messages: Messages): Table = {
    val middleKeys = submissionType match {
      case SH03         => Seq("shares", "for")
      case _ if isAgent => Seq("buyer", "seller")
      case _            => Seq("seller", "securitiesBoughtIn")
    }

    val headingKeys =
      Seq("reference") ++ middleKeys ++ Seq("paymentDueBy", "status", "action")

    Table(
      head = Some(
        headingKeys.map { key =>
          HeadCell(content = Text(messages(s"submissionDetails.transfers.table.$key.heading")))
        }
      ),
      rows = transfers.take(maxRecords).map { transfer =>
        val sellerNames = transfer.sellerNames.getOrElse("")

        

        val (second, third) = submissionType match {
          case SH03 => (transfer.numberOfShares.toString, messages("submissionDetails.reasonForPurchase.cancellation")) // TODO: need to retrieve the actual reason once etmp api spec is updated
          case _ if isAgent => (transfer.buyerNames, sellerNames)
          case _ => (sellerNames, transfer.companyName)
        }

        Seq(
          TableRow(content = Text(transfer.utrn)),
          TableRow(content = Text(second)),
          TableRow(content = Text(third)),
          TableRow(content = Text(formatter.formatDate(transfer.paymentDueByDate))),
          TableRow(content = StatusTag.tagHtml(transfer.status)),
          TableRow(content = viewLink(transfer.utrn))
        )
      }
    )
  }


  private def buildTabs(all: Seq[SttTransfer], formatter: FormattingService, isAgent: Boolean, submissionType: TransferType)(implicit messages: Messages): Seq[TransferTab] = Seq(
    buildTab("allTransfers", all, status = None, formatter, isAgent, submissionType),
    buildTab("readyToPay", all.filter(_.status == ReadyToPay), Some(ReadyToPay), formatter, isAgent, submissionType),
    buildTab("overdue", all.filter(_.status == Overdue), Some(Overdue), formatter, isAgent, submissionType)
  )

  private def buildTab(id: String, transfers: Seq[SttTransfer], status: Option[SubmissionStatus], formatter: FormattingService, isAgent: Boolean, submissionType: TransferType)
                      (implicit messages: Messages): TransferTab = {
    val label = messages(s"submissionDetails.$id.panel.label")

    TransferTab(
      id = messages(s"submissionDetails.$id.tabId"),
      label = status.fold(label)(status => StatusTag.labelWithTag(label, transfers.size, status)),
      heading = messages(s"submissionDetails.$id.panel.heading"),
      emptyText = messages(s"submissionDetails.$id.empty"),
      table = Option.when(transfers.nonEmpty)(transfersTable(transfers, formatter, isAgent, submissionType))
    )
  }

  def build(submission: SttSubmission, formatter: FormattingService, isAgent: Boolean = false)(implicit messages: Messages): SubmissionDetailsViewModel = {
    val submissionType = submission.submissionType
    val transfers = submission.transfers
    SubmissionDetailsViewModel(
      submissionId = submission.submissionId,
      summary = SubmissionDetailsSummary.getSummary(submission, formatter, isAgent),
      tabs = buildTabs(transfers, formatter, isAgent, submissionType)
    )
  }
}
