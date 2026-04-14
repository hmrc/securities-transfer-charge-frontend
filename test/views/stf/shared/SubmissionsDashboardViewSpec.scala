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

package views.stf.shared

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.SubmissionsDashboardView
import views.ViewBaseSpec

class SubmissionsDashboardViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance         = app.injector.instanceOf[SubmissionsDashboardView]

  private def view(submissionIds: List[SubmissionId]): Document =
    Jsoup.parse(
      viewInstance(submissionIds)(fakeRequest, messages).body
    )

  object ExpectedContent {
    val title = "Submissions"
    val heading = "Submissions"
    val createNew = "Create new submission"
    
  }

  "SubmissionsDashboardView (empty state)" - {

    val submissionsDashboardView = view(List.empty)

    "have the correct title" in {
      submissionsDashboardView.title must include(ExpectedContent.title)
    }

    "have the correct heading" in {
      submissionsDashboardView.select("h1").text() mustBe ExpectedContent.heading
    }

    "have a create button with the correct text" in {
      val button = submissionsDashboardView.select(".govuk-button")
      button.text() mustBe ExpectedContent.createNew
    }

    "should not have a back link" in {
      submissionsDashboardView.hasBackLink mustBe false
    }
  }

  "SubmissionsDashboardView (with submissions)" - {

    val submissionIds = List(submissionId)

    val submissionsDashboardView = view(submissionIds)

    "display the submissions table" in {
      submissionsDashboardView.select(".govuk-table").size() mustBe 1
    }

    "display the submission id in the table" in {
      submissionsDashboardView.select("tbody tr td").first().text() mustBe submissionId.value
    }
  }
}
