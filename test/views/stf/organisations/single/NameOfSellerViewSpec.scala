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

package views.stf.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.single.NameOfSellerView
import views.ViewBaseSpec

import scala.language.postfixOps

class NameOfSellerViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[NameOfSellerView]
  private val formProvider = new NameOfSellerFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "What’s the seller’s name?"
    val heading = "What’s the seller’s name?"
    val caption = "Seller details"
    val saveAndContinue = "Save and continue"
    val saveAndReturn = "Save and return to dashboard"

  }

  "The NameOfSellerView" - {
    "when the user is an Individual, should:" - {
      val nameOfSellerView = view()

      "have the correct title" in {
        nameOfSellerView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        nameOfSellerView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        nameOfSellerView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have a button with the text save and continue " in {
        val buttons = nameOfSellerView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = nameOfSellerView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }

}
