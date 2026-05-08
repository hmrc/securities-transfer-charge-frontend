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

package views.stf.shared.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FormattingErrorView
import views.ViewBaseSpec

class FormattingErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[FormattingErrorView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "Your file has formatting errors"
    val heading = "Your file has formatting errors"

    val para1Value = "More than 25 formatting errors have been detected in this file."
    val para2Value = "The errors may include things like incorrect formatting of numbers or dates, or a letters in a cell that can only contain numbers."
    val para3Value = "These will need to be corrected before your file can be uploaded again."
    val para4Value = "Check the instructions in the template file for more guidance."

    val backToFIle = "Back to file upload"
  }

  "The FormattingErrorView" - {
    "the user is an Individual" - {
      val formattingErrorView = view()

      "have the correct title" in {
        formattingErrorView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        formattingErrorView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct of first paragraph content" in {
        formattingErrorView.para(1) mustBe Some(ExpectedContent.para1Value)
      }

      "display the correct second paragraph" in {
        formattingErrorView.para(2) mustBe Some(ExpectedContent.para2Value)
      }

      "display the correct third paragraph" in {
        formattingErrorView.para(3) mustBe Some(ExpectedContent.para3Value)
      }

      "display the correct fourth paragraph" in {
        formattingErrorView.para(4) mustBe Some(ExpectedContent.para4Value)
      }

      "have a button that redirects to the file upload page" in {
        val form = formattingErrorView.select("form")
        form.attr("action") mustBe routes.FileUploadController.onPageLoad().url
        form.attr("method") mustBe "GET"
        formattingErrorView.select(".govuk-button").first().text() mustBe ExpectedContent.backToFIle
      }
    }
  }
}
