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

package views.stf.organisations

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.SecuritiesTargetFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.SecuritiesTargetView
import views.ViewBaseSpec

import scala.language.postfixOps

class SecuritiesTargetViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[SecuritiesTargetView]
  private val formProvider         = new SecuritiesTargetFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("orgs.securitiesTarget.title")
    val heading: String = messages("orgs.securitiesTarget.heading")
    val caption: String = messages("transfer.details.caption")
    val field1: String = messages("orgs.securitiesTarget.businessName")
    val field2: String = messages("orgs.securitiesTarget.crn")
    val crnHintMessage1: String = messages("orgs.securitiesTarget.crn.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The SecuritiesTargetView" - {
      
      val securitiesTargetView = view()

      "have the correct title" in {
        securitiesTargetView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        securitiesTargetView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        securitiesTargetView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "displays the correct field name for the business name" in {
        securitiesTargetView.select(".govuk-label").first().text() mustBe ExpectedContent.field1
      }

      "displays the correct field name for the CRN" in {
        securitiesTargetView.select(".govuk-label").last().text() mustBe ExpectedContent.field2
      }

      "must render CRN hint text" in {
        val hint = securitiesTargetView.select(".govuk-hint").text()
        hint must include(messages("securitiesTarget.crn.hint"))
      }

      "have a save and continue button" in {
        val buttons = securitiesTargetView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        val buttons = securitiesTargetView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
}
