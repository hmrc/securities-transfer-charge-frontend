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

package views.sh03.agents.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.bulk.CompanyDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.bulk.CompanyDetailsView
import views.ViewBaseSpec

class CompanyDetailsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val messageKeyPrefix = "agent.sh03.bulk.companyDetails"

  private val viewInstance = app.injector.instanceOf[CompanyDetailsView]
  private val formProvider = new CompanyDetailsFormProvider()
  private val form = formProvider()

  private def view(): Document =
    Jsoup.parse(
      viewInstance(form, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages(s"$messageKeyPrefix.title")
    val heading: String = messages(s"$messageKeyPrefix.heading")
    val companyNameLabel: String = messages(s"$messageKeyPrefix.companyName.label")
    val crnLabel: String = messages(s"$messageKeyPrefix.crn.label")
    val crnHint: String = messages(s"$messageKeyPrefix.crn.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "CompanyDetailsView" - {

    val doc = view()

    "must have the correct title" in {
      doc.title must include(ExpectedContent.title)
    }

    "must contain the heading" in {
      doc.select("h1").text() mustBe ExpectedContent.heading
    }

    "must contain the company name input" in {
      doc.select("#companyName").size() mustBe 1
      doc.select("label[for=companyName]").text() mustBe ExpectedContent.companyNameLabel
    }

    "must contain the CRN input" in {
      doc.select("#companyRegistrationNumber").size() mustBe 1
      doc.select("label[for=companyRegistrationNumber]").text() mustBe ExpectedContent.crnLabel
      doc.text() must include(ExpectedContent.crnHint)
    }

    "must contain a save and continue button" in {
      doc.select(".govuk-button").get(0).text() mustBe ExpectedContent.saveAndContinue
    }

    "must contain a save and return button" in {
      doc.select(".govuk-button").get(1).text() mustBe ExpectedContent.saveAndReturn
    }

    "must have a back link" in {
      doc.hasBackLink mustBe true
    }
  }

  "CompanyDetailsView with errors" - {

    "must display an error summary" in {
      val formWithErrors = form.bind(Map.empty[String, String])

      val doc = Jsoup.parse(
        viewInstance(formWithErrors, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
      )

      doc.hasErrorSummary mustBe true
    }

    "must display a company name error" in {
      val formWithErrors = form.bind(
        Map(
          "companyName" -> "",
          "companyRegistrationNumber" -> "12345678"
        )
      )

      val doc = Jsoup.parse(
        viewInstance(formWithErrors, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
      )

      doc.text() must include(
        messages(s"$messageKeyPrefix.companyName.error.required")
      )
    }

    "must display a CRN error" in {
      val formWithErrors = form.bind(
        Map(
          "companyName" -> "Test Company",
          "companyRegistrationNumber" -> "123"
        )
      )

      val doc = Jsoup.parse(
        viewInstance(formWithErrors, NormalMode, testBackLinkRoute)(fakeRequest, messages).body
      )

      doc.text() must include(
        messages(s"$messageKeyPrefix.crn.error.length")
      )
    }
  }
}