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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.AmountPaidForSecuritiesFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.AmountPaidForSecuritiesView
import views.ViewBaseSpec

import scala.language.postfixOps

class AmountPaidForSecuritiesViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[AmountPaidForSecuritiesView]
  private val formProvider = new AmountPaidForSecuritiesFormProvider()
  private val form = formProvider()


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("amountPaidForSecurities.title")
    val heading: String = messages("amountPaidForSecurities.heading")
    val caption: String = messages("amountPaidForSecurities.caption")
    val p1: String = messages("amountPaidForSecurities.p1")
    val p2: String = messages("amountPaidForSecurities.p2")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The AmountPaidForSecuritiesView" - {
    "when the user is an Individual, should:" - {
      val amountPaidForSecuritiesView = view()

      "have the correct title" in {
        amountPaidForSecuritiesView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        amountPaidForSecuritiesView.select("h1").text() must include(ExpectedContent.heading)
      }

      "display the correct caption text" in {
        amountPaidForSecuritiesView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "display the correct content" in {
        amountPaidForSecuritiesView.para(1).value mustBe ExpectedContent.p1
        amountPaidForSecuritiesView.para(2).value mustBe ExpectedContent.p2
      }

      "have a button with the text save and continue " in {
        val buttons = amountPaidForSecuritiesView.select(".govuk-button")
        buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
      }

      "have a button with the text save and return to dashboard" in {
        val buttons = amountPaidForSecuritiesView.select(".govuk-button")
        buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }

}
