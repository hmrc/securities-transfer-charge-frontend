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

package views.sh03.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.MaximumAmountPaidFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.MaximumAmountPaidView
import views.ViewBaseSpec

class MaximumAmountPaidViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = orgAffinity).build()

  private val viewInstance = app.injector.instanceOf[MaximumAmountPaidView]
  private val formProvider = new MaximumAmountPaidFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")
  val messageKeyPrefix = "org.sh03.maximumAmountPaid.companyDetails"


  private val form = formProvider(affinityGroupKeyOrg)

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages(s"$messageKeyPrefix.title")
    val heading: String = messages(s"$messageKeyPrefix.heading")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The MaximumAmountPaidView" - {

    "when rendered without errors" - {

      val maximumAmountPaidView = view()

      "have the correct title" in {
        maximumAmountPaidView.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        maximumAmountPaidView.select("h1").text() mustBe ExpectedContent.heading
      }

      "have a pound prefix on the input" in {
        maximumAmountPaidView.select(".govuk-input__prefix").text() mustBe "£"
      }

      "have the correct input field" in {
        val input = maximumAmountPaidView.select("input[name=value]")

        input.attr("id") mustBe "value"
        input.attr("name") mustBe "value"
      }

      "have a save and continue button" in {
        maximumAmountPaidView.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        maximumAmountPaidView.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}