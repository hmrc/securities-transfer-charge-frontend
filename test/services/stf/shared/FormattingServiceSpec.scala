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
  }
}
