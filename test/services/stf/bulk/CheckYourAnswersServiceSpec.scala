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

package services.stf.bulk

import base.{FileUploadFixtures, SpecBase}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.i18n.Lang
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedValue.Valid
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedStcRowsDocument}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.bulk.{CheckYourAnswersService, TaxDueCalculationService}
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.{CheckYourAnswersViewModel, Transfer}

import java.time.LocalDate

class CheckYourAnswersServiceSpec extends SpecBase with FileUploadFixtures {

  implicit val lang: Lang = Lang("en")

  private val taxDueCalculationService = mock[TaxDueCalculationService]
  private val formattingService = mock[FormattingService]
  val chargingPoint: Valid[LocalDate] = Valid(LocalDate.of(2025, 5, 16))

  private val service = new CheckYourAnswersService(taxDueCalculationService, formattingService)
  val row1: ParsedStcRow = parsedStcRow(1).copy(chargingPoint =
    chargingPoint,
    sellerName = Some("Seller 1"),
    securitiesTarget = Some("Company 1"),
    amountPaidForSecurities = Some("10000"))

  val row2: ParsedStcRow = parsedStcRow(2).copy(chargingPoint =
    chargingPoint,
    sellerName = Some("Seller 2"),
    securitiesTarget = Some("Company 2"),
    amountPaidForSecurities = Some("20000"))


  "CheckYourAnswersService.buildViewModel" - {

    "build the view model with the expected transfer details" in {


      val document = ParsedStcRowsDocument(_id = "SomeRef", fileName = "test.csv", rows = Seq(row1, row2))

      when(taxDueCalculationService.calculateTaxDue(row1))
        .thenReturn(Some(BigDecimal("10000.00")))

      when(taxDueCalculationService.calculateTaxDue(row2))
        .thenReturn(Some(BigDecimal("20000.00")))

      when(taxDueCalculationService.formatCurrency(BigDecimal("30000.00")))
        .thenReturn("£30,000.00")

      when(formattingService.formatPaymentDueDate(any[LocalDate])(any()))
        .thenReturn("3 March 2026")

      val result = service.buildViewModel(document)

      result shouldBe CheckYourAnswersViewModel(
        fileName = "test.csv",
        numberOfTransfers = 2,
        taxDue = "£30,000.00",
        paymentDueBy = "3 March 2026",
        transfers = Seq(
          Transfer(
            seller = "Seller 1",
            securitiesBoughtIn = "Company 1",
            consideration = BigDecimal("10000"),
            taxDue = BigDecimal("10000.00")
          ),
          Transfer(
            seller = "Seller 2",
            securitiesBoughtIn = "Company 2",
            consideration = BigDecimal("20000"),
            taxDue = BigDecimal("20000.00")
          )
        )
      )
    }

    "use the latest charging point date and add 30 days to determine the payment due date" in {

      val chargingPoint = Valid(LocalDate.of(2026, 1, 1))
      val row = row1.copy(chargingPoint = chargingPoint)


      val document = ParsedStcRowsDocument(
        _id = "Ref",
        fileName = "test.csv",
        rows = Seq(row, row2)
      )

      when(taxDueCalculationService.calculateTaxDue(any()))
        .thenReturn(Some(BigDecimal("10.00")))

      when(formattingService.formatTaxDue(BigDecimal("20.00")))
        .thenReturn("£20.00")

      when(formattingService.formatPaymentDueDate(any[LocalDate])(any()))
        .thenReturn("31 January 2026")

      val result = service.buildViewModel(document)

      result.paymentDueBy shouldBe "31 January 2026"
    }

    "sum the tax due for all valid transfers" in {

      val document = ParsedStcRowsDocument(
        _id = "Ref",
        fileName = "test.csv",
        rows = Seq(row1, row2)
      )

      when(taxDueCalculationService.calculateTaxDue(row1))
        .thenReturn(Some(BigDecimal("12.50")))

      when(taxDueCalculationService.calculateTaxDue(row2))
        .thenReturn(Some(BigDecimal("25.75")))

      when(taxDueCalculationService.formatCurrency(BigDecimal("38.25")))
        .thenReturn("£38.25")

      when(formattingService.formatPaymentDueDate(LocalDate.of(2026, 1, 31)))
        .thenReturn("31 January 2026")

      val result = service.buildViewModel(document)

      result.taxDue shouldBe "£38.25"
      result.transfers.map(_.taxDue).sum shouldBe BigDecimal("38.25")
    }
  }
}

