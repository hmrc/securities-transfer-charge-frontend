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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.SecuritiesTargetFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.SecuritiesTargetView
import views.ViewBaseSpec

import scala.language.postfixOps

class SecuritiesTargetViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[SecuritiesTargetView]
  private val formProvider = new SecuritiesTargetFormProvider()
  private val form = formProvider()


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "What business are you buying these securities in?"
    val heading = "What business are you buying these securities in?"
    val caption = "Transfer details"
    val field1 = "Business Name"
    val field2 = "Company registration number (CRN) (optional)"
    val crnHintMessage1 = "This is also called a company number."
    val crnHintMessage2 = "For example, SN898989 or 12345678"
    val saveAndContinue = "Save and continue"
    val saveAndReturn = "Save and return to dashboard"

  }

  "The NameOfSellerView" - {
    "when the user is an Individual, should:" - {
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

      "have the correct heading" in {
        securitiesTargetView.select("govuk-hint").text() mustBe ExpectedContent.crnHintMessage1
      }

      "have a button with the text save and continue " in {
        val buttons = securitiesTargetView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = securitiesTargetView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }

}
