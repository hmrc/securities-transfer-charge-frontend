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

package views.dashboard

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.SubmissionsViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DashboardView
import views.ViewBaseSpec

class DashboardViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[DashboardView]

  private val displayName = "Test Name"

  private def view(submissions: SubmissionsViewModel, isIndividual: Boolean): Document =
    Jsoup.parse(viewInstance(submissions, displayName, isIndividual)(fakeRequest, messages).body)

  object ExpectedContent {
    val title: String = messages("dashboard.title")

    val caption: String = messages("dashboard.caption")

    val createSubmissionHeading: String = messages("dashboard.create-submission.heading")

    val createSubmissionStf: String = messages("dashboard.create-submission.stf")

    val tellUs: String = messages("dashboard.tell-us")

    val stf: String = messages("dashboard.stf")

    val sh03: String = messages("dashboard.sh03")

    val submissionsHeading: String = messages("dashboard.submissions.heading")

    val viewAllRecent: String = messages("dashboard.submissions.view-all-recent")

    val viewOverdue: String = messages("dashboard.submissions.view-overdue")

    val viewReadyToPay: String = messages("dashboard.submissions.view-ready-to-pay")

    val viewDrafts: String = messages("dashboard.submissions.view-drafts")
  }

  "The DashboardView" - {

    "when rendered for an Individual" - {

      val submissions = SubmissionsViewModel(overdueCount = 2, readyToPayCount = 3, draftCount = 4)

      val doc = view(submissions = submissions, isIndividual = true)

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
        doc
          .select(".design-system-card__heading")
          .get(0)
          .text() mustBe ExpectedContent.createSubmissionHeading
      }

      "show the create STF submission link" in {
        val link =
          doc
            .select(".design-system-card")
            .get(0)
            .select("a")
            .first()

        link.text() mustBe ExpectedContent.createSubmissionStf
        link.attr("href") mustBe routes.StartSubmissionController.startStf().url
      }

      "not show the non-individual content" in {
        doc.text() must not include ExpectedContent.tellUs
      }

      "not show the SH03 create submission link" in {
        doc
          .select(".design-system-card")
          .get(0)
          .select("a")
          .text() must not include ExpectedContent.sh03
      }

      "have the submissions heading" in {
        doc
          .select(".design-system-card__heading")
          .get(1)
          .text() mustBe ExpectedContent.submissionsHeading
      }

      "have the view all recent submissions link" in {
        doc
          .select(".design-system-card")
          .get(1)
          .select("a")
          .get(0)
          .text() mustBe ExpectedContent.viewAllRecent
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

    "when rendered for a non-individual" - {

      val submissions = SubmissionsViewModel(overdueCount = 2, readyToPayCount = 3, draftCount = 4)

      val doc = view(submissions = submissions, isIndividual = false)

      "show the tell us text" in {
        doc.text() must include(ExpectedContent.tellUs)
      }

      "show the STF link" in {
        val firstCard =
          doc.select(".design-system-card").get(0)

        val stfLink =
          firstCard
            .select("a")
            .get(0)

        stfLink.text() mustBe ExpectedContent.stf
        stfLink.attr("href") mustBe routes.StartSubmissionController.startStf().url
      }

      "show the SH03 link" in {
        val firstCard =
          doc.select(".design-system-card").get(0)

        val sh03Link =
          firstCard
            .select("a")
            .get(1)

        sh03Link.text() mustBe ExpectedContent.sh03
        sh03Link.attr("href") mustBe routes.StartSubmissionController.startSh03().url
      }

      "not show the individual create submission text" in {
        doc
          .select(".design-system-card")
          .get(0)
          .select("a")
          .text() must not include ExpectedContent.createSubmissionStf
      }
    }

    "when all submission counts are zero" - {

      val submissions =
        SubmissionsViewModel(
          overdueCount = 0,
          readyToPayCount = 0,
          draftCount = 0
        )

      val doc = view(
        submissions = submissions,
        isIndividual = true
      )

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
