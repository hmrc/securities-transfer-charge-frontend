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

package views.templates

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.auth.routes as authRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.templates.Layout
import views.ViewBaseSpec

class LayoutSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val layout = app.injector.instanceOf[Layout]

  private val dashboardUrl   = dashboardRoutes.DashboardController.onPageLoad().url
  private val submissionsUrl = dashboardRoutes.SubmissionsController.onPageLoad().url

  private val dashboardNavLink = "li.govuk-service-navigation__item a.govuk-service-navigation__link"

  private def view(path: String, showSignOut: Boolean = true): Document =
    Jsoup.parse(
      layout(pageTitle = "Test page", showSignOut = showSignOut)(Html("<p>content</p>"))(FakeRequest("GET", path), messages).body
    )

  "Layout" - {

    "header" - {

      val doc = view(submissionsUrl)

      "have a GOV.UK logo linking to GOV.UK" in {
        doc.select(".govuk-header__homepage-link").attr("href") mustBe "https://www.gov.uk"
      }

      "have a sign out link" in {
        val signOut = doc.select(".hmrc-sign-out-nav__link")
        signOut.text() mustBe "Sign out"
        signOut.attr("href") mustBe authRoutes.AuthController.signOut().url
      }

      "not show a sign out link when showSignOut is false" in {
        view(submissionsUrl, showSignOut = false).select(".hmrc-sign-out-nav__link").size() mustBe 0
      }
    }

    "service navigation" - {

      val doc = view(submissionsUrl)

      "have the service name linking to the homepage" in {
        val serviceName = doc.select(".govuk-service-navigation__service-name a")
        serviceName.text() mustBe messages("service.navigation.name")
        serviceName.attr("href") mustBe dashboardUrl
      }

      "have a Dashboard link to the homepage" in {
        val dashboardLink = doc.select(dashboardNavLink)
        dashboardLink.size() mustBe 1
        dashboardLink.text() mustBe messages("service.navigation.dashboard")
        dashboardLink.attr("href") mustBe dashboardUrl
      }

      "have the Welsh language toggle" in {
        doc.select(".hmrc-service-navigation-language-select").size() mustBe 1
        doc.select(".hmrc-service-navigation-language-select").text() must include("ENG")
        doc.select(".hmrc-service-navigation-language-select").text() must include("CYM")
      }

      "not mark Dashboard as current when not on the dashboard" in {
        doc.select(".govuk-service-navigation__item--active").size() mustBe 0
        doc.select(dashboardNavLink).hasAttr("aria-current") mustBe false
      }

      Seq(dashboardUrl, "/securities-transfer-charge/dashboard").foreach { path =>
        s"mark Dashboard as current when on $path" in {
          val dashboardDoc = view(path)
          dashboardDoc.select(".govuk-service-navigation__item--active").text() mustBe messages("service.navigation.dashboard")
          dashboardDoc.select(dashboardNavLink).attr("aria-current") mustBe "page"
        }
      }
    }
  }
}
