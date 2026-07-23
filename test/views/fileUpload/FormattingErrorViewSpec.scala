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

package views.fileUpload

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.fileUpload.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.FormattingErrorView
import views.ViewBaseSpec

class FormattingErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[FormattingErrorView]

  def view(): Document = Jsoup.parse(
    viewInstance(STF)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("formattingError.title")
    val heading: String = messages("formattingError.heading")

    val para1Value: String = messages("formattingError.p1")
    val para2Value: String = messages("formattingError.p2")
    val para3Value: String = messages("formattingError.p3")
    val para4Value: String = messages("formattingError.p4")

    val backToFIle: String = messages("site.back-to-upload.button")
  }

  "The FormattingErrorView" - {
    "render view" - {
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

      "display a back link that redirects to the correct page" in {
        val link = formattingErrorView.select(".govuk-link").get(3)
        link.text() mustBe messages("site.back-to-upload.button")
        link.attr("href") mustBe routes.FileUploadController.onPageLoad(STF).url
      }
    }
  }
}
