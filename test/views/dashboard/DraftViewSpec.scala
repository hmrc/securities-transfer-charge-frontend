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

import base.Fixtures.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as stfSharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.shared.AgentReference
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.{AgentReferencePage, CompanyDetailsPage}
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.{DraftDeletion, DraftViewModel}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DraftView
import views.ViewBaseSpec

import java.time.{Clock, LocalTime}
import scala.jdk.CollectionConverters.*

class DraftViewSpec extends ViewBaseSpec {

  private val viewInstance = app.injector.instanceOf[DraftView]

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val companyDetails = CompanyDetails("Test Company Ltd", "12345678", isPlc = true)

  private val sh03Answers: UserAnswers = testUserAnswers.copy(journeyType = SH03)

  private val answersWithCompany: UserAnswers =
    sh03Answers.set(CompanyDetailsPage, companyDetails).success.value

  private val answersWithAgentReference: UserAnswers =
    sh03Answers.set(AgentReferencePage, AgentReference(Some("REF-12345"))).success.value

  private def viewModel(
                         userAnswers: UserAnswers = answersWithCompany,
                         affinityKey: String = affinityGroupKeyOrg,
                         deletion: DraftDeletion = DraftDeletion.InDays(12)
                       ): DraftViewModel =
    DraftViewModel
      .build(userAnswers, affinityKey, Clock.systemUTC(), draftTtlDays = 30)
      .copy(deletion = deletion)

  def view(model: DraftViewModel): Document = Jsoup.parse(
    viewInstance(model)(fakeRequest, messages).body
  )

  class ExpectedContent(model: DraftViewModel) {
    val title: String = s"${messages("draft.title", model.submissionId)} - ${messages("reportAndPay.title")} - ${messages("site.govuk")}"
    val dashboardBreadcrumb: String = messages("breadcrumbs.dashboard")
    val submissionsBreadcrumb: String = messages("breadcrumbs.submissions")
    val bannerTitle: String = messages("confirmation.banner.title")
    val deletionMessage: String = model.deletion.message
    val submissionHeading: String = messages("draft.submission", model.submissionId)
    val buyerDetailsHeading: String = messages("draft.buyerDetails.heading")
    val continueSubmission: String = messages("draft.continueSubmission")
    val deleteDraft: String = messages("draft.deleteDraft")
  }

