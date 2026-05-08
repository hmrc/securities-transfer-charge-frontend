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

package views.stf.individuals.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes as bulkRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes 
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.TemplateInstructionsView
import views.ViewBaseSpec

class TemplateInstructionsViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[TemplateInstructionsView]

  def view(): Document = Jsoup.parse(
    viewInstance()(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title = "How to use the template"
    val heading = "How to use the template"
    val caption: String = "Transfer details"

    val para1Value = "Use the template to tell us about more than one securities transfer at a time."

    val step1 = "Download the template"
    val downloadHref = "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1i.xlsx"
    val downloadFileName = "Bulk Securities Transfer Charge template v1i.xlsx"

    val step2 = "Complete the template, using one row for each type of securities you are buying"
    val step3 = "Upload your file on the next page as a .xlsx or .csv"
    val para2Value = "Remember, do not edit the first two rows, or change the order of the columns."
    val para3Span1 = "Once you have uploaded and submitted your file it will show on your"
    val para3LinkText = "STC dashboard"
    val para3Span2 = "You can then pay any charges due."

    val continue = "Continue"
    val returnLink = "Return to dashboard"
  }

  "The TemplateInstructionsView" - {
    "the user is an Individual" - {
      val templateInstructionsView = view()

      "have the correct title" in {
        templateInstructionsView.title must include(ExpectedContent.title)
      }

      "have the correct heading" in {
        templateInstructionsView.select("h1").text() mustBe ExpectedContent.heading
      }

      "display the correct caption text" in {
        templateInstructionsView.getElementsByClass("govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "display the correct of first paragraph content" in {
        templateInstructionsView.para(1) mustBe Some(ExpectedContent.para1Value)
      }

      "display a numbered list with 3 items" in {
        templateInstructionsView.select("ol.govuk-list.govuk-list--number li").size() mustBe 3
      }

      "display the correct step 1 download link" in {
        val downloadLink = templateInstructionsView.select("ol.govuk-list.govuk-list--number li a.govuk-link").first()
        downloadLink.text() mustBe ExpectedContent.step1
        downloadLink.attr("href") mustBe ExpectedContent.downloadHref
        downloadLink.hasAttr("download") mustBe true
        downloadLink.attr("download") mustBe ExpectedContent.downloadFileName
      }

      "display the correct step 2 text" in {
        templateInstructionsView.select("ol.govuk-list.govuk-list--number li").get(1).text() mustBe ExpectedContent.step2
      }

      "display the correct step 3 text" in {
        templateInstructionsView.select("ol.govuk-list.govuk-list--number li").get(2).text() mustBe ExpectedContent.step3
      }

      "display the correct second paragraph" in {
        templateInstructionsView.para(2) mustBe Some(ExpectedContent.para2Value)
      }

      "display the correct third paragraph with 'STC dashboard' link that takes user back to the submission dashboard page" in {
        val paragraph = templateInstructionsView.select("p.govuk-body").last()
        val spans = paragraph.select("span")
        spans.get(0).text() mustBe ExpectedContent.para3Span1
        spans.get(1).select("a.govuk-link").text() mustBe ExpectedContent.para3LinkText
        spans.get(1).select("a.govuk-link").attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
        spans.get(1).text() mustBe ExpectedContent.para3LinkText + "."
        spans.get(2).text() mustBe ExpectedContent.para3Span2
      }


      "have a continue button" in {
        val continueButton = templateInstructionsView.select(".govuk-button").first()
        continueButton.text() mustBe ExpectedContent.continue
        continueButton.attr("href") mustBe bulkRoutes.FileUploadController.onPageLoad().url
      }

      "have a link to return back to the submission dashboard page" in {
        val returnLink = templateInstructionsView.select(".govuk-button-group a.govuk-link").first()
        returnLink.text() mustBe ExpectedContent.returnLink
        returnLink.attr("href") mustBe routes.SubmissionsDashboardController.onPageLoad().url
      }
    }
  }

}
