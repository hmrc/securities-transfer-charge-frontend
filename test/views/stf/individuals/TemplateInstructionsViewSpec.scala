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

package views.stf.individuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.TemplateInstructionsView
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
    val para3Value = "Once you have uploaded and submitted your file it will show on your STC dashboard. You can then pay any charges due."
    val para3BoldText = "STC dashboard"

    val saveAndContinue = "Save and continue"
    val saveAndReturn = "Save and return to dashboard"
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

      "display the correct dashboard paragraph with bold STC dashboard text" in {
        templateInstructionsView.para(3) mustBe Some(ExpectedContent.para3Value)
        templateInstructionsView.select("p.govuk-body strong").text() mustBe ExpectedContent.para3BoldText
      }

      "have a save and continue button" in {
        val form = templateInstructionsView.select("form")
        form.attr("action") mustBe routes.FileUploadController.onPageLoad().url
        form.attr("method") mustBe "GET"
        templateInstructionsView.select(".govuk-button").first().text() mustBe ExpectedContent.saveAndContinue
      }

      "have a save and return button" in {
        templateInstructionsView.select(".govuk-button--secondary").text() mustBe ExpectedContent.saveAndReturn
      }
    }
  }

}
