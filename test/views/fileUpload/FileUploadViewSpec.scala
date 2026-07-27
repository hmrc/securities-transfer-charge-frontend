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

package views.fileUpload

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.test.FakeRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UploadRequest
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.FileUploadView
import views.ViewBaseSpec

class FileUploadViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[FileUploadView]

  private val fakeRequest = FakeRequest()

  private val uploadRequest = UploadRequest(
    href = "/upscan/upload",
    fields = Map(
      "key" -> "test-key",
      "policy" -> "test-policy"
    )
  )

  def view(): Document =
    Jsoup.parse(
      viewInstance(uploadRequest,journeyType = JourneyType.STF)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("fileUpload.title")
    val caption: String = messages("transfer.details.caption")
    val heading: String = messages("fileUpload.heading")
    val label: String = messages("fileUpload.label")
    val button: String = messages("site.upload.file")
    val insetText: String = messages("fileUpload.inset.text")
    val returnToDashboard: String = messages("return-to-dashboard.link")
  }

  "FileUploadView" - {

    "render view" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct heading" in {
        doc.select("h1").text() mustBe ExpectedContent.heading
      }

      "have the inset text" in {
        doc.select(".govuk-inset-text").text() mustBe ExpectedContent.insetText
      }

      "have the file upload label" in {
        doc.select(".govuk-label").text() must include(ExpectedContent.label)
      }

      "have a file upload input" in {
        doc.select("input[type=file]").attr("id") mustBe "file-input"
      }

      "have a upload button" in {
        doc.select(".govuk-button").text() mustBe ExpectedContent.button
      }

      "have a link to return back to the submission dashboard page" in {
        val returnLink = doc.select(".govuk-button-group a.govuk-link").first()
        returnLink.text() mustBe ExpectedContent.returnToDashboard
        returnLink.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }

      "have the correct form action" in {
        doc.select("form").attr("action") mustBe uploadRequest.href
      }

      "render hidden fields from upscan response" in {
        val hiddenInputs = doc.select("input[type=hidden]")
        hiddenInputs.size() mustBe uploadRequest.fields.size

        uploadRequest.fields.foreach { case (key, value) =>
          doc.select(s"input[name=$key]").attr("value") mustBe value
        }
      }
    }

    "must not have a caption when journey type is SH03" in {
      val view = Jsoup.parse(viewInstance(uploadRequest,journeyType = JourneyType.SH03)(fakeRequest, messages).body)
      view.select(".govuk-caption-l").text() mustBe empty
    }
  }
}
