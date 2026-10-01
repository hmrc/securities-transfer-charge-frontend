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
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Draft, Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{SubmissionStatus, SubmissionSummary}

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

  private def statusTagClass(status: SubmissionStatus): String = status match {
    case SubmissionStatus.ReadyToPay        => "govuk-tag--blue"
    case SubmissionStatus.Draft             => "govuk-tag--grey"
    case SubmissionStatus.Overdue           => "govuk-tag--orange"
    case _                                  => "govuk-tag--white"
  }

  private def statusTag(status: SubmissionStatus): HtmlContent = {
    HtmlContent(s"""<strong class="govuk-tag ${statusTagClass(status)}">${status.toString}</strong>""")
  }

  private def viewLink(submissionId: String)(implicit messages: Messages): HtmlContent = {
    val view = messages("submissions.action.view")
    val hiddenText = messages("submissions.action.view.hidden", submissionId)

    HtmlContent(
      s"""<a class="govuk-link" href="#">$view<span class="govuk-visually-hidden">$hiddenText</span></a>"""
    )
  }

  private def submissionsTable(summaries: Seq[SubmissionSummary], isDraftsTab: Boolean = false)(implicit messages: Messages): Table = {

    val paymentDueByHead: Seq[HeadCell] =
      if (isDraftsTab)
        Seq.empty
      else  Seq(HeadCell(content = Text(messages("submissions.table.paymentDueBy.heading")), classes = "govuk-!-text-align-right"))

    Table(
      head = Some(
        Seq(HeadCell(content = Text(messages("submissions.table.id.heading")))) ++
          paymentDueByHead ++
          Seq(
            HeadCell(content = Text(messages("submissions.table.status.heading"))),
            HeadCell(content = Text(messages("submissions.table.action.heading")))
          )
      ),
      rows = summaries.take(maxRecords).map { summary =>
        val paymentDueByCell: Seq[TableRow] =
          if (isDraftsTab) Seq.empty else Seq(TableRow(content = Text(formatDate(summary.paymentDueBy)), classes = "govuk-!-text-align-right"))

        Seq(TableRow(content = Text(summary.submissionId))) ++
          paymentDueByCell ++
          Seq(
            TableRow(content = statusTag(summary.status)),
            TableRow(content = viewLink(summary.submissionId))
          )
      }
    )
  }

  private def labelWithTag(text: String, count: Int, status: SubmissionStatus): String =
    s"""$text <strong class="govuk-tag govuk-!-margin-left-1 ${statusTagClass(status)}">$count</strong>"""

  private def buildTabs(
                         drafts: Seq[SubmissionSummary],
                         readyToPay: Seq[SubmissionSummary],
                         overdue: Seq[SubmissionSummary]
                       )(implicit messages: Messages): Seq[SubmissionTab] = {

    val recentSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.recentSubmissions.tabId"),
      label = messages("submissions.recentSubmissions.panel.heading"),
      heading = messages("submissions.recentSubmissions.panel.heading"),
      emptyText = messages("submissions.noRecentSubmissions"),
      count = 0, // TODO: update when building recent submissions tab
      table = None
    )

    val draftSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.drafts.tabId"),
      label = labelWithTag(messages("submissions.drafts.panel.heading"), drafts.size, Draft),
      heading = messages("submissions.drafts.panel.heading"),
      emptyText = messages("submissions.noDraftSubmissions"),
      count = drafts.size,
      table = Some(submissionsTable(drafts, true))
    )

    val readyToPaySubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.readyToPay.tabId"),
      label = labelWithTag(messages("submissions.readyToPay.panel.heading"), readyToPay.size, ReadyToPay),
      heading = messages("submissions.readyToPay.panel.heading"),
      emptyText = messages("submissions.noReadyToPaySubmissions"),
      count = readyToPay.size,
      table = Some(submissionsTable(readyToPay))
    )

    val overdueSubmissionsTab: SubmissionTab = SubmissionTab(
      id = messages("submissions.overdue.tabId"),
      label = labelWithTag(messages("submissions.overdue.panel.heading"), overdue.size, Overdue),
      heading = messages("submissions.overdue.panel.heading"),
      emptyText = messages("submissions.noOverdueSubmissions"),
      count = overdue.size,
      table = Some(submissionsTable(overdue))
    )

    Seq(recentSubmissionsTab, draftSubmissionsTab, readyToPaySubmissionsTab, overdueSubmissionsTab)
  }

  def build(
             drafts: Seq[SubmissionSummary] = Seq.empty,
             readyToPay: Seq[SubmissionSummary] = Seq.empty,
             overdue: Seq[SubmissionSummary] = Seq.empty
           )(implicit messages: Messages): SubmissionsViewModel =
    SubmissionsViewModel(
      tabs = buildTabs(drafts, readyToPay, overdue)
    )
}