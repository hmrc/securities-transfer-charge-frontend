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

import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats
import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.Aliases.{HeadCell, TableRow}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.{HtmlContent, Text}
import uk.gov.hmrc.govukfrontend.views.viewmodels.table.Table
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SttSubmission, SttTransfer, SubmissionStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.SubmissionDetailsSummary

case class TransferTab(id: String, label: String, heading: String, emptyText: String, table: Option[Table])

case class SubmissionDetailsViewModel(submissionId: String, summary: SubmissionDetailsSummary, tabs: Seq[TransferTab])

object SubmissionDetailsViewModel {

  private val maxRecords = 10

  private def viewLink(transferRef: String, href: String)(implicit messages: Messages): HtmlContent = {
    val view = messages("submissionDetails.transfers.action.view")
    val hiddenText = messages("submissionDetails.transfers.action.view.hidden", transferRef)

    HtmlContent(
      s"""<a class="govuk-link" href="$href">$view <span class="govuk-visually-hidden">$hiddenText</span></a>"""
    )
  }

  // The number comes from the transfer's UTRN order, not the order the table is displayed in.
  private def viewHref(submission: SttSubmission, transfer: SttTransfer): String =
    submission.transferNumber(transfer) match {
      case Some(number) => dashboardRoutes.TransferDetailsController.onPageLoad(submission.submissionId, number).url
      case None         => "#"
    }

  private def transfersTable(submission: SttSubmission, transfers: Seq[SttTransfer], isAgent: Boolean)(implicit messages: Messages): Table = {
    val submissionType = submission.submissionType
    val middleKeys = submissionType match {
      case SH03         => Seq("shares", "for")
      case STF if isAgent => Seq("buyer", "seller")
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
          case STF if isAgent => (transfer.buyerNames, sellerNames)
          case _ => (sellerNames, transfer.companyName)
        }

        Seq(
          TableRow(content = Text(transfer.utrn)),
          TableRow(content = Text(second)),
          TableRow(content = Text(third)),
          TableRow(content = Text(DateTimeFormats.formatDate(transfer.paymentDueByDate)(messages.lang))),
          TableRow(content = StatusTag.tagHtml(transfer.status)),
          TableRow(content = viewLink(transfer.utrn, viewHref(submission, transfer)))
        )
      }
    )
  }


  private def buildTabs(submission: SttSubmission, all: Seq[SttTransfer], isAgent: Boolean)(implicit messages: Messages): Seq[TransferTab] = Seq(
    buildTab(submission, "allTransfers", all, status = None, isAgent),
    buildTab(submission, "readyToPay", all.filter(_.status == ReadyToPay), Some(ReadyToPay), isAgent),
    buildTab(submission, "overdue", all.filter(_.status == Overdue), Some(Overdue), isAgent)
  )

  private def buildTab(submission: SttSubmission, id: String, transfers: Seq[SttTransfer], status: Option[SubmissionStatus], isAgent: Boolean)
                      (implicit messages: Messages): TransferTab = {
    val label = messages(s"submissionDetails.$id.panel.label")

    TransferTab(
      id = messages(s"submissionDetails.$id.tabId"),
      label = status.fold(label)(status => StatusTag.labelWithTag(label, transfers.size, status)),
      heading = messages(s"submissionDetails.$id.panel.heading"),
      emptyText = messages(s"submissionDetails.$id.empty"),
      table = Option.when(transfers.nonEmpty)(transfersTable(submission, transfers, isAgent))
    )
  }

  def build(submission: SttSubmission, isAgent: Boolean = false)(implicit messages: Messages): SubmissionDetailsViewModel = {
    val transfers = SttTransfer.sorted(submission.transfers)
    SubmissionDetailsViewModel(
      submissionId = submission.submissionId,
      summary = SubmissionDetailsSummary.getSummary(submission, isAgent),
      tabs = buildTabs(submission, transfers, isAgent)
    )
  }
}
