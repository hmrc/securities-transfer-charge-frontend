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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.CompanyDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.CompanyDetailsView
import views.ViewBaseSpec

class CompanyDetailsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder(affinityGroup = agentAffinity).build()

  val messageKeyPrefix = "agent.sh03.companyDetails"

  private val viewInstance = app.injector.instanceOf[CompanyDetailsView]
  private val formProvider = new CompanyDetailsFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode,testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages(s"$messageKeyPrefix.title")
    val heading: String = messages(s"$messageKeyPrefix.heading")
    val companyNameLabel: String = messages(s"$messageKeyPrefix.companyName.label")
    val crnLabel: String = messages(s"$messageKeyPrefix.crn.label")
    val crnHint: String = messages(s"$messageKeyPrefix.crn.hint")
    val isPlcLabel: String = messages(s"$messageKeyPrefix.isPlc.label")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "CompanyDetailsView" - {

    "must contain heading" in {
      val doc = view()
      doc.select("h1").text() must include(ExpectedContent.heading)
    }

    "must contain company name input" in {
      val doc = view()
      doc.select("#companyName").size() mustBe 1
      doc.select("label[for=companyName]").text() must include(ExpectedContent.companyNameLabel)
    }

    "must contain CRN input with hint text" in {
      val doc = view()
      doc.select("#companyRegistrationNumber").size() mustBe 1
      doc.select("label[for=companyRegistrationNumber]").text() must include(ExpectedContent.crnLabel)
      doc.text() must include(ExpectedContent.crnHint)
    }

    "must contain PLC radio buttons" in {
      val doc = view()
      doc.text() must include(ExpectedContent.isPlcLabel)
      doc.text() must include(messages("site.yes"))
      doc.text() must include(messages("site.no"))
    }

    "must contain save and continue button" in {
      val doc = view()
      val buttons = doc.select(".govuk-button")
      buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
    }

    "must contain save and return button" in {
      val doc = view()
      val buttons = doc.select(".govuk-button")
      buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
    }

    "must have back link" in {
      val doc = view()
      doc.hasBackLink mustBe true
    }
  }

  "CompanyDetailsView with errors" - {

    "must show error summary when there are errors" in {
      val formWithErrors = form.bind(Map("companyName" -> ""))
      val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode,testBackLinkRoute)(fakeRequest, messages).body)
      doc.hasErrorSummary mustBe true
    }

    "must show error for company name" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "",
        "companyRegistrationNumber" -> "AB123456",
        "isPlc" -> "true"
      ))
      val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode,testBackLinkRoute)(fakeRequest, messages).body)
      doc.text() must include(messages(s"$messageKeyPrefix.companyName.error.required"))
    }

    "must show error for CRN" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "Test Company",
        "companyRegistrationNumber" -> "ABC",
        "isPlc" -> "true"
      ))
      val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode,testBackLinkRoute)(fakeRequest, messages).body)
      doc.text() must include(messages(s"$messageKeyPrefix.crn.error.length"))
    }

    "must show error for PLC selection" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "Test Company",
        "companyRegistrationNumber" -> "AB123456"
      ))
      val doc = Jsoup.parse(viewInstance(formWithErrors, NormalMode,testBackLinkRoute)(fakeRequest, messages).body)
      doc.text() must include(messages(s"$messageKeyPrefix.isPlc.error.required"))
    }
  }
}
