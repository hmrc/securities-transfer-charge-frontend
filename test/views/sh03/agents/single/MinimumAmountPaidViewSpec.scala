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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.MinimumAmountPaidFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.MinimumAmountPaidView
import views.ViewBaseSpec

class MinimumAmountPaidViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[MinimumAmountPaidView]
  private val formProvider = new MinimumAmountPaidFormProvider()
  private val form         = formProvider(affinityGroupKeyAgent)

  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  def view(): Document =
    Jsoup.parse(
      viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String           = messages("agent.sh03.minimumAmountPaid.title")
    val heading: String         = messages("agent.sh03.minimumAmountPaid.heading")
    val caption: String         = messages("sh03.section.name")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String   = messages("site.save-and-return.button")
  }

  "The MinimumAmountPaidView" - {

    "render view" - {

      val minimumAmountPaidView = view()

      "have the correct title" in {
        minimumAmountPaidView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        minimumAmountPaidView.select("h1").text() mustBe ExpectedContent.heading
      }
      
      "have a currency input" in {
        minimumAmountPaidView.select("input.govuk-input").size() mustBe 1
      }

      "have a pound prefix on the input" in {
        minimumAmountPaidView.select(".govuk-input__prefix").text() mustBe "£"
      }

      "have a button with the text save and continue" in {
        val buttons = minimumAmountPaidView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = minimumAmountPaidView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
