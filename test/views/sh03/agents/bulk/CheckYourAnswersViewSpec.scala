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

package views.sh03.agents.bulk

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.mvc.Call
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.bulk.Sh03AgentRowBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.bulk.{CheckYourAnswersViewModel, TransferRow}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.sh03.agents.bulk.CheckYourAnswersView
import views.ViewBaseSpec

import scala.language.postfixOps

class CheckYourAnswersViewSpec extends ViewBaseSpec {

  override def fakeApplication(): Application = applicationBuilder(affinityGroup = agentAffinity).build()
  
  private val viewInstance         = app.injector.instanceOf[CheckYourAnswersView]
  private val fileName = "testFile.csv"
  private val noOfRows = 4
  private val testBackLinkRoute: Call = Call("GET", "/back-link")
  private val yourDetailsList = SummaryList(rows = Sh03AgentRowBuilder.buildYourDetailsRows(emptyUserAnswers))
  private val buyerDetailsList = SummaryList(Sh03AgentRowBuilder.buildBuyerDetailsRows(emptyUserAnswers))
  private val fileDetailsCard = Sh03AgentRowBuilder.buildFileDetailsCard(fileName, noOfRows)
  private val declarationList = SummaryList(Sh03AgentRowBuilder.buildDeclarationRows(emptyUserAnswers))
  private val summaryLists = Seq(yourDetailsList, buyerDetailsList, fileDetailsCard)
  private val transferRows = Seq(TransferRow(BigDecimal(1000), "Cancellation", BigDecimal(100.00), BigDecimal(100.00)), TransferRow(BigDecimal(1000), "Treasury", BigDecimal(100.00), BigDecimal(100.00)), TransferRow(BigDecimal(3000), "Cancellation", BigDecimal(100.00), BigDecimal(100.00)), TransferRow(BigDecimal(5000), "Treasury", BigDecimal(100.00), BigDecimal(100.00)))
  private val viewModel = CheckYourAnswersViewModel.agentBulkSummaryLists(
    summaryLists = summaryLists, taxDueSummaryRows = transferRows, totalTaxDue = "£0.00", taxDueDate = "23 September 2026", declarationList)




  def view(): Document = Jsoup.parse(
    viewInstance(viewModel, testBackLinkRoute)(fakeRequest, messages).body
  )

  object ExpectedContent {
    val title: String = messages("checkYourAnswers.title")
    val heading: String = messages("checkYourAnswers.heading")
    val printHeading: String = messages("checkYourAnswers.print.heading")
    val printLink: String = messages("checkYourAnswers.print.content.link.text")
    val returnLink: String = messages("checkYourAnswers.saveAndReturn")
    val acceptAndSend: String = messages("checkYourAnswers.acceptAndSend")
    val taxDue: String = s"Tax due: ${viewModel.taxDueFormatted}"
    val fileDetailsHeading: String = messages("agent.checkYourAnswers.fileDetails.heading")
  }

  "The CheckYourAnswersView must" - {

    val checkYourAnswersView = view()

    "have the correct title" in {
      checkYourAnswersView.title must include(ExpectedContent.title)
    }

    "have the correct heading" in {
      checkYourAnswersView.select("h1").text() mustBe ExpectedContent.heading
    }

    "have file details displayed" in {
      val summaryLists = checkYourAnswersView.select(".govuk-summary-list")
      summaryLists.text() must include(ExpectedContent.fileDetailsHeading)
      summaryLists.text() must include(fileName)
      summaryLists.text() must include(noOfRows.toString)
    }

    "have a tax card with amount of tax due displayed" in {
      val taxCard = checkYourAnswersView.select(".tax-card")
      taxCard.select("h2").text() mustBe ExpectedContent.taxDue
    }

    "have an accept and send button" in {
      val buttons = checkYourAnswersView.select(".govuk-button")
      buttons.get(0).text() mustBe ExpectedContent.acceptAndSend
    }

    "have a button to save and return to the submission dashboard page" in {
      val returnLink = checkYourAnswersView.select(".govuk-button-group a.govuk-button--secondary").first()
      returnLink.text() mustBe ExpectedContent.returnLink
    }
  }
}
