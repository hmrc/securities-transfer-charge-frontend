/*
 * Copyright 2025 HM Revenue & Customs
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
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.ChangedFileErrorView
import views.ViewBaseSpec

class ChangedFileErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[ChangedFileErrorView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "There is a problem with your uploaded file"
    val heading = "There is a problem with your uploaded file"

    val para1Value = "The header rows or columns have been changed."
    val para2Value = "Correct the errors then you can upload the file again, or upload a different file."
    val para3Value = "Remember, do not edit the first two rows of the template, or change the order of the columns."

    val downloadText = "Download the template file"
    val downloadHref = "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1b.xlsx"
    val downloadFileName = "Bulk Securities Transfer Charge template v1b.xlsx"
    val uploadAnother = "Upload a different file"
  }

  "The ChangedFileErrorView" - {
    "the user is an Individual" - {
      val changedFileErrorView = view()

      "have the correct title" in {
        changedFileErrorView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        changedFileErrorView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct of first paragraph content" in {
        changedFileErrorView.para(1) mustBe Some(ExpectedContent.para1Value)
      }

      "display the correct second paragraph" in {
        changedFileErrorView.para(2) mustBe Some(ExpectedContent.para2Value)
      }

      "display the correct third paragraph" in {
        changedFileErrorView.para(3) mustBe Some(ExpectedContent.para3Value)
      }

      "have a link to download the template" in {
        val downloadLink = changedFileErrorView.select("a[download]").first()
        downloadLink.text() mustBe ExpectedContent.downloadText
        downloadLink.attr("href") mustBe ExpectedContent.downloadHref
        downloadLink.hasAttr("download") mustBe true
        downloadLink.attr("download") mustBe ExpectedContent.downloadFileName
      }

      "have a button that redirects to the file upload page" in {
        val form = changedFileErrorView.select("form")
        form.attr("action") mustBe routes.JourneyRecoveryController.onPageLoad().url
        form.attr("method") mustBe "GET"
        changedFileErrorView.select(".govuk-button").first().text() mustBe ExpectedContent.uploadAnother
      }
    }
  }
}
