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
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ConfirmableAddress, Country}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.ConfirmAddressView
import views.ViewBaseSpec

class ConfirmAddressViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[ConfirmAddressView]

  val address: ConfirmableAddress = ConfirmableAddress(
    lines = List(
      "1 High Street",
      "Town"
    ),
    postcode = "ZZ1 1ZZ",
    country = Some(Country("United Kingdom", "GB"))
  )

  def view(): Document = Jsoup.parse(
    viewInstance(address)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("org.confirmAddress.title")
    val heading: String = messages("org.confirmAddress.heading")
    val confirmAddress: String = messages("org.confirmAddress.confirm")
    val caption: String = messages("org.details.caption")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The ConfirmAddress" - {
    "create a view" - {
      val confirmAddressView = view()

      "have the correct title" in {
        confirmAddressView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        confirmAddressView.select("h1").text() mustBe ExpectedContent.heading
      }

      "have the correct caption" in {
        confirmAddressView.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "contain all address fields within the address div" in {
        val addressDiv = confirmAddressView.select("div#address")

        addressDiv.select("#line1").text() mustBe "1 High Street"
        addressDiv.select("#line2").text() mustBe "Town"
        addressDiv.select("#postCode").text() mustBe "ZZ1 1ZZ"
        addressDiv.select("#country").text() mustBe "United Kingdom"
      }

      "have a confirm button with the correct text" in {
        val button = confirmAddressView.select(".govuk-button").first()
        button.text() mustBe ExpectedContent.confirmAddress
      }

      "have a save and return button" in {
        confirmAddressView.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
