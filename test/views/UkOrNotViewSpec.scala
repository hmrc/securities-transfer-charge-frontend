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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.UkOrNotFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.UkOrNotView
import views.ViewBaseSpec

import scala.language.postfixOps

class UkOrNotViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[UkOrNotView]
  private val formProvider = new UkOrNotFormProvider()
  private val form = formProvider()


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "Does the seller live in the UK?"
    val caption = "Seller details"
    val heading = "Does the seller live in the UK?"
    val continue = "Continue"

  }

  "The HowToNotifyAboutSecuritiesTransferView" - {
    "when the user is an Individual, should:" - {
      val ukOrNotView = view()

      "have the correct title" in {
        ukOrNotView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        ukOrNotView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        ukOrNotView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have a create button with the correct text" in {
        val button = ukOrNotView.select(".govuk-button")
        button.text() mustBe ExpectedContent.continue
      }
    }
  }

}
