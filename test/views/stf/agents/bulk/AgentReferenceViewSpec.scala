/*
 * Copyright 2025 HM Revenue & Customs
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

package views.stf.agents.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.AgentReferenceFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.bulk.AgentReferenceView
import views.ViewBaseSpec

import scala.language.postfixOps

class AgentReferenceViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder(affinityGroup = agentAffinity).build()
  
  private val viewInstance         = app.injector.instanceOf[AgentReferenceView]
  private val formProvider         = new AgentReferenceFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agentReference.title")
    val heading: String = messages("agentReference.heading")
    val caption: String = messages("agent.details.caption")
    val hint: String = messages("agentReference.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The SecuritiesTargetView" - {
    
      val agentReferenceView = view()

      "have the correct title" in {
        agentReferenceView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        agentReferenceView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        agentReferenceView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "display the correct hint text" in {
        agentReferenceView.hintText mustBe Some(ExpectedContent.hint)
      }

      "have a save and continue button" in {
        val buttons = agentReferenceView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        val buttons = agentReferenceView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
  }

}
