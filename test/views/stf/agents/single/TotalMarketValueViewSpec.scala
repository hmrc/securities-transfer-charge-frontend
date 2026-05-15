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

package views.stf.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.data.Form
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.TotalMarketValueFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.single.TotalMarketValueView
import views.ViewBaseSpec


class TotalMarketValueViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[TotalMarketValueView]

  private val formProvider = new TotalMarketValueFormProvider()

  private val backLink: Call = Call("GET", "/back-link")

  private val form: Form[BigDecimal] = formProvider()

  private def view(form: Form[BigDecimal] = form): Document =
    Jsoup.parse(
      viewInstance(form, NormalMode, backLink)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String           = messages("agent.totalMarketValue.title")
    val caption: String         = messages("transfer.details.caption")
    val heading: String         = messages("agent.totalMarketValue.heading")
    val hint: String            = messages("agent.totalMarketValue.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String   = messages("site.save-and-return.button")
  }

  "TotalMarketValueView" - {

    "when rendered without errors" - {

      val doc: Document = view()

      "must have the correct page title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "must have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "must have the correct heading" in {
        doc.select("h1").text() mustBe ExpectedContent.heading
      }

      "must display the hint text" in {
        doc.select(".govuk-hint").text() mustBe ExpectedContent.hint
      }

      "must have a save and continue button" in {
        doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "must have a save and return button" in {
        doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }

      "must have a backlink" in {
        doc.select(".govuk-back-link").attr("href") mustBe backLink.url
      }

      "must contain the value input field" in {
        doc.select("#value").size() mustBe 1
      }
    }

    "when rendered with form errors" - {

      val formWithErrors: Form[BigDecimal] = form.bind(Map("value" -> ""))

      val doc: Document = view(formWithErrors)

      "must display the error summary" in {
        doc.select(".govuk-error-summary").size() mustBe 1
      }

      "must display the field error message" in {
        doc.select(".govuk-error-message").text() must include(
          messages("agent.totalMarketValue.error.required")
        )
      }
    }
  }
}