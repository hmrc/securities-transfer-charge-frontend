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

package views.dashboard

import base.Fixtures.{drafts, overdue, readyToPay, testSubmissionId}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.pagination.PaginationModel
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.{SubmissionTabIds, SubmissionsViewModel}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionsView
import views.ViewBaseSpec

import java.time.LocalDate
import scala.jdk.CollectionConverters.*

class SubmissionsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[SubmissionsView]


  def view(submissions: SubmissionsViewModel): Document = Jsoup.parse(
    viewInstance(submissions)(fakeRequest, messages).body
  )

  // Helper method to build SubmissionsViewModel with sensible defaults
  def buildViewModel(
                      drafts: Seq[SubmissionSummary] = Seq.empty,
                      readyToPay: Seq[SubmissionSummary] = Seq.empty,
                      overdue: Seq[SubmissionSummary] = Seq.empty,
                      recent: Seq[SubmissionSummary] = Seq.empty
                    ): SubmissionsViewModel = {
    SubmissionsViewModel.build(
      allDrafts = drafts,
      allReadyToPay = readyToPay,
      allOverdue = overdue,
      allRecent = recent,
      paginationModel = PaginationModel.forSubmissions(
        totalDrafts = drafts.size,
        totalReadyToPay = readyToPay.size,
        totalOverdue = overdue.size,
        totalRecent = recent.size
      )
    )
  }


  object ExpectedContent {
    val title: String = messages("submissions.title")
    val caption: String = messages("submissions.caption")

    val dashboardBreadcrumb: String = messages("breadcrumbs.dashboard")
    val submissionsBreadcrumb: String = messages("breadcrumbs.submissions")

    val recentHeading: String = messages("submissions.recentSubmissions.panel.heading")
    val draftsHeading: String = messages("submissions.drafts.panel.heading")
    val readyToPayHeading: String = messages("submissions.readyToPay.panel.heading")
    val overdueHeading: String = messages("submissions.overdue.panel.heading")

    val recentTabId: String = SubmissionTabIds.Recent
    val draftsTabId: String = SubmissionTabIds.Drafts
    val readyToPayTabId: String = SubmissionTabIds.ReadyToPay
    val overdueTabId: String = SubmissionTabIds.Overdue

    val noRecent: String = messages("submissions.noRecentSubmissions")
    val noDrafts: String = messages("submissions.noDraftSubmissions")
    val noReadyToPay: String = messages("submissions.noReadyToPaySubmissions")
    val noOverdue: String = messages("submissions.noOverdueSubmissions")
  }


  "The SubmissionsView" - {

    "when rendered with no submissions " - {

      val doc = view(buildViewModel())

      val tabs = doc.select("a.govuk-tabs__tab")
      val panels = doc.select(".govuk-tabs__panel")
      val breadcrumbs = doc.select(".govuk-breadcrumbs__list-item")

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-heading-l").text() mustBe ExpectedContent.caption
      }

      "have two breadcrumb items" in {
        breadcrumbs.size() mustBe 2
      }

      "have the dashboard breadcrumb as a link" in {
        val link = breadcrumbs.get(0).select("a.govuk-breadcrumbs__link")
        link.text() mustBe ExpectedContent.dashboardBreadcrumb
        link.attr("href") mustBe routes.DashboardController.onPageLoad().url
      }

      "have the tabs component" in {
        doc.select("[data-module=govuk-tabs]").size() mustBe 1
      }

      "have four tabs and four panels" in {
        tabs.size() mustBe 4
        panels.size() mustBe 4
      }

      "have the tabs in the correct order" in {
        tabs.asScala.map(_.text()).toList mustBe List(
          ExpectedContent.recentHeading,
          s"${ExpectedContent.draftsHeading} 0",
          s"${ExpectedContent.readyToPayHeading} 0",
          s"${ExpectedContent.overdueHeading} 0"
        )
      }

      "show the recent submissions tab without a tag" in {
        tabs.get(0).select(".govuk-tag").size() mustBe 0
      }

      "show a grey tag on the drafts tab" in {
        val tag = tabs.get(1).select(".govuk-tag")
        tag.text() mustBe "0"
        tag.hasClass("govuk-tag--grey") mustBe true
      }

      "show a blue tag on the ready to pay tab" in {
        val tag = tabs.get(2).select(".govuk-tag")
        tag.text() mustBe "0"
        tag.hasClass("govuk-tag--blue") mustBe true
      }

      "show an orange tag on the overdue tab" in {
        val tag = tabs.get(3).select(".govuk-tag")
        tag.text() mustBe "0"
        tag.hasClass("govuk-tag--orange") mustBe true
      }

      "select only the first tab" in {
        val items = doc.select("li.govuk-tabs__list-item")
        items.get(0).hasClass("govuk-tabs__list-item--selected") mustBe true
        items.asScala.drop(1).foreach(_.hasClass("govuk-tabs__list-item--selected") mustBe false)
      }

      "show only the first panel" in {
        panels.get(0).hasClass("govuk-tabs__panel--hidden") mustBe false
        panels.asScala.drop(1).foreach(_.hasClass("govuk-tabs__panel--hidden") mustBe true)
      }

      "link each tab to a panel with a matching id" in {
        val expectedIds = List(
          ExpectedContent.recentTabId,
          ExpectedContent.draftsTabId,
          ExpectedContent.readyToPayTabId,
          ExpectedContent.overdueTabId
        )

        tabs.asScala.map(_.attr("href")).toList mustBe expectedIds.map("#" + _)
        panels.asScala.map(_.id()).toList mustBe expectedIds
      }

      "have the correct heading in each panel" in {
        panels.asScala.map(_.select("h2").text()).toList mustBe List(
          ExpectedContent.recentHeading,
          ExpectedContent.draftsHeading,
          ExpectedContent.readyToPayHeading,
          ExpectedContent.overdueHeading
        )
      }

      "show the empty message in each panel" in {
        panels.asScala.map(_.select("p.govuk-body").text()).toList mustBe List(
          ExpectedContent.noRecent,
          ExpectedContent.noDrafts,
          ExpectedContent.noReadyToPay,
          ExpectedContent.noOverdue
        )
      }
    }

    "when rendered with drafts" - {

      val doc = view(buildViewModel(drafts = drafts))
      val panels = doc.select(".govuk-tabs__panel")

      "select only the drafts tab" in { // TODO: Need to rethink this test as currently it's actually selecting the recent submissions tab which is the default, unless accessing via the dashboard where the redirect links have the '#<tab name>' at the end
        val items = doc.select("li.govuk-tabs__list-item")

        items.get(0).hasClass("govuk-tabs__list-item--selected") mustBe true
        items.asScala
          .drop(1)
          .foreach(_.hasClass("govuk-tabs__list-item--selected") mustBe false)
      }

      "show only the drafts panel" in { // TODO: Need to rethink this test as currently it's actually displaying the recent submissions panel which is the default, unless accessing via the dashboard where the redirect links have the '#<tab name>' at the end
        panels.get(0).hasClass("govuk-tabs__panel--hidden") mustBe false
        panels.asScala
          .drop(1)
          .foreach(_.hasClass("govuk-tabs__panel--hidden") mustBe true)
      }

      "render the drafts table with accessible hidden text" in {
        val draftsPanel = doc.select("#drafts")
        val table = draftsPanel.select("table.govuk-table")

        table.size() mustBe 1

        val hiddenText = table.select("a.govuk-link .govuk-visually-hidden")

        hiddenText.size() mustBe 1
        hiddenText.text() mustBe messages("submissions.action.view.hidden", testSubmissionId)
      }

      "show the empty message for the other panels" in {

        panels.asScala
          .map(_.select("p.govuk-body").text())
          .filter(_.nonEmpty)
          .toList mustBe List(
          ExpectedContent.noRecent,
          ExpectedContent.noReadyToPay,
          ExpectedContent.noOverdue
        )
      }
    }

    "when rendered with ready to pay submissions" - {

      val readyToPaySubmissions: Seq[SubmissionSummary] = (10 to 1 by -1).map { i =>
        SubmissionSummary(
          submissionId = f"STC-000000$i%02d",
          maybePaymentDueBy = Some(java.time.LocalDate.of(2026, 10, i).toString),
          maybeAgentReference = None,
          status = ReadyToPay,
          sortDate = java.time.LocalDate.of(2026, 9, i)
        )
      }

      val doc = view(buildViewModel(readyToPay = readyToPaySubmissions))
      val tabs = doc.select("a.govuk-tabs__tab")
      val readyToPayPanel = doc.select(s"#${ExpectedContent.readyToPayTabId}")
      val tableRows = readyToPayPanel.select("tbody .govuk-table__row")

      "display the total number of ready to pay submissions in the blue tab tag" in {
        val tag = tabs.get(2).select(".govuk-tag")
        tag.text() mustBe "10"
        tag.hasClass("govuk-tag--blue") mustBe true
      }

      "not display the empty message in the ready to pay panel" in {
        readyToPayPanel.select("p.govuk-body").size() mustBe 0
      }

      "render the ready to pay table with the correct column headers" in {
        val headers = readyToPayPanel.select("thead .govuk-table__header").asScala.map(_.text()).toList
        headers mustBe List(
          messages("submissions.table.id.heading"),
          messages("submissions.table.paymentDueBy.heading"),
          messages("submissions.table.status.heading"),
          messages("submissions.table.action.heading")
        )
      }

      "display a maximum of 10 rows ordered by most recent submission date descending" in {
        tableRows.size() mustBe 10

        val displayedIds = tableRows.asScala.map(_.select("td").get(0).text()).toList
        displayedIds mustBe (10 to 1 by -1).map(i => f"STC-000000$i%02d").toList
      }

      "display the formatted payment due date, blue status tag, and View link with visually hidden submission ID" in {
        val firstRowCells = tableRows.get(0).select("td")

        firstRowCells.get(0).text() mustBe "STC-00000010"
        firstRowCells.get(1).text() mustBe "10 October 2026"

        val statusTag = firstRowCells.get(2).select(".govuk-tag")
        statusTag.text() mustBe messages("submissions.status.readyToPay")
        statusTag.hasClass("govuk-tag--blue") mustBe true

        val viewLink = firstRowCells.get(3).select("a.govuk-link")
        viewLink.attr("href") mustBe routes.SubmissionDetailsController.onPageLoad("STC-00000010").url
        viewLink.text() mustBe s"${messages("submissions.action.view")}${messages("submissions.action.view.hidden", "STC-00000010")}"
        viewLink.select(".govuk-visually-hidden").text() mustBe messages("submissions.action.view.hidden", "STC-00000010")
      }

      "show the empty message for the other panels" in {
        val panels = doc.select(".govuk-tabs__panel")

        panels.asScala
          .map(_.select("p.govuk-body").text())
          .filter(_.nonEmpty)
          .toList mustBe List(
          ExpectedContent.noRecent,
          ExpectedContent.noDrafts,
          ExpectedContent.noOverdue
        )
      }
    }
  }

  "when rendered with overdue" - {

    val doc = view(buildViewModel(overdue = overdue))
    val panels = doc.select(".govuk-tabs__panel")
    val tabs = doc.select("a.govuk-tabs__tab")
    val overduePanel = doc.select(s"#${ExpectedContent.overdueTabId}")


    "display the total number of overdue submissions in the orange tab tag" in {
      val tag = tabs.get(3).select(".govuk-tag")
      tag.text() mustBe "1"
      tag.hasClass("govuk-tag--orange") mustBe true
    }

    "not display the empty message in the overdue panel" in {
      overduePanel.select("p.govuk-body").size() mustBe 0
    }

    "show only the overdue panel" in {
      panels.get(0).hasClass("govuk-tabs__panel--hidden") mustBe false
      panels.asScala
        .drop(1)
        .foreach(_.hasClass("govuk-tabs__panel--hidden") mustBe true)
    }

    "render the drafts table with accessible hidden text" in {
      val overduePanel = doc.select("#overdue")
      val table = overduePanel.select("table.govuk-table")

      table.size() mustBe 1

      val hiddenText = table.select("a.govuk-link .govuk-visually-hidden")

      hiddenText.size() mustBe 1
      hiddenText.text() mustBe messages("submissions.action.view.hidden", testSubmissionId)
    }

    "show the empty message for the other panels" in {

      panels.asScala
        .map(_.select("p.govuk-body").text())
        .filter(_.nonEmpty)
        .toList mustBe List(
        ExpectedContent.noRecent,
        ExpectedContent.noDrafts,
        ExpectedContent.noReadyToPay
      )
    }
  }

  "when rendered with recent submissions" - {

    val recent = readyToPay ++ drafts ++ overdue
    val doc = view(buildViewModel(recent = recent))

    val recentPanel = doc.select("#recent-submissions")
    val table = recentPanel.select("table.govuk-table")

    "display the correct tags" in {
      val tabs = doc.select("a.govuk-tabs__tab")
      val tags = tabs.select(".govuk-tag")

      tags.get(0).hasClass("govuk-tag--grey") mustBe true
      tags.get(1).hasClass("govuk-tag--blue") mustBe true
      tags.get(2).hasClass("govuk-tag--orange") mustBe true
    }

    "render the recent submissions table" in {
      table.size() mustBe 1
    }

    "have the correct body text" in {
      doc.select(".govuk-body").get(0).text() mustBe messages("submissions.recentSubmissions.body")
    }

    "render the correct table headings" in {
      val headings = table.select("thead th").eachText().asScala

      headings must contain theSameElementsInOrderAs Seq(
        messages("submissions.table.id.heading"),
        messages("submissions.table.paymentDueBy.heading"),
        messages("submissions.table.status.heading"),
        messages("submissions.table.action.heading")
      )
    }

    "render a row for each recent submission" in {
      val rows = table.select("tbody tr")

      rows.size() mustBe recent.size
    }

    "display each submission ID" in {
      val submissionIds = table
        .select("tbody tr td:first-child")
        .eachText()
        .asScala

      submissionIds must contain theSameElementsInOrderAs recent.map(_.submissionId)
    }

    "render the View links with accessible hidden text" in {
      val links = table.select("a.govuk-link")

      links.size() mustBe recent.size

      recent.zipWithIndex.foreach { case (submission, index) =>
        val link = links.get(index)

        link.select(".govuk-visually-hidden").text() mustBe
          s"${messages("submissions.action.view.hidden", submission.submissionId)}"
      }
    }
  }


  "when rendered for an agent" - {

    val recent = readyToPay ++ overdue

    val doc = view(SubmissionsViewModel.build(
      allDrafts = Seq.empty,
      allReadyToPay = readyToPay,
      allOverdue = overdue,
      allRecent = recent,
      isAgent = true,
      paginationModel = PaginationModel.forSubmissions(totalDrafts = 0, totalReadyToPay = readyToPay.size, totalOverdue = overdue.size, totalRecent = recent.size)
    ))

    val submissions = Seq(
      (ExpectedContent.readyToPayTabId, readyToPay),
      (ExpectedContent.overdueTabId, overdue),
      (ExpectedContent.recentTabId, recent)
    )

    submissions.foreach { case (tabId, expectedSubmissions) =>

      s"render the correct column headings for $tabId" in {
        val headings = doc
          .select(s"#$tabId thead .govuk-table__header")
          .asScala
          .map(_.text())
          .toList

        headings mustBe List(
          messages("submissions.table.id.heading"),
          messages("submissions.table.yourReference.heading"),
          messages("submissions.table.paymentDueBy.heading"),
          messages("submissions.table.status.heading"),
          messages("submissions.table.action.heading")
        )
      }

      s"display the agent reference for each submission in $tabId" in {
        val rows = doc.select(s"#$tabId tbody .govuk-table__row")

        rows.size() mustBe expectedSubmissions.size

        rows.asScala.zip(expectedSubmissions).foreach { case (row, submission) =>
          val cells = row.select("td")

          cells.get(0).text() mustBe submission.submissionId
          cells.get(1).text() mustBe submission.agentReference
        }
      }
    }
  }


  "when rendered with drafts for an agent without an agent reference" - {

    val drafts: Seq[SubmissionSummary] = List(
      SubmissionSummary(
        submissionId = testSubmissionId.toString,
        maybePaymentDueBy = None,
        maybeAgentReference = None,
        status = Draft,
        sortDate = LocalDate.parse("2026-10-19")
      )
    )

    val doc = view(SubmissionsViewModel.build(
      allDrafts = drafts,
      allReadyToPay = Seq.empty,
      allOverdue = Seq.empty,
      allRecent = Seq.empty,
      isAgent = true,
      paginationModel = PaginationModel.forSubmissions(totalDrafts = drafts.size, totalReadyToPay = 0, totalOverdue = 0, totalRecent = 0)
    ))

    val draftsPanel = doc.select(s"#${ExpectedContent.draftsTabId}")

    "display the Your reference column heading" in {
      val headings = draftsPanel
        .select("thead .govuk-table__header")
        .asScala
        .map(_.text())
        .toList

      headings mustBe List(
        messages("submissions.table.id.heading"),
        messages("submissions.table.yourReference.heading"),
        messages("submissions.table.status.heading"),
        messages("submissions.table.action.heading")
      )
    }

    "display Not provided as the agent reference for each draft" in {
      val rows = draftsPanel.select("tbody .govuk-table__row")

      rows.size() mustBe drafts.size

      rows.asScala.zip(drafts).foreach { case (row, submission) =>
        val cells = row.select("td")

        cells.get(0).text() mustBe submission.submissionId
        cells.get(1).text() mustBe "Not provided"
      }
    }
  }

  "when rendered with drafts for an agent with an agent reference" - {
    val reference = "Reference-1234"

    val drafts: Seq[SubmissionSummary] = List(
      SubmissionSummary(
        submissionId = testSubmissionId.toString,
        maybePaymentDueBy = None,
        maybeAgentReference = Some(reference),
        status = Draft,
        sortDate = LocalDate.parse("2026-10-19")
      )
    )

    val doc = view(SubmissionsViewModel.build(
      allDrafts = drafts,
      allReadyToPay = Seq.empty,
      allOverdue = Seq.empty,
      allRecent = Seq.empty,
      isAgent = true,
      paginationModel = PaginationModel.forSubmissions(totalDrafts = drafts.size, totalReadyToPay = 0, totalOverdue = 0, totalRecent = 0)
    ))

    val draftsPanel = doc.select(s"#${ExpectedContent.draftsTabId}")

    "display the Your reference column heading" in {
      val headings = draftsPanel
        .select("thead .govuk-table__header")
        .asScala
        .map(_.text())
        .toList

      headings mustBe List(
        messages("submissions.table.id.heading"),
        messages("submissions.table.yourReference.heading"),
        messages("submissions.table.status.heading"),
        messages("submissions.table.action.heading")
      )
    }

    "display Not provided as the agent reference for each draft" in {
      val rows = draftsPanel.select("tbody .govuk-table__row")

      rows.size() mustBe drafts.size

      rows.asScala.zip(drafts).foreach { case (row, submission) =>
        val cells = row.select("td")

        cells.get(0).text() mustBe submission.submissionId
        cells.get(1).text() mustBe reference
      }
    }
  }
}
