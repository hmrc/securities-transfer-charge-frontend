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

package views.stf.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.ConnectedPersonsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.single.ConnectedPersonsView
import views.ViewBaseSpec

class ConnectedPersonsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[ConnectedPersonsView]
  private val formProvider = new ConnectedPersonsFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agent.connectedPersons.title")
    val caption: String = messages("transfer.details.caption")
    val heading: String = messages("agent.connectedPersons.heading")
    val para: String = messages("agent.connectedPersons.p")
    val legend: String = messages("agent.connectedPersons.legend")
    val paraLinkText: String = messages("agent.connectedPersons.content.link.text")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The ConnectedPersonsView" - {

    "when rendered without errors" - {

      val connectedPersonsView = view()

      "have the correct title" in {
        connectedPersonsView.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        connectedPersonsView.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        connectedPersonsView.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the correct text with a link" in {
        connectedPersonsView.select(".govuk-body").text() mustBe ExpectedContent.para + " " + ExpectedContent.paraLinkText + " (opens in new tab)."
        connectedPersonsView.select("p.govuk-body").select("a.govuk-link").attr("href") mustBe "https://www.gov.uk/hmrc-internal-manuals/vat-insurance/vatins9000"
      }

      "have the correct legend" in {
        connectedPersonsView.select(".govuk-fieldset").text() must include(ExpectedContent.legend)
      }

      "have a save and continue button" in {
        connectedPersonsView.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        connectedPersonsView.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
