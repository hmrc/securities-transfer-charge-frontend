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

package views.sh03.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.DetailsOfThisSharePurchaseFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.DetailsOfThisSharePurchaseView
import views.ViewBaseSpec


class DetailsOfThisSharePurchaseViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[DetailsOfThisSharePurchaseView]
  private val formProvider = new  DetailsOfThisSharePurchaseFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val form = formProvider(affinityKey = affinityGroupKeyAgent)

  def view(requireMarketValue:Boolean): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute,requireMarketValue)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agent.sh03.detailsOfSharePurchase.title")
    val heading: String = messages("agent.sh03.detailsOfSharePurchase.heading")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
    val numberOfShares: String = messages("agent.sh03.detailsOfSharePurchase.numberOfShares")
    val typeOfShares: String = messages("agent.sh03.detailsOfSharePurchase.typeOfShares")
    val amountPaid: String = messages("agent.sh03.detailsOfSharePurchase.amountPaid")
    val marketValue: String = messages("agent.sh03.detailsOfSharePurchase.marketValue")
  }

  "The DetailsOfThisSharePurchase" - {

    "render view" - {

      val doc = view(requireMarketValue = true)

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
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
