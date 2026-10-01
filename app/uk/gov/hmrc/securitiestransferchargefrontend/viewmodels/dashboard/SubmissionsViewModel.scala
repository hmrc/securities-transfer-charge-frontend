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
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

case class SubmissionTab(id: String, label: String, heading: String, emptyText: String, count: Int, table: Option[Table])

case class SubmissionsViewModel(tabs: Seq[SubmissionTab])

object SubmissionsViewModel {

  private val maxRecords = 10
  private val govUkDateFormatter = DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.UK)

  private def formatDate(dateStr: String): String =
    Try(LocalDate.parse(dateStr).format(govUkDateFormatter)).getOrElse(dateStr)

  private def draftsTable(drafts: Seq[SubmissionSummary])(implicit messages: Messages): Table =
    Table(
      head = Some(
        Seq(
          HeadCell(content = Text(messages("submissions.drafts.table.id.heading"))),
          HeadCell(content = Text(messages("submissions.drafts.table.status.heading"))),
          HeadCell(content = Text(messages("submissions.drafts.table.action.heading")))
        )
      ),
      rows = drafts.map { draft =>
        val hiddenText = messages("submissions.drafts.action.hidden", draft.submissionId)
        Seq(
          TableRow(content = Text(draft.submissionId)),
          TableRow(content = HtmlContent(s"""<strong class="govuk-tag govuk-tag--grey"> ${draft.status.toString}</strong>""")),
          TableRow(content = HtmlContent(s"""<a class="govuk-link" href="#">View<span class="govuk-visually-hidden"> $hiddenText</span></a>"""))
        )
      }
    )

  private def readyToPayTable(submissions: Seq[SubmissionSummary])(implicit messages: Messages): Table =
    Table(
      head = Some(
        Seq(
          HeadCell(content = Text(messages("submissions.readyToPay.table.id.heading"))),
          HeadCell(content = Text(messages("submissions.readyToPay.table.paymentDueBy.heading"))),
          HeadCell(content = Text(messages("submissions.readyToPay.table.status.heading"))),
          HeadCell(content = Text(messages("submissions.readyToPay.table.action.heading")))
        )
      ),
      rows = submissions.sortBy(_.sortDate)(Ordering[LocalDate].reverse).take(maxRecords).map { submission =>
        val hiddenText = messages("submissions.action.view.hidden", submission.submissionId)
        Seq(
          TableRow(content = Text(submission.submissionId)),
          TableRow(content = Text(formatDate(submission.paymentDueBy))),
          TableRow(content = HtmlContent(s"""<strong class="govuk-tag govuk-tag--blue">${messages("submissions.status.readyToPay")}</strong>""")),
          TableRow(content = HtmlContent(s"""<a class="govuk-link" href="#">${messages("submissions.action.view")}<span class="govuk-visually-hidden"> $hiddenText</span></a>"""))
        )
      }
    )

  private def labelWithTag(text: String, count: Int, tagClass: String): String =
    s"""$text <strong class="govuk-tag govuk-!-margin-left-1 $tagClass">$count</strong>"""

  private def buildTabs(drafts: Seq[SubmissionSummary], readyToPay: Seq[SubmissionSummary])(implicit messages: Messages): Seq[SubmissionTab] = {

    val recentSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.recentSubmissions.tabId"),
      label = messages("submissions.recentSubmissions.panel.heading"),
      heading = messages("submissions.recentSubmissions.panel.heading"),
      emptyText = messages("submissions.noRecentSubmissions"),
      count = 0,
      table = None
    )

    val draftSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.drafts.tabId"),
      label = labelWithTag(messages("submissions.drafts.panel.heading"), drafts.size, "govuk-tag--grey"),
      heading = messages("submissions.drafts.panel.heading"),
      emptyText = messages("submissions.noDraftSubmissions"),
      count = drafts.size,
      table = Some(draftsTable(drafts))
    )

    val readyToPaySubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.readyToPay.tabId"),
      label = labelWithTag(messages("submissions.readyToPay.panel.heading"), readyToPay.size, "govuk-tag--blue"),
      heading = messages("submissions.readyToPay.panel.heading"),
      emptyText = messages("submissions.noReadyToPaySubmissions"),
      count = readyToPay.size,
      table = Some(readyToPayTable(readyToPay))
    )

    val overdueSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.overdue.tabId"),
      label = labelWithTag(messages("submissions.overdue.panel.heading"), 0, "govuk-tag--red"),
      heading = messages("submissions.overdue.panel.heading"),
      emptyText = messages("submissions.noOverdueSubmissions"),
      count = 0,
      table = None
    )

    Seq(recentSubmissionsTab, draftSubmissionsTab, readyToPaySubmissionsTab, overdueSubmissionsTab)
  }

  def build(
             drafts: Seq[SubmissionSummary] = Seq.empty,
             readyToPay: Seq[SubmissionSummary] = Seq.empty
           )(implicit messages: Messages): SubmissionsViewModel =
    SubmissionsViewModel(
      tabs = buildTabs(drafts, readyToPay)
    )
}