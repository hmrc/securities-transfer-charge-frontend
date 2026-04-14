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

package views.stf.individuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.HowToNotifyAboutSecuritiesTransferView
import views.ViewBaseSpec

import scala.language.postfixOps

class HowToNotifyAboutSecuritiesTransferViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]
  private val formProvider = new HowToNotifyAboutSecuritiesTransferFormProvider()
  private val form = formProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, affinityGroupKeyInd, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "How do you want to tell us about your securities transfers?"
    val caption = "Transfer details"
    val heading = "How do you want to tell us about your securities transfers?"
    val continue = "Continue"
    val returnLink = "Return to dashboard"
  }

  "The HowToNotifyAboutSecuritiesTransferView" - {
    "when the user is an Individual, should:" - {
      val howToNotifyAboutSecuritiesTransferView = view()

      "have the correct title" in {
        howToNotifyAboutSecuritiesTransferView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        howToNotifyAboutSecuritiesTransferView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        howToNotifyAboutSecuritiesTransferView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have a continue button" in {
        val button = howToNotifyAboutSecuritiesTransferView.select(".govuk-button").first()
        button.text() mustBe ExpectedContent.continue
      }

      "have a link to return back to the submission dashboard page" in {
        val returnLink = howToNotifyAboutSecuritiesTransferView.select(".govuk-button-group a.govuk-link").first()
        returnLink.text() mustBe ExpectedContent.returnLink
        returnLink.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }
    }
  }

}
