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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.ReasonForPurchaseFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.ReasonForPurchaseView
import views.ViewBaseSpec

class ReasonForPurchaseViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[ReasonForPurchaseView]
  private val formProvider = new ReasonForPurchaseFormProvider()

  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("agent.sh03.reasonForPurchase.title")
    val heading: String = messages("agent.sh03.reasonForPurchase.heading")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")

    val cancellation: String = messages("reasonForPurchase.cancellation")
    val treasury: String = messages("reasonForPurchase.treasury")

  }

  "ReasonForPurchaseView" - {

    val doc = view()

    "have the correct title" in {
      doc.title() must include(ExpectedContent.title)
    }

    "have the correct heading" in {
      doc.select("h1").text() mustBe ExpectedContent.heading
    }

    "have the correct radio buttons" in {
      val radios = doc.select(".govuk-radios").text()

      radios must include(ExpectedContent.cancellation)
      radios must include(ExpectedContent.treasury)

    }

    "have a save and continue button" in {
      doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
    }

    "have a save and return button" in {
      doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
    }
  }
}


