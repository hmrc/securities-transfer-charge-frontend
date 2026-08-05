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

package views.stf.individuals.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.PurchasingSharesFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.single.PurchasingSharesView
import views.ViewBaseSpec


class PurchasingSharesViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = individualAffinity).build()

  private val viewInstance = app.injector.instanceOf[PurchasingSharesView]
  private val formProvider = new PurchasingSharesFormProvider()

  private val form = formProvider(affinityKey = affinityGroupKeyInd)

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("individual.purchasingShares.title")
    val caption: String = messages("transfer.details.caption")
    val heading: String = messages("individual.purchasingShares.heading")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
    val yes: String = messages("site.yes")
    val no: String = messages("site.no")
  }

  "WhatTypeOfSecuritiesView" - {

    val doc = view()

    "have the correct title" in {
      doc.title() must include(ExpectedContent.title)
    }

    "have the correct caption" in {
      doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
    }


    "have the correct radio buttons" in {
      val radios = doc.select(".govuk-radios").text()
      radios must include(ExpectedContent.yes)
      radios must include(ExpectedContent.no)
      doc.select("govuk-radios--inline") mustBe empty
    }

    "have a save and continue button" in {
      doc.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
    }

    "have a save and return button" in {
      doc.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
    }
  }
}
