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

import base.Fixtures.{singleSubmissionReadyToPay, singleSubmissionReadyToPayWithLateFilingCharge}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{Other, SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionDetailsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionDetailsView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*


class SubmissionDetailsViewSpec extends ViewBaseSpec {

  private val viewInstance = app.injector.instanceOf[SubmissionDetailsView]
  private val formatter = app.injector.instanceOf[FormattingService]

  override def fakeApplication(): Application =
    applicationBuilder().build()

  def view(submission: SttSubmission): Document = Jsoup.parse(
    viewInstance(SubmissionDetailsViewModel.build(submission, formatter))(fakeRequest, messages).body
  )

  class ExpectedContent(submission: SttSubmission) {
    val title: String = messages("submissionDetails.title", submission.submissionId)
    val heading: String = messages("submissionDetails.heading", submission.submissionId)

    val dashboardBreadcrumb: String = messages("breadcrumbs.dashboard")
    val submissionsBreadcrumb: String = messages("breadcrumbs.submissions")

    val print: String = messages("submissionDetails.print")
    val total: String = formatter.formatCurrency(submission.totalToPay)
    val totalHeading: String = messages("submissionDetails.total.heading", total)

    val paymentDueBy: String = messages("submissionDetails.paymentDueBy")
    val dueDate: String = formatter.formatDate(submission.paymentDueByDate)

    val submissionDate: String = formatter.formatDate(submission.submissionDate)
    val submissionDateLabel: String = messages("submissionDetails.submission.date")

    val submissionType: String = submission.submissionType match {
      case STF => messages("submissionDetails.transferType.stf")
      case SH03 => messages("submissionDetails.transferType.sh03")
      case Other => messages("submissionDetails.transferType.other")
    }

    val submissionTypeLabel: String = messages("submissionDetails.submission.type")

    val numberOfTransfers: String = submission.numberOfTransfers.toString
    val transfersLabel: String = messages("submissionDetails.submission.transfers")

    val totalTax: String = formatter.formatCurrency(submission.taxToPay)
    val totalTaxDueLabel: String = messages("submissionDetails.submission.totalTaxDue")

    val payForSubmission: String = messages("submissionDetails.submission.payForSubmission")

    val readyToPayCount: Int = submission.transfers.count(_.status == ReadyToPay)
    val overdueCount: Int = submission.transfers.count(_.status == Overdue)

    private def key(id: String, suffix: String) = messages(s"submissionDetails.$id.$suffix")

    val tabIds: Seq[String] = List("allTransfers", "readyToPay", "overdue")
    val tabLabels: Seq[String] = tabIds.map(key(_, "panel.label"))
    val tabHeadings: Seq[String] = tabIds.map(key(_, "panel.heading"))
    val tabHtmlIds: Seq[String] = tabIds.map(key(_, "tabId"))
    val emptyTexts: Seq[String] = tabIds.map(key(_, "empty"))
    val tabCounts: Seq[Int] = List(submission.transfers.size, readyToPayCount, overdueCount)
  }

  private val submissions: Seq[(String, SttSubmission)] = Seq(
    "a ready to pay STF submission with a single charge" -> singleSubmissionReadyToPay,
    "a ready to pay STF submission with a tax charge AND a late filing charge" -> singleSubmissionReadyToPayWithLateFilingCharge,
    "a ready to pay SH03 submission" -> singleSubmissionReadyToPay.copy(declareeName = Some("James White"))
  )

  submissions.foreach { case (name, submission) =>
    s"The SubmissionDetailsView for $name should" - {
      val expected = ExpectedContent(submission)
      val doc = view(submission)
      val breadcrumbs = doc.select(".govuk-breadcrumbs__list-item")
      val taxCard = doc.select(".tax-card")
      val tabs = doc.select("a.govuk-tabs__tab")
      val panels = doc.select(".govuk-tabs__panel")

      "have the correct title" in {
        doc.title() must include(expected.title)
      }

      "have the correct heading" in {
        doc.select(".govuk-heading-l").text() mustBe expected.heading
      }

      "have two breadcrumb items" in {
        breadcrumbs.size() mustBe 2
      }

      "have the dashboard breadcrumb as a link" in {
        val link = breadcrumbs.get(0).select("a.govuk-breadcrumbs__link")
        link.text() mustBe expected.dashboardBreadcrumb
        link.attr("href") mustBe routes.DashboardController.onPageLoad().url
      }

      "have the submissions breadcrumb as a link" in {
        val link = breadcrumbs.get(1).select("a.govuk-breadcrumbs__link")
        link.text() mustBe expected.submissionsBreadcrumb
        link.attr("href") mustBe routes.SubmissionsController.onPageLoad().url
      }

      "have a print button" in {
        val buttons = doc.select(".govuk-button")
        buttons.get(0).text() mustBe expected.print
      }

      "have the correct total heading in the tax card" in {
        taxCard.select("h2.govuk-heading-m").text() mustBe expected.totalHeading
      }

      "have the correct payment due by text" in {
        taxCard.select("p.govuk-body-s").text() mustBe s"${expected.paymentDueBy} ${expected.dueDate}"
      }

      "have the due date in bold" in {
        taxCard.select("p.govuk-body-s strong").text() mustBe expected.dueDate
      }

      "have a summary list" in {
        doc.select(".govuk-summary-list").size() mustBe 1
      }

      "have the correct submission date in the summary list" in {
        val rows = doc.select(".govuk-summary-list__row")
        rows.get(0).select(".govuk-summary-list__key").text() mustBe
          expected.submissionDateLabel
        rows.get(0).select(".govuk-summary-list__value").text() mustBe expected.submissionDate
      }

      "have the correct submission type in the summary list" in {
        val rows = doc.select(".govuk-summary-list__row")
        rows.get(1).select(".govuk-summary-list__key").text() mustBe
          expected.submissionTypeLabel
        rows.get(1).select(".govuk-summary-list__value").text() mustBe expected.submissionType
      }

      "have the correct number of transfers in the summary list" in {
        val rows = doc.select(".govuk-summary-list__row")
        rows.get(2).select(".govuk-summary-list__key").text() mustBe
          expected.transfersLabel
        rows.get(2).select(".govuk-summary-list__value").text() mustBe expected.numberOfTransfers
      }

      "have the correct total tax due in the summary list" in {
        val rows = doc.select(".govuk-summary-list__row")
        rows.get(3).select(".govuk-summary-list__key").text() mustBe
          expected.totalTaxDueLabel
        rows.get(3).select(".govuk-summary-list__value").text() mustBe expected.totalTax
      }

      "have four rows in the summary list" in {
        doc.select(".govuk-summary-list__row").size() mustBe 4
      }

      "have the no-bottom-border class on the total tax due row" in {
        doc.select(".govuk-summary-list__row.no-bottom-border").size() mustBe 1
      }

      "have a pay for submission button" in {
        val buttons = doc.select(".govuk-button")
        buttons.last().text() mustBe expected.payForSubmission
      }

      "have the pay for submission button linking to the correct destination" in {
        val button = doc.select(".govuk-button").last()
        button.attr("href") mustBe "#"
      }

      "have the correct class on the pay for submission button" in {
        val button = doc.select(".govuk-button").last()
        button.hasClass("govuk-!-margin-bottom-0") mustBe true
      }

      "have the tabs component" in {
        doc.select("[data-module=govuk-tabs]").size() mustBe 1
      }

      "have three tabs and three panels" in {
        tabs.size() mustBe 3
        panels.size() mustBe 3
      }

      "have the tabs in the correct order" in {
        tabs.asScala.map(_.text()).toList mustBe List(
          expected.tabLabels.head,
          s"${expected.tabLabels(1)} ${expected.readyToPayCount}",
          s"${expected.tabLabels(2)} ${expected.overdueCount}"
        )
      }

      "show the all transfers tab without a tag" in {
        tabs.get(0).select(".govuk-tag").size() mustBe 0
      }

      "show a blue tag on the ready to pay tab" in {
        val tag = tabs.get(1).select(".govuk-tag")
        tag.text() mustBe expected.readyToPayCount.toString
        tag.hasClass("govuk-tag--blue") mustBe true
      }

      "show an orange tag on the overdue tab" in {
        val tag = tabs.get(2).select(".govuk-tag")
        tag.text() mustBe expected.overdueCount.toString
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
        tabs.asScala.map(_.attr("href")).toList mustBe expected.tabHtmlIds.map("#" + _)
        panels.asScala.map(_.id()).toList mustBe expected.tabHtmlIds
      }

      "have the correct heading in each panel" in {
        panels.asScala.map(_.select("h1").text()).toList mustBe expected.tabHeadings
      }

      "show a table or the empty message in each panel" in {
        panels.asScala.zip(expected.tabCounts).zip(expected.emptyTexts).foreach {
          case ((panel, count), emptyText) =>
            if (count == 0) {
              panel.select("p.govuk-body").text() mustBe emptyText
              panel.select("table").size() mustBe 0
            } else {
              panel.select("table").size() mustBe 1
              panel.select("p.govuk-body").size() mustBe 0
            }
        }
      }
    }
  }
}