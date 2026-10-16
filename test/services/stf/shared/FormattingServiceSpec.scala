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

package services.stf.shared

import base.SpecBase
import play.api.i18n.Lang
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService

import java.time.LocalDate

class FormattingServiceSpec extends SpecBase {

  val service = new FormattingService()

  "FormattingService" - {

    "formatTaxDue" - {
      "must format tax due with pound sign and two decimal places" in {
        val taxDue = BigDecimal("1234.56")
        val result = service.formatTaxDue(taxDue)
        result mustBe "£1234.56"
      }

      "must format whole numbers with two decimal places" in {
        val taxDue = BigDecimal("1000")
        val result = service.formatTaxDue(taxDue)
        result mustBe "£1000.00"
      }

      "must format zero correctly" in {
        val taxDue = BigDecimal("0")
        val result = service.formatTaxDue(taxDue)
        result mustBe "£0.00"
      }

      "must format large amounts correctly" in {
        val taxDue = BigDecimal("999999.99")
        val result = service.formatTaxDue(taxDue)
        result mustBe "£999999.99"
      }

      "must format small amounts correctly" in {
        val taxDue = BigDecimal("0.01")
        val result = service.formatTaxDue(taxDue)
        result mustBe "£0.01"
      }
    }

    "formatPaymentDueDate" - {
      "must format date in English" in {
        implicit val lang: Lang = Lang("en")
        val date = LocalDate.of(2024, 2, 14)
        val result = service.formatPaymentDueDate(date)
        result mustBe "14 February 2024"
      }

      "must format date with single digit day" in {
        implicit val lang: Lang = Lang("en")
        val date = LocalDate.of(2024, 1, 5)
        val result = service.formatPaymentDueDate(date)
        result mustBe "5 January 2024"
      }

      "must format date at year end" in {
        implicit val lang: Lang = Lang("en")
        val date = LocalDate.of(2024, 12, 31)
        val result = service.formatPaymentDueDate(date)
        result mustBe "31 December 2024"
      }

      "must format date at year start" in {
        implicit val lang: Lang = Lang("en")
        val date = LocalDate.of(2024, 1, 1)
        val result = service.formatPaymentDueDate(date)
        result mustBe "1 January 2024"
      }

      "must format leap year date" in {
        implicit val lang: Lang = Lang("en")
        val date = LocalDate.of(2024, 2, 29)
        val result = service.formatPaymentDueDate(date)
        result mustBe "29 February 2024"
      }
    }

    "formatCurrency" - {
      "must show whole numbers without decimals" in {
        service.formatCurrency(BigDecimal("1000")) mustBe "£1,000"
      }

      "must convert a whole number with .00 so that no pence are displayed" in {
        service.formatCurrency(BigDecimal("1000.00")) mustBe "£1,000"
      }

      "must format zero without decimals" in {
        service.formatCurrency(BigDecimal("0")) mustBe "£0"
      }

      "must show two decimals when there are pence" in {
        service.formatCurrency(BigDecimal("123.45")) mustBe "£123.45"
      }

      "must keep a trailing zero on pence" in {
        service.formatCurrency(BigDecimal("246.90")) mustBe "£246.90"
        service.formatCurrency(BigDecimal("10.5")) mustBe "£10.50"
      }

      "must add thousands separators" in {
        service.formatCurrency(BigDecimal("1234567.89")) mustBe "£1,234,567.89"
      }

      "must format small amounts correctly" in {
        service.formatCurrency(BigDecimal("0.01")) mustBe "£0.01"
      }

      "must truncate rather than round up" in {
        service.formatCurrency(BigDecimal("99.999")) mustBe "£99.99"
        service.formatCurrency(BigDecimal("12.345")) mustBe "£12.34"
      }

      "must show amounts that truncate to zero as £0" in {
        service.formatCurrency(BigDecimal("0.001")) mustBe "£0"
      }
    }

    "formatDate" - {
      "must format a date as day, month name and year" in {
        service.formatDate(LocalDate.of(2024, 2, 14)) mustBe "14 February 2024"
      }

      "must not pad a single digit day with a zero" in {
        service.formatDate(LocalDate.of(2024, 1, 5)) mustBe "5 January 2024"
      }

      "must format date at year end" in {
        service.formatDate(LocalDate.of(2024, 12, 31)) mustBe "31 December 2024"
      }

      "must format date at year start" in {
        service.formatDate(LocalDate.of(2024, 1, 1)) mustBe "1 January 2024"
      }

      "must format leap year date" in {
        service.formatDate(LocalDate.of(2024, 2, 29)) mustBe "29 February 2024"
      }

      "must capitalise only the first letter of the month" in {
        service.formatDate(LocalDate.of(2026, 9, 14)) mustBe "14 September 2026"
      }
    }
  }
}
