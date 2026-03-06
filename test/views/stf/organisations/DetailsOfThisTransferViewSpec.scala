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

package views.stf.organisations

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.DetailsOfThisTransferFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.DetailsOfThisTransferView
import views.ViewBaseSpec


class DetailsOfThisTransferViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[DetailsOfThisTransferView]
  private val formProvider = new  DetailsOfThisTransferFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val form = formProvider()

  def view(requireMarketValue:Boolean): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute,requireMarketValue)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("detailsOfThisTransfer.title")
    val caption: String = messages("transfer.details.caption")
    val heading: String = messages("detailsOfThisTransfer.heading")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
    val numberOfShares: String = messages("detailsOfThisTransfer.numberOfShares")
    val typeOfShares: String = messages("detailsOfThisTransfer.typeOfShares")
    val amountPaid: String = messages("detailsOfThisTransfer.amountPaid")
    val marketValue: String = messages("detailsOfThisTransfer.marketValue")
  }

  "The DetailsOfThisTransferView" - {

    "render view" - {

      val doc = view(requireMarketValue = true)

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        doc.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the correct input text " in {
        doc.select(".govuk-label").text()  must include(ExpectedContent.numberOfShares)
        doc.select(".govuk-label").text()  must include(ExpectedContent.typeOfShares)
        doc.select(".govuk-label").text()  must include(ExpectedContent.amountPaid)
        doc.select(".govuk-label").text()  must include(ExpectedContent.marketValue)
      }

      "have a save and continue button" in {
        doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
    "when rendered without marketValue" - {
      val doc = view(requireMarketValue = false)

      "must not render the market value input" in {
        doc.select(".govuk-label").text() must not include(ExpectedContent.marketValue)
      }
    }

  }
}
