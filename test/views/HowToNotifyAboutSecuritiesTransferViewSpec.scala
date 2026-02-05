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
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.HowToNotifyAboutSecuritiesTransferView
import uk.gov.hmrc.securitiestransferchargefrontend.forms.HowToNotifyAboutSecuritiesTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import views.ViewBaseSpec

class HowToNotifyAboutSecuritiesTransferViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()
  
  private val viewInstance         = app.injector.instanceOf[HowToNotifyAboutSecuritiesTransferView]
  private val formProvider = new HowToNotifyAboutSecuritiesTransferFormProvider()
  private val form = formProvider()


  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "How do you want to tell us about your securities transfer?"
    val captionHint = "Transfer details"
    val heading = "How do you want to tell us about your securities transfer?"
    val continue = "Continue"
    
  }

  "The SubmissionsDashboardView" - {
    "the user is an Individual" - {
      val HowToNotifyAboutSecuritiesTransferView = view()

      "have the correct title" in {
        HowToNotifyAboutSecuritiesTransferView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        HowToNotifyAboutSecuritiesTransferView.select("h1").text() mustBe ExpectedContent.heading
      }
      ???
    }
  }

}
