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

package views.sh03.organisations.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.shared.bulk.routes.YourFileWillNotBeSavedController
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations.bulk.{CheckYourAnswersViewModel, Sh03Transfer}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.organisations.bulk.CheckYourAnswersView
import views.ViewBaseSpec

import scala.jdk.CollectionConverters.*

class CheckYourAnswersViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application =
    applicationBuilder(affinityGroup = orgAffinity).build()

  private val viewInstance = app.injector.instanceOf[CheckYourAnswersView]

  private val testBackLinkRoute: Call = Call("GET", "/back-link")

  val fileName = "test-file.csv"

  private val transfers = Seq(
    Sh03Transfer(
      amountOfShares = "10,000",
      reasonFor = "Cancellation",
      consideration = BigDecimal("10000.00"),
      taxDue = BigDecimal("50.00")
    ),
    Sh03Transfer(
      amountOfShares = "20,000",
      reasonFor = "Treasury",
      consideration = BigDecimal("20000.00"),
      taxDue = BigDecimal("100.00")
    )
  )

  private val viewModel =
    CheckYourAnswersViewModel(
      companyDetailsRows = Seq.empty,
      fileName = fileName,
      numberOfTransfers = 2,
      taxDue = "£150.00",
      paymentDueBy = "31 December 2026",
      transfers = transfers,
      declarationRows = Seq.empty
    )

  def view(): Document = Jsoup.parse(viewInstance(viewModel, testBackLinkRoute)(fakeRequest, messages).body)

  object ExpectedContent {

    val title: String = messages("checkYourAnswers.title")
    val sectionTitle: String = messages("sectionTitle.sh03")

    val mainHeading: String = messages("checkYourAnswers.heading")
    val yourDetailsHeading: String = messages("checkYourAnswers.yourDetails.heading")
    val fileDetailsHeading: String = messages("checkYourAnswers.bulk.fileDetails.heading")

    val numberOfTransfers: String = "2"

    val taxDueHeading: String = messages("checkYourAnswers.bulk.taxDue.heading", "£150.00")
    val taxDueBodyP1: String = messages("checkYourAnswers.bulk.taxDue.body.p1")
    val taxDueBodyP2: String = messages("checkYourAnswers.bulk.taxDue.body.p2")
    val paymentDueBy: String = "31 December 2026"

    val amountHeading: String = messages("checkYourAnswers.sh03.bulk.taxDue.section.amount.heading")
    val forHeading: String = messages("checkYourAnswers.sh03.bulk.taxDue.section.for.heading")
    val considerationHeading: String = messages("checkYourAnswers.sh03.bulk.taxDue.section.consideration.heading")
    val taxDueTableHeading: String = messages("checkYourAnswers.sh03.bulk.taxDue.section.taxDue.heading")

    val declarationHeading: String = messages("checkYourAnswers.bulk.declaration.heading")
    val declarationBody: String = messages("checkYourAnswers.bulk.declaration.body")

    val acceptAndSend: String = messages("checkYourAnswers.acceptAndSend")
    val saveAndReturn: String = messages("site.save-and-return.button")
  }

  "The CheckYourAnswersView" - {

    "should have the correct title" in {
      view().title must include(ExpectedContent.title)
      view().title must include(ExpectedContent.sectionTitle)
    }

    "should have the correct main heading" in {
      view().select("h1.govuk-heading-l").first().text() mustBe ExpectedContent.mainHeading
    }

    "should have the your details heading" in {
      view().select("h2").eachText().asScala must contain(ExpectedContent.yourDetailsHeading)
    }

    "should have the file details heading" in {
      view().select("h2").eachText().asScala must contain(ExpectedContent.fileDetailsHeading)
    }

    "should display the file name in the file details summary card" in {
      view()
        .select(".govuk-summary-list")
        .get(1)
        .select(".govuk-summary-list__value")
        .get(0)
        .text() mustBe fileName
    }

    "should display the number of transfers in the file details summary card" in {
      view()
        .select(".govuk-summary-list")
        .get(1)
        .select(".govuk-summary-list__value")
        .get(1)
        .text() mustBe ExpectedContent.numberOfTransfers
    }

    "should display the tax due heading in the tax card" in {
      view().select(".tax-card h1").text() mustBe ExpectedContent.taxDueHeading
    }

    "should display the first tax due paragraph" in {
      view().select(".tax-card p").get(0).text() mustBe ExpectedContent.taxDueBodyP1
    }

    "should display the payment due paragraph" in {
      val paymentParagraph = view().select(".tax-card p").get(1)
      paymentParagraph.text() mustBe s"${ExpectedContent.taxDueBodyP2} ${ExpectedContent.paymentDueBy}"
    }

    "should display the transfer table" in {
      view().select(".govuk-table").size() mustBe 1
    }

    "should display the correct table headings" in {
      view()
        .select(".govuk-table thead th")
        .eachText()
        .asScala must contain theSameElementsInOrderAs Seq(
        ExpectedContent.amountHeading,
        ExpectedContent.forHeading,
        ExpectedContent.considerationHeading,
        ExpectedContent.taxDueTableHeading
      )
    }

    "should display all transfer rows" in {
      view().select(".govuk-table tbody tr").size() mustBe transfers.size
    }

    "should display the first transfer row correctly" in {
      val row = view().select(".govuk-table tbody tr").get(0)

      row.select("td").get(0).text() mustBe "10,000"
      row.select("td").get(1).text() mustBe "Cancellation"
      row.select("td").get(2).text() mustBe "£10,000.00"
      row.select("td").get(3).text() mustBe "£50.00"
    }

    "should display the second transfer row correctly" in {
      val row = view().select(".govuk-table tbody tr").get(1)

      row.select("td").get(0).text() mustBe "20,000"
      row.select("td").get(1).text() mustBe "Treasury"
      row.select("td").get(2).text() mustBe "£20,000.00"
      row.select("td").get(3).text() mustBe "£100.00"
    }

    "should have the declaration heading" in {
      view().select("h2").eachText().asScala must contain(ExpectedContent.declarationHeading)
    }

    "should display the declaration paragraph" in {
      view().select(".govuk-body").eachText().asScala.exists(_.contains(ExpectedContent.declarationBody)) mustBe true
    }

    "should render three summary lists (Company Details, File Details, Declaration)" in {
      view().select(".govuk-summary-list").size() mustBe 3
    }

    "should have an accept and send button" in {
      view().select(".govuk-button").get(0).text() mustBe ExpectedContent.acceptAndSend
    }

    "should have a save and return secondary button" in {
      val saveAndReturnButton = view().select(".govuk-button").get(1)

      saveAndReturnButton.text() mustBe ExpectedContent.saveAndReturn
      saveAndReturnButton.hasClass("govuk-button--secondary") mustBe true
    }

    "should have the correct save and return href" in {
      view().select(".govuk-button").get(1).attr("href") mustBe YourFileWillNotBeSavedController.onPageLoad().url
    }

    "should have the correct back link" in {
      view().select("a.govuk-back-link").attr("href") mustBe testBackLinkRoute.url
    }

    "should submit to the correct check your answers endpoint" in {
      view().select("form").attr("action") mustBe routes.CheckYourAnswersController.onSubmit().url
    }
  }
}