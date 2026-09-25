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
import play.api.Application
import uk.gov.hmrc.securitiestransferchargefrontend.models.ConfirmationViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.ConfirmationView
import views.ViewBaseSpec

class ConfirmationViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder().build()

  private val viewInstance =
    app.injector.instanceOf[ConfirmationView]

  private val viewModel = ConfirmationViewModel(
    submissionId = "STC-123456789",
    reference = None,
    taxDue = "£500.00",
    paymentDueBy = "30 October 2026",
    isAgent = false
  )

  private val agentViewModel = ConfirmationViewModel(
    submissionId = "STC-123456789",
    reference = Some("CLIENT-123"),
    taxDue = "£500.00",
    paymentDueBy = "30 October 2026",
    isAgent = true
  )

  def view(viewModel: ConfirmationViewModel = viewModel): Document =
    Jsoup.parse(viewInstance(viewModel)(fakeRequest, messages).body)

  object ExpectedContent {
    val title: String = messages("confirmation.title")
    val bannerTitle: String = messages("confirmation.banner.title")
    val nextStepsHeading: String = messages("confirmation.heading.nextSteps")
    val transferReferenceHeading: String = messages("confirmation.heading.transferReference")
    val transferReferenceLink: String = messages("confirmation.heading.transferReference.link")
    val deadlineHeading: String = messages("confirmation.heading.deadline")
    val deadlineText: String = messages("confirmation.heading.deadline.p1")

    val submissionIdHeading: String = messages("confirmation.table.heading.submissionId")

    val referenceHeading: String = messages("confirmation.table.heading.reference")

    val taxDueHeading: String = messages("confirmation.table.heading.taxDue")

    val payButton: String = messages("confirmation.button.pay")

    val returnToDashboard: String = messages("return-to-dashboard.link")
  }

  "The ConfirmationView" - {

    "render view" - {

      val confirmationView = view()

      "have the correct title" in {
        confirmationView.title() must include(ExpectedContent.title)
      }

      "display the notification banner" in {
        confirmationView
          .select(".govuk-notification-banner")
          .text() must include(ExpectedContent.bannerTitle)
      }

      "display the payment due date in the notification banner" in {
        confirmationView
          .select(".govuk-notification-banner")
          .text() must include(viewModel.paymentDueBy)
      }

      "have the correct next steps heading" in {
        confirmationView.select("h1").text() mustBe
          ExpectedContent.nextStepsHeading
      }

      "have the transfer reference heading" in {
        confirmationView
          .select("h2")
          .get(1)
          .text() mustBe ExpectedContent.transferReferenceHeading
      }

      "have the transfer reference dashboard link" in {
        val link = confirmationView
          .select("a.govuk-link")
          .stream()
          .filter(_.text() == ExpectedContent.transferReferenceLink)
          .findFirst()
          .orElseThrow()

        link.attr("href") mustBe
          uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes
            .SubmissionsDashboardController
            .onPageLoad()
            .url
      }

      "have the deadline heading" in {
        confirmationView
          .select("h2")
          .get(2)
          .text() mustBe ExpectedContent.deadlineHeading
      }

      "display the deadline text" in {
        confirmationView.text() must include(ExpectedContent.deadlineText)
      }

      "display the submission ID" in {
        confirmationView.text() must include(ExpectedContent.submissionIdHeading)
        confirmationView.text() must include(viewModel.submissionId)
      }

      "not display the reference row" in {
        confirmationView.text() must not include ExpectedContent.referenceHeading
      }

      "display the tax due" in {
        confirmationView.text() must include(ExpectedContent.taxDueHeading)
        confirmationView.text() must include(viewModel.taxDue)
      }

      "have the pay button" in {
        confirmationView
          .select(".govuk-button")
          .text() mustBe ExpectedContent.payButton
      }

      "have the return to dashboard link" in {
        confirmationView.text() must include(ExpectedContent.returnToDashboard)
      }
    }

    "for an agent" - {

      val confirmationView = view(agentViewModel)

      "display the reference row" in {
        confirmationView.text() must include(ExpectedContent.referenceHeading)
        confirmationView.text() must include("CLIENT-123")
      }

      "display not provided when the reference is empty" in {
        val viewModel = agentViewModel.copy(reference = None)

        val confirmationView = view(viewModel)

        confirmationView.text() must include(messages("site.notProvided"))
      }

      "display not provided when the reference is an empty string" in {
        val viewModel = agentViewModel.copy(reference = Some(""))

        val confirmationView = view(viewModel)

        confirmationView.text() must include(messages("site.notProvided"))
      }
    }
  }
}