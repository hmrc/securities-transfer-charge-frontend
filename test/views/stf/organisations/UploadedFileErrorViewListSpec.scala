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

import base.Fixtures
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.UploadedFileErrorListView
import views.ViewBaseSpec

class UploadedFileErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[UploadedFileErrorListView]

  def view(): Document =
    Jsoup.parse(
      viewInstance(Fixtures.uploadedFileErrors)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("org.uploadedFileErrorList.title")
    val heading: String = messages("org.uploadedFileErrorList.heading")
    val paragraph: String = messages("org.uploadedFileErrorList.p1")

    val tableCaption: String = messages("org.uploadedFileErrorList.table.heading")
    val cellHeader: String = messages("org.uploadedFileErrorList.table.cell")
    val errorHeader: String = messages("org.uploadedFileErrorList.table.error")
  }

  "UploadedFileErrorView" - {

    "when rendered" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        doc.select("h1").text() mustBe ExpectedContent.heading
      }

      "have the correct paragraph" in {
        doc.select(".govuk-body").first().text() mustBe ExpectedContent.paragraph
      }

      "have the correct table headers" in {
        val headers = doc.select("th")
        headers.get(0).text() mustBe ExpectedContent.cellHeader
        headers.get(1).text() mustBe ExpectedContent.errorHeader
      }

      "have the correct table caption" in {
        doc.select("caption").text() mustBe ExpectedContent.tableCaption
      }

      "display all errors in the table" in {
        val rows = doc.select("tbody tr")

        rows.size() mustBe Fixtures.uploadedFileErrors.size

        rows.get(0).text() must include("J6")
        rows.get(0).text() must include("The seller's name cannot contain numbers")

        rows.get(1).text() must include("J36")
        rows.get(1).text() must include("You have selected that the buyer is a company, you need to enter the registered address")

        rows.get(2).text() must include("K3")
        rows.get(2).text() must include("Buyer's country can only contain letters, numbers and hyphens")
      }

      "have a back to file upload button" in {
        doc.select(".govuk-button").text() mustBe messages("site.back-to-upload.button")
      }

      "file upload button must redirect to the correct page" in {
        val form = doc.select("form")
        form.attr("action") mustBe routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
