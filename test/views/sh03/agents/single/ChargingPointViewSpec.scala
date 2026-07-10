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

package views.sh03.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.ChargingPointFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.ChargingPointView
import views.ViewBaseSpec

class ChargingPointViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[ChargingPointView]
  private val formProvider = new ChargingPointFormProvider()
  private val form = formProvider(affinityGroupKeyAgent)
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  def view(): Document =
    Jsoup.parse(
      viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("agent.sh03.chargingPoint.title")
    val heading: String = messages("agent.sh03.chargingPoint.heading")
    val heading2: String = messages("agent.sh03.chargingPoint.heading2")
    val p1: String = messages("agent.sh03.chargingPoint.p1")
    val p2: String = messages("agent.sh03.chargingPoint.p2")
    val p3: String = messages("agent.sh03.chargingPoint.p3")
    val bullet1: String = messages("agent.sh03.chargingPoint.bullet1")
    val bullet2: String = messages("agent.sh03.chargingPoint.bullet2")
    val linkText: String = messages("agent.sh03.chargingPoint.link.text")
    val hint: String = messages("agent.sh03.chargingPoint.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The ChargingPointView" - {

    "should" - {

      val chargingPointView = view()

      "have the correct title" in {
        chargingPointView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        chargingPointView.select("h1").text() mustBe ExpectedContent.heading
        chargingPointView.select("h2").first().text() mustBe ExpectedContent.heading2
      }

      "display the first paragraph" in {
        chargingPointView.select("p.govuk-body").get(0).text() mustBe ExpectedContent.p1
      }

      "display the second paragraph" in {
        chargingPointView.select("p.govuk-body").get(1).text() mustBe ExpectedContent.p2
      }

      "display the bullet points" in {
        val bullets = chargingPointView.select("ul.govuk-list--bullet li")

        bullets.get(0).text() mustBe ExpectedContent.bullet1
        bullets.get(1).text() mustBe ExpectedContent.bullet2
      }

      "display the third paragraph" in {
        chargingPointView.select("p.govuk-body").get(2).text() must include(ExpectedContent.p3)
      }

      "display the guidance link" in {
        val link = chargingPointView.select("p.govuk-body a")

        link.text() must include(ExpectedContent.linkText)
        link.attr("href") mustBe routes.JourneyRecoveryController.onPageLoad().url
      }

      "have the correct hint" in {
        chargingPointView.select("#value-hint").text() mustBe ExpectedContent.hint
      }

      "have a button with the text save and continue" in {
        val buttons = chargingPointView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = chargingPointView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}