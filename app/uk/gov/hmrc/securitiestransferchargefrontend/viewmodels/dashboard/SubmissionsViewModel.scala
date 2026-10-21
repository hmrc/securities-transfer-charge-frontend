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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Draft, Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.pagination.PaginationViewModel

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

case class SubmissionTab(id: String, label: String, heading: String, emptyText: String, count: Int, totalCount: Int, table: Option[Table], pagination: Option[PaginationViewModel])

case class SubmissionsViewModel(tabs: Seq[SubmissionTab])

object SubmissionsViewModel {

  private val govUkDateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK)

  private def formatDate(dateStr: String): String =
    Try(LocalDate.parse(dateStr).format(govUkDateFormatter)).getOrElse(dateStr)

  private def viewLink(submissionId: String)(implicit messages: Messages): HtmlContent = {
    val view = messages("submissions.action.view")
    val hiddenText = messages("submissions.action.view.hidden", submissionId)
    val href = routes.SubmissionDetailsController.onPageLoad(submissionId).url

    HtmlContent(
      s"""<a class="govuk-link" href="$href">$view<span class="govuk-visually-hidden">$hiddenText</span></a>"""
    )
  }

  private def submissionsTable(summaries: Seq[SubmissionSummary], isDraftsTab: Boolean = false, isAgent: Boolean)(implicit messages: Messages): Table = {

    val paymentDueByHead: Seq[HeadCell] =
      if (isDraftsTab)
        Seq.empty
      else Seq(HeadCell(content = Text(messages("submissions.table.paymentDueBy.heading")), classes = "govuk-!-text-align-right"))

    val yourReferenceHead =
      if (isAgent)
        Seq(HeadCell(content = Text(messages("submissions.table.yourReference.heading"))))
      else Seq.empty


    Table(
      head = Some(
        Seq(HeadCell(content = Text(messages("submissions.table.id.heading")))) ++
          yourReferenceHead ++
          paymentDueByHead ++
          Seq(
            HeadCell(content = Text(messages("submissions.table.status.heading"))),
            HeadCell(content = Text(messages("submissions.table.action.heading")))
          )
      ),
      rows = summaries.map { summary =>
        val paymentDueByCell: Seq[TableRow] =
          if (isDraftsTab) Seq.empty else Seq(TableRow(content = Text(formatDate(summary.paymentDueBy)), classes = "govuk-!-text-align-right"))

        val yourReferenceCell: Seq[TableRow] =
          if (isAgent) Seq(TableRow(content = Text(summary.agentReference))) else Seq.empty

        Seq(TableRow(content = Text(summary.submissionId))) ++
          yourReferenceCell ++
          paymentDueByCell ++
          Seq(
            TableRow(content = StatusTag.tagHtml(summary.status)),
            TableRow(content = viewLink(summary.submissionId))
          )
      }
    )
  }

  private def buildSubmissionsUrl(model: PaginationModel, tabId: String): String = {
    val tabParams = model.tabs.map { case (id, tabPagination) =>
      id -> (if (tabPagination.page != 1) Some(tabPagination.page) else None)
    }

    routes.SubmissionsController.onPageLoad(
      draftsPage = tabParams.getOrElse("drafts", None),
      readyToPayPage = tabParams.getOrElse("ready-to-pay", None),
      overduePage = tabParams.getOrElse("overdue", None),
      recentPage = tabParams.getOrElse("recent-submissions", None)
    ).url + s"#$tabId"
  }

  private def buildTabs(
                         drafts: Seq[SubmissionSummary],
                         readyToPay: Seq[SubmissionSummary],
                         overdue: Seq[SubmissionSummary],
                         recent: Seq[SubmissionSummary],
                         isAgent: Boolean,
                         paginationModel: PaginationModel
                       )(implicit messages: Messages): Seq[SubmissionTab] = {

    val recentTabId = messages("submissions.recentSubmissions.tabId")
    val draftsTabId = messages("submissions.drafts.tabId")
    val readyToPayTabId = messages("submissions.readyToPay.tabId")
    val overdueTabId = messages("submissions.overdue.tabId")

    // Create pagination with submissions-specific URL builder
    val recentPagination = paginationModel.createPaginationIfNeeded(recentTabId, (model, _) => buildSubmissionsUrl(model, recentTabId))
    val draftsPagination = paginationModel.createPaginationIfNeeded(draftsTabId, (model, _) => buildSubmissionsUrl(model, draftsTabId))
    val readyToPayPagination = paginationModel.createPaginationIfNeeded(readyToPayTabId, (model, _) => buildSubmissionsUrl(model, readyToPayTabId))
    val overduePagination = paginationModel.createPaginationIfNeeded(overdueTabId, (model, _) => buildSubmissionsUrl(model, overdueTabId))

    val recentSubmissionsTab: SubmissionTab = SubmissionTab(
      id = recentTabId,
      label = messages("submissions.recentSubmissions.panel.heading"),
      heading = messages("submissions.recentSubmissions.panel.heading"),
      emptyText = messages("submissions.noRecentSubmissions"),
      count = recent.size,
      totalCount = paginationModel.getTabTotal(recentTabId),
      table = Some(submissionsTable(recent, isAgent = isAgent)),
      pagination = recentPagination
    )

    val draftSubmissionsTab: SubmissionTab = SubmissionTab(
      id = draftsTabId,
      label = StatusTag.labelWithTag(messages("submissions.drafts.panel.heading"), paginationModel.getTabTotal(draftsTabId), Draft),
      heading = messages("submissions.drafts.panel.heading"),
      emptyText = messages("submissions.noDraftSubmissions"),
      count = drafts.size,
      totalCount = paginationModel.getTabTotal(draftsTabId),
      table = Some(submissionsTable(drafts, isDraftsTab = true, isAgent = isAgent)),
      pagination = draftsPagination
    )

    val readyToPaySubmissionsTab: SubmissionTab = SubmissionTab(
      id = readyToPayTabId,
      label = StatusTag.labelWithTag(messages("submissions.readyToPay.panel.heading"), paginationModel.getTabTotal(readyToPayTabId), ReadyToPay),
      heading = messages("submissions.readyToPay.panel.heading"),
      emptyText = messages("submissions.noReadyToPaySubmissions"),
      count = readyToPay.size,
      totalCount = paginationModel.getTabTotal(readyToPayTabId),
      table = Some(submissionsTable(readyToPay, isAgent = isAgent)),
      pagination = readyToPayPagination
    )

    val overdueSubmissionsTab: SubmissionTab = SubmissionTab(
      id = overdueTabId,
      label = StatusTag.labelWithTag(messages("submissions.overdue.panel.heading"), paginationModel.getTabTotal(overdueTabId), Overdue),
      heading = messages("submissions.overdue.panel.heading"),
      emptyText = messages("submissions.noOverdueSubmissions"),
      count = overdue.size,
      totalCount = paginationModel.getTabTotal(overdueTabId),
      table = Some(submissionsTable(overdue, isAgent = isAgent)),
      pagination = overduePagination
    )

    Seq(recentSubmissionsTab, draftSubmissionsTab, readyToPaySubmissionsTab, overdueSubmissionsTab)
  }

  def build(
             allDrafts: Seq[SubmissionSummary] = Seq.empty,
             allReadyToPay: Seq[SubmissionSummary] = Seq.empty,
             allOverdue: Seq[SubmissionSummary] = Seq.empty,
             allRecent: Seq[SubmissionSummary] = Seq.empty,
             isAgent: Boolean = false,
             paginationModel: PaginationModel
           )(implicit messages: Messages): SubmissionsViewModel = {

    val recentTabId = messages("submissions.recentSubmissions.tabId")
    val draftsTabId = messages("submissions.drafts.tabId")
    val readyToPayTabId = messages("submissions.readyToPay.tabId")
    val overdueTabId = messages("submissions.overdue.tabId")

    val paginatedDrafts = paginationModel.paginateSubmissions(allDrafts, paginationModel.getTabPage(draftsTabId))
    val paginatedReadyToPay = paginationModel.paginateSubmissions(allReadyToPay, paginationModel.getTabPage(readyToPayTabId))
    val paginatedOverdue = paginationModel.paginateSubmissions(allOverdue, paginationModel.getTabPage(overdueTabId))
    val paginatedRecent = paginationModel.paginateSubmissions(allRecent, paginationModel.getTabPage(recentTabId))

    SubmissionsViewModel(
      tabs = buildTabs(paginatedDrafts, paginatedReadyToPay, paginatedOverdue, paginatedRecent, isAgent, paginationModel)
    )
  }
}