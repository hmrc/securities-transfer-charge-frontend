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

package services.sh03.agents.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.{Messages, Lang}
import play.api.test.Helpers.stubMessagesApi
import uk.gov.hmrc.govukfrontend.views.Aliases.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.bulk.Sh03AgentRowBuilder
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedValue.Valid
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument}
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.agents.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.TaxDueCalculationService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.agents.bulk.{CheckYourAnswersViewModel, TransferRow}

import java.time.LocalDate

class CheckYourAnswersServiceSpec extends SpecBase with FileUploadFixtures {

  implicit val messages: Messages = stubMessagesApi().preferred(Seq(Lang("en")))

  private val taxDueCalculationService = mock[TaxDueCalculationService]
  val chargingPoint: Valid[LocalDate] = Valid(LocalDate.of(2025, 5, 16))
  val taxDueDate: LocalDate = chargingPoint.value.plusDays(30)

  private val service = new CheckYourAnswersService(taxDueCalculationService)
  val row1: ParsedStcRow = parsedStcRow(1).copy(
    sharePurchaseReason = Some("Treasury"),
    chargingPoint = chargingPoint,
    sellerName = Some("Seller 1"),
    securitiesQuantity = Some("10000"),
    securitiesTarget = Some("Company 1"),
    amountPaidForSecurities = Some("10000"))

  val row2: ParsedStcRow = parsedStcRow(2).copy(
    sharePurchaseReason = Some("Cancellation"),
    chargingPoint = chargingPoint,
    sellerName = Some("Seller 2"),
    securitiesQuantity = Some("50000"),
    securitiesTarget = Some("Company 2"),
    amountPaidForSecurities = Some("20000"),
  )

  "CheckYourAnswersService.buildViewModel" - {

    "build the view model with the expected transfer details" in {

      val document = ParsedStcRowsDocument(_id = "SomeRef", fileName = "test.csv", rows = Seq(row1, row2))
      val yourDetailsList = SummaryList(rows = Sh03AgentRowBuilder.buildYourDetailsRows(emptyUserAnswers))
      val buyerDetailsList = SummaryList(rows = Sh03AgentRowBuilder.buildBuyerDetailsRows(emptyUserAnswers))
      val fileDetailsCard = Sh03AgentRowBuilder.buildFileDetailsCard(document.fileName, document.rows.size)
      val declarationList = SummaryList(rows = Sh03AgentRowBuilder.buildDeclarationRows(emptyUserAnswers))

      val summaryLists = Seq(yourDetailsList, buyerDetailsList, fileDetailsCard)

      val transferRows = Seq(
        TransferRow(amount = BigDecimal(10000), reason = "Treasury", consideration = BigDecimal(10000), taxDue = BigDecimal(50)),
        TransferRow(amount = BigDecimal(50000), reason = "Cancellation", consideration = BigDecimal(20000), taxDue = BigDecimal(100)),
      )

      when(taxDueCalculationService.buildTransferRows(document.rows))
        .thenReturn(transferRows)

      when(taxDueCalculationService.formatCurrency(BigDecimal(150)))
        .thenReturn("£150.00")

      when(taxDueCalculationService.calculatePaymentDueDate(document.rows))
        .thenReturn(taxDueDate)

      when(taxDueCalculationService.formatDate(taxDueDate))
        .thenReturn("15 June 2025")

      val result = service.buildViewModel(emptyUserAnswers, document)

      result shouldBe CheckYourAnswersViewModel.agentBulkSummaryLists(
        summaryLists, transferRows, "£150.00", "15 June 2025", declarationList)
    }

    "correctly sum tax due across multiple rows before formatting" in {

      val document = ParsedStcRowsDocument(_id = "Ref", fileName = "test.csv", rows = Seq(row1, row2))

      val transferRows = Seq(
        TransferRow(amount = BigDecimal(10000), reason = "Treasury", consideration = BigDecimal(10000), taxDue = BigDecimal(12.50)),
        TransferRow(amount = BigDecimal(50000), reason = "Cancellation", consideration = BigDecimal(20000), taxDue = BigDecimal(25.75)),
      )

      when(taxDueCalculationService.buildTransferRows(document.rows)).thenReturn(transferRows)
      when(taxDueCalculationService.formatCurrency(BigDecimal("38.25"))).thenReturn("£38.25")
      when(taxDueCalculationService.calculatePaymentDueDate(document.rows)).thenReturn(taxDueDate)
      when(taxDueCalculationService.formatDate(taxDueDate)).thenReturn("15 June 2025")

      val result = service.buildViewModel(emptyUserAnswers, document)

      result.taxDueFormatted shouldBe "£38.25"
    }
  }
}
