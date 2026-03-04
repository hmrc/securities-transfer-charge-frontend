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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.organisations.WhatReliefAreYouApplyingForFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, ReliefsDataSource}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.WhatReliefAreYouApplyingForView
import views.ViewBaseSpec


class WhatReliefAreYouApplyingForViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[WhatReliefAreYouApplyingForView]
  private val source = app.injector.instanceOf[ReliefsDataSource]
  private val formProvider = new WhatReliefAreYouApplyingForFormProvider()
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, source.reliefs, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("orgs.whatReliefAreYouApplyingFor.title")
    val caption: String = messages("transfer.details.caption")
    val heading: String = messages("orgs.whatReliefAreYouApplyingFor.heading")
    val paragraph: String = messages("orgs.whatReliefAreYouApplyingFor.p")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The WhatReliefAreYouApplyingForView" - {

    "when rendered without errors" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        doc.select("h1").text() must include(ExpectedContent.heading)
      }

      "have the first paragraph" in {
        doc.select(".govuk-hint").text() must include(ExpectedContent.paragraph)
      }

      "have a save and continue button" in {
        doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }
}
