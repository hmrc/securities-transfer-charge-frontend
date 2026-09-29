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

package views.stf.individuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.SubmissionsView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import views.ViewBaseSpec

import scala.jdk.CollectionConverters._

class SubmissionsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[SubmissionsView]


  def view(submissions: SubmissionsViewModel): Document = Jsoup.parse(
    viewInstance(submissions)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("submissions.title")
    val caption: String = messages("submissions.caption")
    
    val dashboardBreadcrumb: String = messages("breadcrumbs.dashboard")
    val submissionsBreadcrumb: String = messages("breadcrumbs.submissions")

    val recentHeading: String = messages("submissions.recentSubmissions.panel.heading")
    val draftsHeading: String = messages("submissions.drafts.panel.heading")
    val readyToPayHeading: String = messages("submissions.readyToPay.panel.heading")
    val overdueHeading: String = messages("submissions.overdue.panel.heading")

    val recentTabId: String = messages("submissions.recentSubmissions.tabId")
    val draftsTabId: String = messages("submissions.drafts.tabId")
    val readyToPayTabId: String = messages("submissions.readyToPay.tabId")
    val overdueTabId: String = messages("submissions.overdue.tabId")

    val noRecent: String = messages("submissions.noRecentSubmissions")
    val noDrafts: String = messages("submissions.noDraftSubmissions")
    val noReadyToPay: String = messages("submissions.noReadyToPaySubmissions")
    val noOverdue: String = messages("submissions.noOverdueSubmissions")
  }


  "The SubmissionsView" - {

    "when rendered with no submissions " - {
      
      val doc = view(SubmissionsViewModel.empty())
      
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

      "have the submissions breadcrumb as the current page, without a link" in {
        val current = breadcrumbs.get(1)
        current.text() mustBe ExpectedContent.submissionsBreadcrumb
        current.select("a").size() mustBe 0
        current.attr("aria-current") mustBe "page"
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

      "show a red tag on the overdue tab" in {
        val tag = tabs.get(3).select(".govuk-tag")
        tag.text() mustBe "0"
        tag.hasClass("govuk-tag--red") mustBe true
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
  }
}