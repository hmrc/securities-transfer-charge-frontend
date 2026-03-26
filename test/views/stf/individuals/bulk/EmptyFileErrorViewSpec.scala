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

package views.stf.individuals.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.EmptyFileErrorView
import views.ViewBaseSpec

class EmptyFileErrorViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[EmptyFileErrorView]

  def view(): Document =
    Jsoup.parse(
      viewInstance()(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title: String =
      messages("fileUpload.error.emptyFile.title")

    val heading: String =
      messages("fileUpload.error.emptyFile.heading")

    val paragraph: String =
      messages("fileUpload.error.emptyFile.p1")

    val downloadText: String =
      messages("fileUpload.error.emptyFile.download.text")

    val buttonText: String =
      messages("fileUpload.upload.another")
  }

  "EmptyFileErrorView" - {

    "when rendered" - {

      val doc = view()

      "must have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "must have the correct heading" in {
        doc.select("h1").text() mustBe ExpectedContent.heading
      }

      "must have the correct paragraph text" in {
        doc.select(".govuk-body").first().text() mustBe ExpectedContent.paragraph
      }

      "must have the download link" in {
        val link = doc.select(".govuk-link").get(3)

        link.text() mustBe ExpectedContent.downloadText
        link.attr("href") mustBe "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1i.xlsx"
        link.attr("download") mustBe "Bulk Securities Transfer Charge template v1i.xlsx"
      }

      "must have a continue button" in {
        doc.select(".govuk-button").text() mustBe ExpectedContent.buttonText
      }

      "must post to the correct controller action" in {
        val form = doc.select("form")
        form.attr("action") mustBe routes.FileUploadController.onPageLoad().url
      }
    }
  }
}