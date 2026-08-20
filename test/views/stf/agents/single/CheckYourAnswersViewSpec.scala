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

package views.stf.agents.single

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.Text
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{Key, SummaryList, SummaryListRow, Value}
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.agents.single.routes as agentRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.shared.single.{CheckYourAnswersViewModel, SummarySection}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.agents.single.CheckYourAnswersView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class CheckYourAnswersViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = agentAffinity).build()

  private val viewInstance = app.injector.instanceOf[CheckYourAnswersView]

  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  private val summaryList =
    SummaryList(
      rows = Seq(
        SummaryListRow(
          key = Key(Text("Name")),
          value = Value(Text("John Smith"))
        )
      )
    )

  private val viewModel =
    CheckYourAnswersViewModel(
      sections = Seq.empty,
      taxDueFormatted = Some("£1,234.56"),
      paymentDueDateFormatted = Some("31 December 2026")
    )

  private val viewModelWithSections =
    CheckYourAnswersViewModel(
      sections = Seq(
        SummarySection(
          CheckYourAnswersViewModel.MessageKeys.sellerDetailsHeading,
          summaryList
        ),
        SummarySection(
          CheckYourAnswersViewModel.MessageKeys.transferDetailsHeading,
          summaryList
        )
      ),
      taxDueFormatted = Some("£1,234.56"),
      paymentDueDateFormatted = Some("31 December 2026")
    )

  def view(): Document = Jsoup.parse(viewInstance(viewModel, testBackLinkRoute)(fakeRequest, messages).body)

  def viewWithSections(): Document = Jsoup.parse(viewInstance(viewModelWithSections, testBackLinkRoute)(fakeRequest, messages).body)

  object ExpectedContent {

    val title: String = messages(CheckYourAnswersViewModel.MessageKeys.title)

    val heading: String = messages(CheckYourAnswersViewModel.MessageKeys.heading)

    val sellerDetailsHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.sellerDetailsHeading)

    val transferDetailsHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.transferDetailsHeading)

    val taxDueHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.taxDueHeading, "£1,234.56")

    val taxDueBody: String = messages(CheckYourAnswersViewModel.MessageKeys.taxDueBody)

    val paymentDueBy: String = messages(CheckYourAnswersViewModel.MessageKeys.paymentDueBy)

    val paymentDueDate: String = "31 December 2026"

    val printHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.printHeading)

    val printP1: String = messages("checkYourAnswers.print.p1")

    val printP2: String = messages("checkYourAnswers.print.content.link.text")

    val printP3: String = messages("checkYourAnswers.print.p3")

    val declarationHeading: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationHeading)

    val declarationP1: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationP1)

    val declarationBullet1: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationBullet1)

    val declarationBullet2: String = messages(CheckYourAnswersViewModel.MessageKeys.declarationBullet2)

    val acceptAndSend: String = messages(CheckYourAnswersViewModel.MessageKeys.acceptAndSend)

    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The CheckYourAnswersView" - {

    "should have the correct title" in {
      view().title must include(ExpectedContent.title)
    }

    "should have the correct heading" in {
      view().select("h1").text() mustBe ExpectedContent.heading
    }

    "should display the tax due heading" in {
      view().select(".tax-card h2").text() mustBe ExpectedContent.taxDueHeading
    }

    "should display the tax due body" in {
      view().select(".tax-card p").get(0).text() mustBe ExpectedContent.taxDueBody
    }

    "should display the payment due date" in {
      val paymentParagraph = view().select(".tax-card p").get(1)

      paymentParagraph.text() mustBe s"${ExpectedContent.paymentDueBy} ${ExpectedContent.paymentDueDate}"
    }

    "should display the print heading" in {
      view().select("h2").eachText().asScala must contain(ExpectedContent.printHeading)
    }


    "should display the print link" in {
      val printLink = view().select("#print-this-page")

      printLink.text() mustBe ExpectedContent.printP2
    }

    "should have the correct print link href" in {
      view().select("#print-this-page").attr("href") mustBe "#"
    }

    "should have the print link class" in {
      val printLink = view().select("#print-this-page")

      printLink.hasClass("govuk-link") mustBe true
      printLink.hasClass("govuk-link--no-visited-state") mustBe true
    }


    "should display the declaration heading" in {
      view().select("h2").eachText().asScala must contain(ExpectedContent.declarationHeading)
    }

    "should display the declaration paragraph" in {
      val declarationParagraph =
        view()
          .select(".govuk-list--bullet")
          .first()
          .parent()
          .select("p")
          .first()

      declarationParagraph.text() mustBe ExpectedContent.declarationP1
    }


    "should display the first declaration bullet" in {
      view().select(".govuk-list--bullet li").get(0).text() mustBe ExpectedContent.declarationBullet1
    }

    "should display the second declaration bullet" in {
      view().select(".govuk-list--bullet li").get(1).text() mustBe ExpectedContent.declarationBullet2
    }


    "should have an accept and send button" in {
      val buttons = view().select(".govuk-button")

      buttons.get(0).text() mustBe ExpectedContent.acceptAndSend
    }

    "should have a save and return button" in {
      view().select(".govuk-button-group a.govuk-link").text() mustBe ExpectedContent.saveAndReturn
    }

    "should have the correct save and return link" in {
      view().select(".govuk-button-group a.govuk-link").attr("href") mustBe sharedRoutes.SubmissionsDashboardController.onPageLoad().url
    }

    "should have the correct back link" in {
      view().select("a.govuk-back-link").attr("href") mustBe testBackLinkRoute.url
    }

    "should have a form" in {
      view().select("form").size() mustBe 1
    }

    "should submit to the check your answers endpoint" in {
      view().select("form").attr("action") mustBe agentRoutes.CheckYourAnswersController.onSubmit().url
    }


    "should render summary sections" in {
      val headings =
        viewWithSections()
          .select("h2")
          .eachText()
          .asScala

      headings must contain(ExpectedContent.sellerDetailsHeading)
      headings must contain(ExpectedContent.transferDetailsHeading)
    }

    "should render the summary list" in {
      viewWithSections().select(".govuk-summary-list").size() mustBe 2
    }

    "should render the summary list rows" in {
      viewWithSections().select(".govuk-summary-list__row").size() mustBe 2
    }

    "should render the summary list key" in {
      viewWithSections()
        .select(".govuk-summary-list__key")
        .eachText()
        .asScala must contain("Name")
    }

    "should render the summary list value" in {
      viewWithSections()
        .select(".govuk-summary-list__value")
        .eachText()
        .asScala must contain("John Smith")
    }
  }
}