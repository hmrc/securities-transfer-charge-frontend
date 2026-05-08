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
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileProcessingView
import views.ViewBaseSpec

class FileProcessingViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[FileProcessingView]

  def view(): Document = Jsoup.parse(viewInstance()(fakeRequest, messages).body)

  object ExpectedContent {
    val title: String = messages("fileUpload.processing.title")

    val paragraph: String = messages("fileUpload.processing.paragraph")
  }

  "FileProcessingView" - {

    "when rendered" - {

      val doc = view()

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        val heading = doc.select("h1")
        heading.html() must include(ExpectedContent.title)
      }

      "have the correct paragraph text" in {
        doc.select(".govuk-body").text() mustBe ExpectedContent.paragraph
      }

      "display the loading spinner" in {
        doc.select(".loading-spinner__spinner").size() mustBe 1
      }

      "have a noscript refresh tag" in {
        val noscriptMeta = doc.select("noscript meta[http-equiv=refresh]")

        noscriptMeta.attr("content") mustBe "2"
      }

      "not display a back link" in {
        doc.select(".govuk-back-link").size() mustBe 0
      }
    }
  }
}