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

package views.sh03.agents

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.HowToNotifyAboutShareBuybackFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.HowToNotifyAboutShareBuybackView
import views.ViewBaseSpec

import scala.language.postfixOps

class HowToNotifyAboutShareBuybackViewSpec extends ViewBaseSpec {
  
  override def fakeApplication(): Application = applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[HowToNotifyAboutShareBuybackView]
  private val formProvider = new HowToNotifyAboutShareBuybackFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, affinityGroupKeyInd, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agent.sh03.howToNotifyAboutShareBuyback.title")
    val heading: String = messages("agent.sh03.howToNotifyAboutShareBuyback.heading")
    val continue: String = messages("site.continue")
    val returnLink: String = messages("return-to-dashboard.link")
  }

  "The HowToNotifyAboutShareBuybackView" - {
    "when the user is an agent, should:" - {
      val howToNotifyAboutShareBuybackView = view()

      "have the correct title" in {
        howToNotifyAboutShareBuybackView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        howToNotifyAboutShareBuybackView.select("h1").text() mustBe ExpectedContent.heading
      }

      "have a continue button" in {
        val button = howToNotifyAboutShareBuybackView.select(".govuk-button").first()
        button.text() mustBe ExpectedContent.continue
      }

      "have a return to dashboard link" in {
        val link = howToNotifyAboutShareBuybackView.select(".govuk-button-group a.govuk-link").first()
        link.text() mustBe ExpectedContent.returnLink
        link.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }
    }
  }

}
