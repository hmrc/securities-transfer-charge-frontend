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

package views.stf.agents.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.routes as sharedRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.{CheckYourAnswersViewModel, Transfer}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.CheckYourAnswersView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class CheckYourAnswersViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = individualAffinity).build()

  private val viewInstance = app.injector.instanceOf[CheckYourAnswersView]

  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  val fileName = "test-file.csv"

  private val transfers = Seq(
    Transfer(
      seller = "John Smith",
      securitiesBoughtIn = "ABC Shares",
      consideration = BigDecimal("10000.00"),
      taxDue = BigDecimal("50.00")
    ),
    Transfer(
      seller = "Jane Doe",
      securitiesBoughtIn = "XYZ Shares",
      consideration = BigDecimal("20000.00"),
      taxDue = BigDecimal("100.00")
    )
  )

  private val viewModel =
    CheckYourAnswersViewModel(
      fileName = fileName,
      numberOfTransfers = 2,
      taxDue = "£150.00",
      paymentDueBy = "31 December 2026",
      transfers = transfers
    )

  def view(): Document = Jsoup.parse(viewInstance(viewModel, testBackLinkRoute)(fakeRequest, messages).body)

  object ExpectedContent {

    val title: String = messages("checkYourAnswers.title")

    val heading: String = messages("checkYourAnswers.heading")

    val fileDetailsHeading: String = messages("checkYourAnswers.bulk.fileDetails.heading")

    val numberOfTransfers: String = "2"

    val taxDueHeading: String = messages("checkYourAnswers.bulk.taxDue.heading", "£150.00")

    val taxDueBodyP1: String = messages("checkYourAnswers.bulk.taxDue.body.p1")

    val taxDueBodyP2: String = messages("checkYourAnswers.bulk.taxDue.body.p2")

    val paymentDueBy: String = "31 December 2026"

    val sellerHeading: String = messages("checkYourAnswers.bulk.taxDue.section.seller.heading")

    val securitiesHeading: String = messages("checkYourAnswers.bulk.taxDue.section.securities.heading")

    val considerationHeading: String = messages("checkYourAnswers.bulk.taxDue.section.consideration.heading")

    val taxDueTableHeading: String = messages("checkYourAnswers.bulk.taxDue.section.taxDue.heading")

    val declarationHeading: String = messages("checkYourAnswers.bulk.declaration.heading")

    val declarationBody: String = messages("checkYourAnswers.bulk.declaration.body")

    val acceptAndSend: String = messages("checkYourAnswers.acceptAndSend")

    val returnToDashboard: String = messages("checkYourAnswers.returnToDashboard")
  }

  "The CheckYourAnswersView" - {

    "should have the correct title" in {
      view().title must include(ExpectedContent.title)
    }

    "should have the correct heading" in {
      view().select("h1").text() mustBe ExpectedContent.heading
    }

    "should have the file details heading" in {
      view()
        .select("h2")
        .eachText()
        .asScala must contain(ExpectedContent.fileDetailsHeading)
    }

    "should display the file name" in {
      view()
        .select(".govuk-summary-list__value")
        .get(0)
        .text() mustBe fileName
    }

    "should display the number of transfers" in {
      view()
        .select(".govuk-summary-list__value")
        .get(1)
        .text() mustBe ExpectedContent.numberOfTransfers
    }

    "should display the tax due heading" in {
      view()
        .select(".tax-card h2")
        .text() mustBe ExpectedContent.taxDueHeading
    }

    "should display the first tax due paragraph" in {
      view()
        .select(".tax-card p")
        .get(0)
        .text() mustBe ExpectedContent.taxDueBodyP1
    }

    "should display the payment due paragraph" in {
      val paymentParagraph =
        view()
          .select(".tax-card p")
          .get(1)

      paymentParagraph.text() mustBe
        s"${ExpectedContent.taxDueBodyP2} ${ExpectedContent.paymentDueBy}"
    }

    "should display the transfer table" in {
      view()
        .select(".govuk-table")
        .size() mustBe 1
    }

    "should display the correct table headings" in {
      view()
        .select(".govuk-table thead th")
        .eachText()
        .asScala must contain theSameElementsInOrderAs Seq(
        ExpectedContent.sellerHeading,
        ExpectedContent.securitiesHeading,
        ExpectedContent.considerationHeading,
        ExpectedContent.taxDueTableHeading
      )
    }

    "should display all transfer rows" in {
      view()
        .select(".govuk-table tbody tr")
        .size() mustBe transfers.size
    }

    "should display the first transfer correctly" in {
      val row =
        view()
          .select(".govuk-table tbody tr")
          .get(0)

      row.select("td").get(0).text() mustBe "John Smith"
      row.select("td").get(1).text() mustBe "ABC Shares"
      row.select("td").get(2).text() mustBe "£10,000.00"
      row.select("td").get(3).text() mustBe "£50.00"
    }

    "should display the second transfer correctly" in {
      val row =
        view()
          .select(".govuk-table tbody tr")
          .get(1)

      row.select("td").get(0).text() mustBe "Jane Doe"
      row.select("td").get(1).text() mustBe "XYZ Shares"
      row.select("td").get(2).text() mustBe "£20,000.00"
      row.select("td").get(3).text() mustBe "£100.00"
    }

    "should have the declaration heading" in {
      view()
        .select("h2")
        .eachText()
        .asScala must contain(ExpectedContent.declarationHeading)
    }

    "should display the declaration paragraph" in {
      view()
        .select(".tax-card")
        .next()
        .next()
        .text() must include(ExpectedContent.declarationBody)
    }

    "should have an accept and send button" in {
      view()
        .select(".govuk-button")
        .text() mustBe ExpectedContent.acceptAndSend
    }

    "should have a return to dashboard link" in {
      view()
        .select(".govuk-button-group a.govuk-link")
        .text() mustBe ExpectedContent.returnToDashboard
    }

    "should have the correct return to dashboard link" in {
      view()
        .select(".govuk-button-group a.govuk-link")
        .attr("href") mustBe
        sharedRoutes.SubmissionsDashboardController.onPageLoad().url
    }

    "should have the correct back link" in {
      view()
        .select("a.govuk-back-link")
        .attr("href") mustBe testBackLinkRoute.url
    }

    "should have a form" in {
      view()
        .select("form")
        .size() mustBe 1
    }

    "should submit to the check your answers endpoint" in {
      view()
        .select("form")
        .attr("action") mustBe
        routes.CheckYourAnswersController.onSubmit().url
    }

    "should have the correct summary list" in {
      view()
        .select(".govuk-summary-list")
        .size() mustBe 1
    }

    "should have the correct summary list rows" in {
      view()
        .select(".govuk-summary-list__row")
        .size() mustBe 2
    }
  }
}