  "The DraftView with company details answered should" - {
    val model = viewModel()
    val expected = ExpectedContent(model)
    val doc = view(model)
    val breadcrumbs = doc.select(".govuk-breadcrumbs__list-item")
    val buttons = doc.select(".govuk-button-group .govuk-button")

    "have the correct title" in {
      doc.title() mustBe expected.title
    }

    "not have a back link" in {
      doc.select(".govuk-back-link").size() mustBe 0
    }

    "have two breadcrumb items" in {
      breadcrumbs.size() mustBe 2
    }

    "have the dashboard breadcrumb as a link" in {
      val link = breadcrumbs.get(0).select("a.govuk-breadcrumbs__link")
      link.text() mustBe expected.dashboardBreadcrumb
      link.attr("href") mustBe routes.DashboardController.onPageLoad().url
    }

    "have the submissions breadcrumb as a link" in {
      val link = breadcrumbs.get(1).select("a.govuk-breadcrumbs__link")
      link.text() mustBe expected.submissionsBreadcrumb
      link.attr("href") mustBe routes.SubmissionsController.onPageLoad().url
    }

    "have a notification banner with the correct title" in {
      doc.select(".govuk-notification-banner__title").text() mustBe expected.bannerTitle
    }

    "have the deletion message in the notification banner" in {
      doc.select(".govuk-notification-banner__content").text() mustBe expected.deletionMessage
    }

    "put the heading class on the banner message, not the banner" in {
      doc.select(".govuk-notification-banner").hasClass("govuk-notification-banner__heading") mustBe false
      doc.select(".govuk-notification-banner__content p.govuk-notification-banner__heading").text() mustBe expected.deletionMessage
    }

    "have the submission ID as the only h1" in {
      val h1s = doc.select("h1")
      h1s.size() mustBe 1
      h1s.first().text() mustBe expected.submissionHeading
      h1s.first().hasClass("govuk-heading-l") mustBe true
    }

    "only show the sections that have answers" in {
      val headings = doc.select("h2.govuk-heading-m").asScala.map(_.text()).toList
      headings mustBe List(expected.buyerDetailsHeading)
      doc.select(".govuk-summary-list").size() mustBe 1
    }

    "show the company details in the buyer details summary list" in {
      val rows = doc.select(".govuk-summary-list__row")
      rows.size() mustBe 3

      rows.get(0).select(".govuk-summary-list__key").text() mustBe messages("org.sh03.companyDetails.companyName.label")
      rows.get(0).select(".govuk-summary-list__value").text() mustBe companyDetails.companyName

      rows.get(1).select(".govuk-summary-list__key").text() mustBe messages("org.sh03.companyDetails.crn.label")
      rows.get(1).select(".govuk-summary-list__value").text() mustBe companyDetails.companyRegistrationNumber

      rows.get(2).select(".govuk-summary-list__key").text() mustBe messages("org.sh03.companyDetails.isPlc.label")
      rows.get(2).select(".govuk-summary-list__value").text() mustBe messages("site.yes")
    }

    "have two buttons" in {
      buttons.size() mustBe 2
    }

    "have a continue submission button linking to the correct destination" in {
      buttons.get(0).text() mustBe expected.continueSubmission
      buttons.get(0).attr("href") mustBe stfSharedRoutes.SaveAndReturnController.restore(model.submissionId).url
      buttons.get(0).hasClass("govuk-button--secondary") mustBe false
    }

    "have a secondary delete draft button linking to the correct destination" in {
      buttons.get(1).text() mustBe expected.deleteDraft
      buttons.get(1).attr("href") mustBe routes.DraftController.onDeleteDraft(model.submissionId).url
      buttons.get(1).hasClass("govuk-button--secondary") mustBe true
    }
  }

  "The DraftView for an agent" - {
    val model = viewModel(userAnswers = sh03Answers, affinityKey = affinityGroupKeyAgent)
    val doc = view(model)

    "show the your details section" in {
      val headings = doc.select("h2.govuk-heading-m").asScala.map(_.text()).toList
      headings must contain(messages("draft.yourDetails.heading"))
    }

    "show the agent reference as not provided" in {
      val rows = doc.select(".govuk-summary-list__row")
      rows.size() mustBe 1
      rows.get(0).select(".govuk-summary-list__key").text() mustBe messages("agentReference.checkYourAnswersLabel")
      rows.get(0).select(".govuk-summary-list__value").text() mustBe messages("site.notProvided")
    }

    "show the agent reference " in {
      val model = viewModel(userAnswers = answersWithAgentReference, affinityKey = affinityGroupKeyAgent)
      val doc = view(model)
      val rows = doc.select(".govuk-summary-list__row")
      rows.size() mustBe 1
      rows.get(0).select(".govuk-summary-list__key").text() mustBe messages("agentReference.checkYourAnswersLabel")
      rows.get(0).select(".govuk-summary-list__value").text() mustBe messages("REF-12345")
    }
  }

  "The DraftView deletion banner should" - {

    Seq(
      "in a number of days" -> (DraftDeletion.InDays(12), messages("draft.deletion.p1", 12)),
      "tomorrow" -> (DraftDeletion.Tomorrow(LocalTime.of(9, 0)), messages("draft.deletion.tomorrow", "9:00am")),
      "today" -> (DraftDeletion.Today(LocalTime.of(14, 30)), messages("draft.deletion.today", "2:30pm"))
    ).foreach { case (when, (deletion, expectedMessage)) =>

      s"show the deletion message when the draft will be deleted $when" in {
        val doc = view(viewModel(deletion = deletion))
        doc.select(".govuk-notification-banner__content").text() mustBe expectedMessage
      }
    }
  }
}
