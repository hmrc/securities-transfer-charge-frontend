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
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.EncryptedFileErrorView
import views.ViewBaseSpec

class EncryptedFileErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[EncryptedFileErrorView]

  def view(): Document = Jsoup.parse(viewInstance(SH03)(fakeRequest, messages).body)

  object ExpectedContent {
    val title: String = messages("fileUploaded.error.encryptedFile.title")
    val heading: String = messages("fileUploaded.error.encryptedFile.heading")
    val paragraph: String = messages("fileUploaded.error.encryptedFile.p")
  }

  "EncryptedFileErrorView" - {

    "when rendered" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        val heading = doc.select("h1")
        heading.html() must include(ExpectedContent.heading)
      }

      "have the correct paragraph text" in {
        doc.select(".govuk-body").first().text() mustBe ExpectedContent.paragraph
      }

      "display a back link" in {
        doc.select(".govuk-back-link").size() mustBe 1
      }
    }
  }
}