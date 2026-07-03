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

package views.sh03.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.organisations.CompanyDetailsFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.CompanyDetailsView
import views.ViewBaseSpec

class CompanyDetailsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder(affinityGroup = orgAffinity).build()

  val messageKeyPrefix = "org.sh03.companyDetails"

  private val viewInstance = app.injector.instanceOf[CompanyDetailsView]
  private val formProvider = new CompanyDetailsFormProvider()
  private val form = formProvider()

  def view(): Document = Jsoup.parse(
    viewInstance(form, NormalMode)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages(s"$messageKeyPrefix.title")
    val heading: String = messages(s"$messageKeyPrefix.heading")
    val hint: String = messages(s"$messageKeyPrefix.crn.hint")
    val saveAndContinue: String = messages("site.save-and-continue.button")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "CompanyDetailsView" - {
    val doc = view()

    "have the correct title" in {
      doc.title must include(ExpectedContent.title)
    }

    "must contain heading" in {
      doc.select("h1").text() must include(ExpectedContent.heading)
    }

    "display the correct hint text" in {
      doc.hintText mustBe Some(ExpectedContent.hint)
    }

    "must contain save and continue button" in {
      val buttons = doc.select(".govuk-button")
      buttons.get(0).text() mustBe ExpectedContent.saveAndContinue
    }

    "must contain save and return button" in {
      val buttons = doc.select(".govuk-button")
      buttons.get(1).text() mustBe ExpectedContent.saveAndReturn
    }

    "must have back link" in {
      doc.hasBackLink mustBe true
    }
  }
}
