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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.RecentSubmissionsView
import views.ViewBaseSpec

class RecentSubmissionsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[RecentSubmissionsView]

  def view(isIndividual: Boolean): Document = Jsoup.parse(
    viewInstance(isIndividual)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = s"${messages("recentSubmissions.title")} - ${messages("reportAndPay.title")} - ${messages("site.govuk")}"
    val heading: String = messages("recentSubmissions.heading")
    val h2: String = messages("recentSubmissions.no-recent.h2")
    val p: String = messages("recentSubmissions.no-recent.p")
    val buttonText: String = messages("recentSubmissions.button.stf")
    val breadcrumbDashboard: String = messages("breadcrumbs.dashboard")
    val breadcrumbSubmissions: String = messages("breadcrumbs.submissions")
    val tellUs: String = messages("dashboard.tell-us")
    val stf: String = messages("dashboard.stf")
    val sh03: String = messages("dashboard.sh03")
  }

  "The RecentSubmissionsView" - {

    "when rendering common content" - {

      val doc = view(isIndividual = true)

      "have the correct title" in {
        doc.title() mustBe ExpectedContent.title
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

      "have the correct breadcrumb navigation" - {

        val breadcrumbs = doc.select(".govuk-breadcrumbs__list-item")

        "showing the Dashboard link" in {
          val dashboardCrumb = breadcrumbs.get(0)
          dashboardCrumb.text() mustBe ExpectedContent.breadcrumbDashboard
          dashboardCrumb.select("a").attr("href") mustBe dashboardRoutes.DashboardController.onPageLoad().url
        }

        "showing the current Submissions page text" in {
          val submissionsCrumb = breadcrumbs.get(1)
          submissionsCrumb.text() mustBe ExpectedContent.breadcrumbSubmissions
        }
      }
    }

    "when isIndividual is true" - {

      val doc = view(isIndividual = true)

      "have a button to create a new submission" - {

        val button = doc.select(".govuk-button")

        "with the correct text" in {
          button.text() mustBe ExpectedContent.buttonText
        }

        "with the correct href" in {
          button.attr("href") mustBe dashboardRoutes.StartSubmissionController.startStf().url
        }
      }

      "not display the STF and SH03 links" in {
        doc.select("ul.govuk-list--bullet li").size() mustBe 0
      }
    }

    "when isIndividual is false" - {

      val doc = view(isIndividual = false)

      "display the correct introductory text" in {
        doc.select("p.govuk-body").last().text() mustBe ExpectedContent.tellUs
      }

      "display the STF and SH03 links" - {

        val links = doc.select("ul.govuk-list--bullet li a")

        "display two links" in {
          links.size() mustBe 2
        }

        "display the STF link with the correct text and href" in {
          val stfLink = links.get(0)

          stfLink.text() mustBe ExpectedContent.stf
          stfLink.attr("href") mustBe dashboardRoutes.StartSubmissionController.startStf().url
        }

        "display the SH03 link with the correct text and href" in {
          val sh03Link = links.get(1)

          sh03Link.text() mustBe ExpectedContent.sh03
          sh03Link.attr("href") mustBe dashboardRoutes.StartSubmissionController.startSh03().url
        }
      }

      "not display the create submission button" in {
        doc.select(".govuk-button").size() mustBe 0
      }
    }
  }
}
