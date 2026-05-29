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

package views.stf.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.ConnectedPersonsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.single.ConnectedPersonsView
import views.ViewBaseSpec

class ConnectedPersonsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = orgAffinity).build()

  private val viewInstance = app.injector.instanceOf[ConnectedPersonsView]
  private val formProvider = new ConnectedPersonsFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode,testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("connectedPersons.title")
    val caption: String = messages("seller.details.caption")
    val heading: String = messages("connectedPersons.heading")
    val paragraph: String = messages("org.connectedPersons.p")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
    val legend: String = messages("org.connectedPersons.legend")
    val linkText: String = messages("org.connectedPersons.p.link.content")
  }

  "The ConnectedPersonsView" - {

    "when rendered without errors" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        doc.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the correct legend" in {
        doc.select(".govuk-fieldset").text() must include(ExpectedContent.legend)
      }

      "have the correct paragraph and link text" in {
        doc.select(".govuk-body").text() must include(ExpectedContent.paragraph)
        doc.select(".govuk-body").text() must include(ExpectedContent.linkText)
      }

      "have a save and continue button" in {
        doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
