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

package views.stf.individuals

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.individual.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.DashboardView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as stfRoutes
import views.ViewBaseSpec

class DashboardViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[DashboardView]

  private val displayName = "Test Name"

  def view(submissions: SubmissionsViewModel): Document = Jsoup.parse(
    viewInstance(submissions, displayName)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("dashboard.title")
    val caption: String = messages("dashboard.caption")
    val createSubmissionHeading: String = messages("dashboard.create-submission.heading")
    val createSubmissionStf: String = messages("dashboard.create-submission.stf")
    val submissionsHeading: String = messages("dashboard.submissions.heading")
    val viewAllRecent: String = messages("dashboard.submissions.view-all-recent")
    val viewOverdue: String = messages("dashboard.submissions.view-overdue")
    val viewReadyToPay: String = messages("dashboard.submissions.view-ready-to-pay")
    val viewDrafts: String = messages("dashboard.submissions.view-drafts")
  }

  "The DashboardView" - {

    "when rendered with submission counts greater than zero" - {

      val submissions = SubmissionsViewModel(
        overdueCount = 2,
        readyToPayCount = 3,
        draftCount = 4,
        recentCount = 1
      )

      val doc = view(submissions)

      "have the correct title" in {
        doc.title() must include(ExpectedContent.title)
      }

      "have the correct caption" in {
        doc.select(".govuk-caption-l").text() mustBe ExpectedContent.caption
      }

      "have the correct display name" in {
        doc.select("h1").text() mustBe displayName
      }

      "have the create submission heading" in {
        doc.select(".design-system-card__heading").get(0).text() mustBe
          ExpectedContent.createSubmissionHeading
      }

      "have the create STF submission link" in {
        doc.select(".design-system-card").get(0).select("a").text() mustBe
          ExpectedContent.createSubmissionStf
      }

      "have the submissions heading" in {
        doc.select(".design-system-card__heading").get(1).text() mustBe
          ExpectedContent.submissionsHeading
      }

      "have the view all recent submissions link" in {
        doc.select(".design-system-card").get(1).select("a").get(0).text() mustBe
          ExpectedContent.viewAllRecent
      }

      "route the view all recent submissions link to '#' when counts exist" in {
        doc.select(".design-system-card").get(1).select("a").get(0).attr("href") mustBe "#"
      }

      "show the overdue count" in {
        doc.text() must include("You have 2 overdue submissions.")
      }

      "show the ready to pay count" in {
        doc.text() must include("You have 3 submissions that are ready to pay.")
      }

      "show the draft count" in {
        doc.text() must include("You have 4 draft submissions.")
      }

      "show the overdue link" in {
        doc.select("a").text() must include(ExpectedContent.viewOverdue)
      }

      "show the ready to pay link" in {
        doc.select("a").text() must include(ExpectedContent.viewReadyToPay)
      }

      "show the drafts link" in {
        doc.select("a").text() must include(ExpectedContent.viewDrafts)
      }
    }

    "when all submission counts are zero" - {

      val submissions = SubmissionsViewModel(
        overdueCount = 0,
        readyToPayCount = 0,
        draftCount = 0,
        recentCount = 0
      )

      val doc = view(submissions)

      "route the view all recent submissions link to the empty recent submissions page" in {
        doc.select(".design-system-card").get(1).select("a").get(0).attr("href") mustBe stfRoutes.RecentSubmissionsController.onPageLoad().url
      }

      "show the overdue count" in {
        doc.text() must include("You have 0 overdue submissions.")
      }

      "not show the overdue link" in {
        doc.select("a").text() must not include ExpectedContent.viewOverdue
      }

      "show the ready to pay count" in {
        doc.text() must include("You have 0 submissions that are ready to pay.")
      }

      "not show the ready to pay link" in {
        doc.select("a").text() must not include ExpectedContent.viewReadyToPay
      }

      "show the draft count" in {
        doc.text() must include("You have 0 draft submissions.")
      }

      "not show the drafts link" in {
        doc.select("a").text() must not include ExpectedContent.viewDrafts
      }
    }
  }
}