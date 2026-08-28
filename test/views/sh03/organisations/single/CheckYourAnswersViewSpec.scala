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

package views.sh03.organisations.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.SummaryListViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.shared.single.CheckYourAnswersViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.single.CheckYourAnswersView
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes
import views.ViewBaseSpec

class CheckYourAnswersViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder().build()

  private val viewInstance = app.injector.instanceOf[CheckYourAnswersView]
  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val dummyViewModel = CheckYourAnswersViewModel.fromOrganisationSummaryLists(
    yourDetails = SummaryListViewModel(rows = Seq.empty),
    transferDetails = SummaryListViewModel(rows = Seq.empty),
    declarationDetails = SummaryListViewModel(rows = Seq.empty),
    taxDueFormatted = Some("£100.00"),
    paymentDueDateFormatted = Some("1 August 2026")
  )

  def view(viewModel: CheckYourAnswersViewModel = dummyViewModel): Document = Jsoup.parse(
    viewInstance(viewModel, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages(CheckYourAnswersViewModel.MessageKeys.title)
    val heading: String = messages(CheckYourAnswersViewModel.MessageKeys.heading)

    val taxDueHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.taxDueHeading, "£100.00")
    val taxDueBody: String = messages(CheckYourAnswersViewModel.MessageKeys.taxDueBody)
    val paymentDueByPrefix: String = messages(CheckYourAnswersViewModel.MessageKeys.paymentDueBy)
    val paymentDueDate: String = "1 August 2026"
    val defaultPaymentDueDate: String = "Not calculated"

    val declarationHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationHeading)
    val declarationOrgConfirm: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationOrgConfirm)

    val acceptAndSend: String = messages(CheckYourAnswersViewModel.MessageKeys.acceptAndSend)
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The CheckYourAnswersView for Organisations" - {

    val cyaView = view()

    "have the correct title" in {
      cyaView.title must include(ExpectedContent.title)
    }

    "have the correct heading" in {
      cyaView.select("h1").text() mustBe ExpectedContent.heading
    }

    "display the tax card" - {
      "with the correct formatted tax due amount in the heading" in {
        cyaView.select(".tax-card h2").text() mustBe ExpectedContent.taxDueHeading
      }

      "with the tax calculation explanation body" in {
        cyaView.select(".tax-card p.govuk-body").get(0).text() mustBe ExpectedContent.taxDueBody
      }

      "with the payment due date" in {
        val paymentDueText = cyaView.select(".tax-card p.govuk-body").get(1).text()
        paymentDueText must include(ExpectedContent.paymentDueByPrefix)
        paymentDueText must include(ExpectedContent.paymentDueDate)
      }

      "with fallback text when dates and amounts are not calculated" in {
        val emptyViewModel = dummyViewModel.copy(taxDueFormatted = None, paymentDueDateFormatted = None)
        val emptyView = view(emptyViewModel)

        val emptyTaxHeading = messages(CheckYourAnswersViewModel.MessageKeys.taxDueHeading, "£0.00")

        emptyView.select(".tax-card h2").text() mustBe emptyTaxHeading
        emptyView.select(".tax-card p.govuk-body").get(1).text() must include(ExpectedContent.defaultPaymentDueDate)
      }
    }

    "display the declaration section correctly" - {
      "with the correct heading" in {
        cyaView.select("h2:contains(" + ExpectedContent.declarationHeading + ")").size() mustBe 1
      }

      "with the confirmation body text specific to organisations" in {
        cyaView.select(s"p.govuk-body:contains(${ExpectedContent.declarationOrgConfirm})").size() mustBe 1
      }
    }

    "contain a form that submits to the correct route" in {
      val form = cyaView.select("form")
      form.attr("action") mustBe routes.CheckYourAnswersController.onSubmit().url
      form.attr("method") mustBe "POST"
    }

    "have an Accept and send button" in {
      val button = cyaView.select(".govuk-button").first()
      button.text() mustBe ExpectedContent.acceptAndSend
    }

    "have a Save and return to dashboard button" in {
      val saveAndReturnButton = cyaView.select(".govuk-button").get(1)
      saveAndReturnButton.text() mustBe ExpectedContent.saveAndReturn
    }
  }
}