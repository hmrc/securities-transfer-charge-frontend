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

package views.sh03.organisations

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.routes as sh03Routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.BeforeYouStartView
import views.ViewBaseSpec

class BeforeYouStartViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[BeforeYouStartView]

  def view(): Document =
    Jsoup.parse(
      viewInstance()(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("agent.sh03.beforeYouStart.title")
    val heading: String = messages("agent.sh03.beforeYouStart.heading")
    val p1: String = messages("agent.sh03.beforeYouStart.p1")
    val p2: String = messages("agent.sh03.beforeYouStart.p2")
    val p3: String = messages("agent.sh03.beforeYouStart.p3")
    val p4: String = messages("agent.sh03.beforeYouStart.p4")
    val continue: String = messages("site.continue")
    val returnToDashboard: String = messages("return-to-dashboard.link")
  }

  "The BeforeYouStartView" - {

    "render view" - {

      val beforeYouStartView = view()

      "have the correct title" in {
        beforeYouStartView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        beforeYouStartView.select("h1").text() must include(ExpectedContent.heading)
      }

      "display the correct content" in {
        beforeYouStartView.para(1) mustBe Some(ExpectedContent.p1)
        beforeYouStartView.para(2) mustBe Some(ExpectedContent.p2)
        beforeYouStartView.para(3) mustBe Some(ExpectedContent.p3)
        beforeYouStartView.para(4) mustBe Some(ExpectedContent.p4)
      }

      "have a continue button" in {
        val button = beforeYouStartView.select(".govuk-button").first()
        button.text() mustBe ExpectedContent.continue
      }

      "submits to the correct endpoint" in {
        val form = beforeYouStartView.select("form")
        form.attr("action") mustBe sh03Routes.BeforeYouStartController.onSubmit().url
      }

      "have a return to dashboard link" in {
        val link = beforeYouStartView.select(".govuk-button-group a.govuk-link").first()

        link.text() mustBe ExpectedContent.returnToDashboard
        link.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }
    }
  }
}
