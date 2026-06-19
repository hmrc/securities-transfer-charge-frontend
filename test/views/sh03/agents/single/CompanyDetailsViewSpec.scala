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

import forms.sh03.agents.CompanyDetailsFormProvider
import models.NormalMode
import play.api.data.Form
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.single.CompanyDetailsView
import views.ViewBaseSpec
import views.helper.JsoupHelper

class CompanyDetailsViewSpec extends ViewBaseSpec with JsoupHelper {

  val messageKeyPrefix = "agent.sh03.companyDetails"

  val form: Form[CompanyDetails] = new CompanyDetailsFormProvider()()

  val view: CompanyDetailsView = app.injector.instanceOf[CompanyDetailsView]

  def createView: () => HtmlFormat.Appendable = () =>
    view(form, NormalMode)(fakeRequest, messages)

  "CompanyDetailsView" - {

    behave like normalPage(createView, messageKeyPrefix)

    behave like pageWithBackLink(createView)

    behave like pageWithSubmitButton(createView)

    "must contain heading" in {
      val doc = asDocument(createView())
      assertContainsText(doc, messages(s"$messageKeyPrefix.heading"))
    }

    "must contain company name input" in {
      val doc = asDocument(createView())
      assertRenderedById(doc, "companyName")
      assertContainsLabel(doc, "companyName", messages(s"$messageKeyPrefix.companyName.label"))
    }

    "must contain CRN input with hint text" in {
      val doc = asDocument(createView())
      assertRenderedById(doc, "companyRegistrationNumber")
      assertContainsLabel(doc, "companyRegistrationNumber", messages(s"$messageKeyPrefix.crn.label"))
      assertContainsText(doc, messages(s"$messageKeyPrefix.crn.hint"))
    }

    "must contain PLC radio buttons" in {
      val doc = asDocument(createView())
      assertContainsText(doc, messages(s"$messageKeyPrefix.isPlc.label"))
      assertContainsText(doc, messages("site.yes"))
      assertContainsText(doc, messages("site.no"))
    }

    "must contain save and return button" in {
      val doc = asDocument(createView())
      assertContainsText(doc, messages("site.save-and-return.button"))
    }
  }

  "CompanyDetailsView with errors" - {

    "must show error summary when there are errors" in {
      val formWithErrors = form.bind(Map("companyName" -> ""))
      val doc = asDocument(view(formWithErrors, NormalMode)(fakeRequest, messages))
      assertRenderedById(doc, "error-summary-title")
    }

    "must show error for company name" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "",
        "companyRegistrationNumber" -> "AB123456",
        "isPlc" -> "true"
      ))
      val doc = asDocument(view(formWithErrors, NormalMode)(fakeRequest, messages))
      assertContainsText(doc, messages(s"$messageKeyPrefix.companyName.error.required"))
    }

    "must show error for CRN" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "Test Company",
        "companyRegistrationNumber" -> "ABC",
        "isPlc" -> "true"
      ))
      val doc = asDocument(view(formWithErrors, NormalMode)(fakeRequest, messages))
      assertContainsText(doc, messages(s"$messageKeyPrefix.crn.error.length"))
    }

    "must show error for PLC selection" in {
      val formWithErrors = form.bind(Map(
        "companyName" -> "Test Company",
        "companyRegistrationNumber" -> "AB123456"
      ))
      val doc = asDocument(view(formWithErrors, NormalMode)(fakeRequest, messages))
      assertContainsText(doc, messages(s"$messageKeyPrefix.isPlc.error.required"))
    }
  }
}
