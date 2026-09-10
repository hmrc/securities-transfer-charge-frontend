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

package services.sh03.organisations.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentMatchers.eq => eqTo
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Lang
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedValue.Valid
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument}
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.bulk.TaxDueCalculationService
import uk.gov.hmrc.securitiestransferchargefrontend.services.sh03.organisations.bulk.CheckYourAnswersService
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations.bulk.{CheckYourAnswersViewModel, CompanyDetailsSummary, RoleAtPurchasingCompanySummary, Sh03Transfer}

import java.time.LocalDate

class CheckYourAnswersServiceSpec extends SpecBase with FileUploadFixtures {

  implicit val lang: Lang = Lang("en")

  private val taxDueCalculationService = mock[TaxDueCalculationService]
  private val formattingService = mock[FormattingService]
  private val service = new CheckYourAnswersService(taxDueCalculationService, formattingService)

  val chargingPoint: Valid[LocalDate] = Valid(LocalDate.of(2026, 5, 16))

  val row1: ParsedStcRow = parsedStcRow(1).copy(
    chargingPoint = chargingPoint,
    securitiesQuantity = Some("10000"),
    typeOfShares = Some("Ordinary"),
    purchaseForCancellation = Some(true),
    amountPaidForSecurities = Some("15000.00")
  )

  val row2: ParsedStcRow = parsedStcRow(2).copy(
    chargingPoint = chargingPoint,
    securitiesQuantity = Some("5000"),
    whatTypeOfSecurities = Some("Preference"),
    typeOfShares = None,
    purchaseForCancellation = Some(false),
    amountPaidForSecurities = None,
    totalMarketValue = Some("25000.00")
  )

  "CheckYourAnswersService.buildViewModel" - {

    "build the view model with the expected transfer details and fallbacks" in {

      val document = ParsedStcRowsDocument(_id = "SomeRef", fileName = "test.csv", rows = Seq(row1, row2))

      when(taxDueCalculationService.calculateTaxDue(row1))
        .thenReturn(BigDecimal("75.00"))

      when(taxDueCalculationService.calculateTaxDue(row2))
        .thenReturn(BigDecimal("125.00"))

      when(taxDueCalculationService.formatCurrency(BigDecimal("200.00")))
        .thenReturn("£200.00")

      when(formattingService.formatPaymentDueDate(any[LocalDate])(any()))
        .thenReturn("15 June 2026")

      val result = service.buildViewModel(document, emptyUserAnswers)(lang, messages(applicationBuilder().build()))

      result shouldBe CheckYourAnswersViewModel(
        companyDetailsRows = CompanyDetailsSummary.rows(emptyUserAnswers)(messages(applicationBuilder().build())),
        fileName = "test.csv",
        numberOfTransfers = 2,
        taxDue = "£200.00",
        paymentDueBy = "15 June 2026",
        transfers = Seq(
          Sh03Transfer(
            amountOfShares = "10,000",
            reasonFor = "Cancellation",
            consideration = BigDecimal("15000.00"),
            taxDue = BigDecimal("75.00")
          ),
          Sh03Transfer(
            amountOfShares = "5,000",
            reasonFor = "Treasury",
            consideration = BigDecimal("25000.00"),
            taxDue = BigDecimal("125.00")
          )
        ),
        declarationRows = RoleAtPurchasingCompanySummary.rows(emptyUserAnswers)(messages(applicationBuilder().build()))
      )
    }

    "use the earliest charging point date and add 30 days to determine the payment due date" in {

      val earlierChargingPoint = Valid(LocalDate.of(2026, 1, 1))
      val rowEarliest = row1.copy(chargingPoint = earlierChargingPoint)

      val document = ParsedStcRowsDocument(
        _id = "Ref",
        fileName = "test.csv",
        rows = Seq(rowEarliest, row2)
      )

      when(taxDueCalculationService.calculateTaxDue(any()))
        .thenReturn(BigDecimal("10.00"))

      when(taxDueCalculationService.formatCurrency(any()))
        .thenReturn("£20.00")

      when(formattingService.formatPaymentDueDate(eqTo(LocalDate.of(2026, 1, 31)))(any()))
        .thenReturn("31 January 2026")
      
      val result = service.buildViewModel(document, emptyUserAnswers)(lang, messages(applicationBuilder().build()))

      result.paymentDueBy shouldBe "31 January 2026"
    }

    "sum the tax due for all valid transfers" in {

      val document = ParsedStcRowsDocument(
        _id = "Ref",
        fileName = "test.csv",
        rows = Seq(row1, row2)
      )

      when(taxDueCalculationService.calculateTaxDue(row1))
        .thenReturn(BigDecimal("12.50"))

      when(taxDueCalculationService.calculateTaxDue(row2))
        .thenReturn(BigDecimal("25.75"))

      when(taxDueCalculationService.formatCurrency(BigDecimal("38.25")))
        .thenReturn("£38.25")

      when(formattingService.formatPaymentDueDate(any())(any()))
        .thenReturn("15 June 2026")

      val result = service.buildViewModel(document, emptyUserAnswers)(lang, messages(applicationBuilder().build()))

      result.taxDue shouldBe "£38.25"
      result.transfers.map(_.taxDue).sum shouldBe BigDecimal("38.25")
    }
  }
}