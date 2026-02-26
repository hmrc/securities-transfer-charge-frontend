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

package views

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.TaxRateFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.TaxRateView

class TaxRateViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[TaxRateView]
  private val formProvider = new TaxRateFormProvider()

  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "Tax rate"
    val caption = "Transfer details"
    val heading = "Tax rate"
    val firstParagraph = "For most securities transfers, you will be charged a tax rate of 0.5% of the consideration."
    val secondParagraph = "If the securities are being transferred into a ‘depository receipt scheme’ or a ‘clearance service’, you will need to pay the 1.5% tax rate in most circumstances. This is usually where the transfer is not integral to capital raising or where the clearance service has elected to remain in the 0.5% regime."
    val saveAndContinue = "Save and continue"
    val saveAndReturn = "Save and return to dashboard"
  }

  "The TaxRateView" - {

    "when rendered without errors" - {

      val taxRateView = view()

      "have the correct title" in {
        taxRateView.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        taxRateView.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        taxRateView.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the first paragraph" in {
        taxRateView.select(".govuk-body").first().text() mustBe ExpectedContent.firstParagraph
      }      
      
      "have the last paragraph" in {
        taxRateView.select(".govuk-body").last().text() mustBe ExpectedContent.secondParagraph
      }
      
      "have a save and continue button" in {
        taxRateView.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        taxRateView.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
