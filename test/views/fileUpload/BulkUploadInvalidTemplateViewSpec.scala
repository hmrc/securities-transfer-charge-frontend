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
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.fileUpload.BulkUploadInvalidTemplateView
import views.ViewBaseSpec
import views.helper.JsoupHelper

class BulkUploadInvalidTemplateViewSpec extends ViewBaseSpec with JsoupHelper {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[BulkUploadInvalidTemplateView]

  def view(): Document = Jsoup.parse(viewInstance("https://example.com/template",SH03)(fakeRequest, messages).body)

  object ExpectedContent {
    val title: String = messages("fileUpload.error.invalidTemplate.title")
    val heading: String = messages("fileUpload.error.invalidTemplate.heading")
    val paragraph: String = s"${messages("fileUpload.error.invalidTemplate.p.start")} ${messages("fileUpload.error.invalidTemplate.p.link")} ${messages("fileUpload.error.invalidTemplate.p.end")}"
  }

  "BulkUploadInvalidTemplateView" - {

    "when rendered" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        doc.heading mustBe Some(ExpectedContent.heading)
      }

      "have the correct paragraph text" in {
        doc.para(1) mustBe Some(ExpectedContent.paragraph)
      }

      "display a back link" in {
        doc.hasBackLink mustBe true
      }
    }
  }
}