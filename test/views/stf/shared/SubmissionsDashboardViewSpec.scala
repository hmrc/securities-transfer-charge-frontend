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

package views.stf.shared

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.Html
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.SubmissionsDashboardView
import views.ViewBaseSpec
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes

class SubmissionsDashboardViewSpec extends ViewBaseSpec {

  private val view: SubmissionsDashboardView = app.injector.instanceOf[SubmissionsDashboardView]

  private val emptySubmissionIds: List[SubmissionId] = List.empty
  private val submissionIds: List[SubmissionId] = List(
    SubmissionId("sub-001"),
    SubmissionId("sub-002")
  )

  "SubmissionsDashboardView" - {

    "when rendered with no submissions" - {

      val html: Html = view(emptySubmissionIds)(fakeRequest, messages)
      val document: Document = Jsoup.parse(html.toString())

      "must display the page title" in {
        document.title() must include(messages("submissionsDashboard.title"))
      }

      "must display the heading" in {
        document.getElementsByClass("govuk-heading-l").text() mustBe messages("submissionsDashboard.heading")
      }

      "must display the hint text" in {
        document.getElementById("caption-heading").text() mustBe messages("submissionsDashboard.hint")
      }

      "must display the STF create new submission button" in {
        val buttons = document.getElementsByClass("govuk-button")
        buttons.size() mustBe 2
        
        val stfButton = buttons.get(0)
        stfButton.text() mustBe messages("submissionsDashboard.create-new-submission.button")
        stfButton.hasClass("govuk-button--secondary") mustBe false
      }

      "must display the SH03 start button" in {
        val buttons = document.getElementsByClass("govuk-button")
        buttons.size() mustBe 2
        
        val sh03Button = buttons.get(1)
        sh03Button.text() mustBe "start sh03"
        sh03Button.hasClass("govuk-button--secondary") mustBe true
      }

      "must have the SH03 button as a link" in {
        val buttons = document.getElementsByClass("govuk-button")
        val sh03Button = buttons.get(1)
        
        sh03Button.tagName() mustBe "a"
        sh03Button.attr("href") mustBe routes.SubmissionsDashboardController.startSh03().url
      }

      "must have the STF button as a submit button" in {
        val buttons = document.getElementsByClass("govuk-button")
        val stfButton = buttons.get(0)
        
        stfButton.tagName() mustBe "button"
        stfButton.attr("type") mustBe "submit"
      }

      "must not display the submissions table" in {
        document.select("table").isEmpty mustBe true
      }
    }

    "when rendered with submissions" - {

      val html: Html = view(submissionIds)(fakeRequest, messages)
      val document: Document = Jsoup.parse(html.toString())

      "must display the page title" in {
        document.title() must include(messages("submissionsDashboard.title"))
      }

      "must display both buttons" in {
        val buttons = document.getElementsByClass("govuk-button")
        buttons.size() mustBe 2
      }

      "must display the STF button with correct text" in {
        val buttons = document.getElementsByClass("govuk-button")
        val stfButton = buttons.get(0)
        stfButton.text() mustBe messages("submissionsDashboard.create-new-submission.button")
      }

      "must display the SH03 button with correct text" in {
        val buttons = document.getElementsByClass("govuk-button")
        val sh03Button = buttons.get(1)
        sh03Button.text() mustBe "start sh03"
      }

      "must display the submissions table" in {
        document.select("table").isEmpty mustBe false
      }
    }
  }
}