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

package views.stf.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.NameOfBuyerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.single.NameOfBuyerView
import views.ViewBaseSpec

import scala.language.postfixOps

class NameOfBuyerViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[NameOfBuyerView]
  private val formProvider = new NameOfBuyerFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agent.nameOfBuyer.title")
    val heading: String = messages("agent.nameOfBuyer.heading")
    val caption: String = messages("agent.nameOfBuyer.details.caption")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
    val hint: String = messages("agent.nameOfBuyer.hint")

  }

  "The NameOfBuyerView" - {
    "render view" - {
      val nameOfBuyerView = view()

      "have the correct title" in {
        nameOfBuyerView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        nameOfBuyerView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        nameOfBuyerView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "display the correct hint text" in {
        nameOfBuyerView.hintText mustBe Some(ExpectedContent.hint)
      }

      "have a button with the text save and continue " in {
        val buttons = nameOfBuyerView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = nameOfBuyerView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }

}
