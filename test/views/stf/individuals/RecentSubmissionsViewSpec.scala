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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as stfRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.RecentSubmissionsView
import views.ViewBaseSpec

class RecentSubmissionsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[RecentSubmissionsView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("recentSubmissions.title")
    val heading: String = messages("recentSubmissions.heading")
    val h2: String = messages("recentSubmissions.no-recent.h2")
    val p: String = messages("recentSubmissions.no-recent.p")
    val buttonText: String = messages("recentSubmissions.button.stf")
    val breadcrumbDashboard: String = messages("breadcrumbs.dashboard")
    val breadcrumbSubmissions: String = messages("breadcrumbs.submissions")
  }

  "The RecentSubmissionsView" - {

    val doc = view()

    "have the correct title" in {
      doc.title() must include(ExpectedContent.title)
    }

    "have the correct heading" in {
      doc.select("h1.govuk-heading-xl").text() mustBe ExpectedContent.heading
    }

    "have the correct sub-heading (h2)" in {
      doc.select("h2.govuk-heading-m").text() mustBe ExpectedContent.h2
    }

    "have the correct body text" in {
      doc.select("p.govuk-body").text() mustBe ExpectedContent.p
    }

    "have a button to create a new submission" - {

      val button = doc.select(".govuk-button")

      "with the correct text" in {
        button.text() mustBe ExpectedContent.buttonText
      }

      "with the correct href" in {
        button.attr("href") mustBe stfRoutes.HowToNotifyAboutSecuritiesTransferController.onPageLoad().url
      }
    }

    "have the correct breadcrumb navigation" - {

      val breadcrumbs = doc.select(".govuk-breadcrumbs__list-item")

      "showing the Dashboard link" in {
        val dashboardCrumb = breadcrumbs.get(0)
        dashboardCrumb.text() mustBe ExpectedContent.breadcrumbDashboard
        dashboardCrumb.select("a").attr("href") mustBe stfRoutes.DashboardController.onPageLoad().url
      }

      "showing the current Submissions page text" in {
        val submissionsCrumb = breadcrumbs.get(1)
        submissionsCrumb.text() mustBe ExpectedContent.breadcrumbSubmissions
      }
    }
  }
}