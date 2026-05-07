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

package views.stf.shared.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileUploadedView
import views.ViewBaseSpec

class FileUploadedViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[FileUploadedView]

  private val testFileUpload = FileUpload(
    reference = "ref123",
    status = UpscanJourneyStatus.Ready,
    downloadUrl = None,
    uploadDetails = None,
    failureReason = None,
    message = None
  )

  def view(): Document =
    Jsoup.parse(
      viewInstance(testFileUpload)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String = messages("fileUploaded.title")
    val heading: String = messages("fileUploaded.heading")
    val referenceLabel: String = messages("fileUploaded.reference")
    val statusLabel: String = messages("fileUploaded.status")
  }

  "FileUploadedView" - {

    "when rendered" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        doc.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the reference" in {
        val text = doc.select(".govuk-body").get(0).text()
        text must include(ExpectedContent.referenceLabel)
        text must include(testFileUpload.reference)
      }

      "display the status" in {
        val text = doc.select(".govuk-body").get(1).text()
        text must include(ExpectedContent.statusLabel)
        text must include(testFileUpload.status.toString)
      }
    }
  }
}
