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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes as dashboardRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.CanNotAccessPageView

class CannotAccessPageViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[CanNotAccessPageView]

  def view(): Document =
    Jsoup.parse(
      viewInstance()(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("cannotAccessPage.title")
    val heading: String = messages("cannotAccessPage.heading")
    val p1: String = messages("cannotAccessPage.p1")
    val returnToDashboard: String = messages("return-to-dashboard.link")
  }

  "The CannotAccessPageView" - {

    "render view" - {

      val cannotAccessPageView = view()

      "have the correct title" in {
        cannotAccessPageView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        cannotAccessPageView.select("h1").text() must include(ExpectedContent.heading)
      }

      "display the correct content" in {
        cannotAccessPageView.para(1) mustBe Some(ExpectedContent.p1)
      }

      "have a return to dashboard link" in {
        val link = cannotAccessPageView.select("a.govuk-link").get(3)
        
        link.text() mustBe ExpectedContent.returnToDashboard
        link.attr("href") mustBe dashboardRoutes.DashboardController.onPageLoad().url
      }
    }
  }
}
