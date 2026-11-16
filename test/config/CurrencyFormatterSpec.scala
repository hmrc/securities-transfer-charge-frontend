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

package config

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat

class CurrencyFormatterSpec extends AnyFreeSpec with Matchers {

  "currencyFormat" - {

    "must show two decimal places when the pence are not zero" in {
      currencyFormat(BigDecimal("10.10")) mustBe "£10.10"
      currencyFormat(BigDecimal("10.1")) mustBe "£10.10"
      currencyFormat(BigDecimal("0.01")) mustBe "£0.01"
    }

    "must not show decimal places when the pence are zero" in {
      currencyFormat(BigDecimal("100")) mustBe "£100"
      currencyFormat(BigDecimal("100.00")) mustBe "£100"
      currencyFormat(BigDecimal("0")) mustBe "£0"
    }

    "must not add a comma for amounts of 3 digits or fewer" in {
      currencyFormat(BigDecimal("999.99")) mustBe "£999.99"
      currencyFormat(BigDecimal("999")) mustBe "£999"
    }

    "must add commas every three digits for amounts over 3 digits" in {
      currencyFormat(BigDecimal("1000")) mustBe "£1,000"
      currencyFormat(BigDecimal("3000000")) mustBe "£3,000,000"
      currencyFormat(BigDecimal("9000000.00")) mustBe "£9,000,000"
      currencyFormat(BigDecimal("1234567.89")) mustBe "£1,234,567.89"
    }

    "must round to two decimal places" in {
      currencyFormat(BigDecimal("10.005")) mustBe "£10.01"
      currencyFormat(BigDecimal("99.999")) mustBe "£100"
    }
  }
}
