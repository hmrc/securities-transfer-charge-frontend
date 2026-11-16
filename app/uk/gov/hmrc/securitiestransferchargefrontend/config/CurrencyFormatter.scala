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

package uk.gov.hmrc.securitiestransferchargefrontend.config

import java.util.Locale
import scala.math.BigDecimal.RoundingMode

trait CurrencyFormatter {

  /**
   * Formats an amount in GBP for display, e.g. £10.10, £100, £1,000, £3,000,000.50.
   * Up to 2 decimal places are shown, unless the pence are zero, in which case no decimals are shown.
   * Amounts over 3 digits are grouped with commas every three digits.
   */
  def currencyFormat(amt: BigDecimal): String = {
    val rounded = amt.setScale(2, RoundingMode.HALF_UP)
    if (rounded.isWhole) String.format(Locale.UK, "£%,.0f", rounded.bigDecimal)
    else String.format(Locale.UK, "£%,.2f", rounded.bigDecimal)
  }
}

object CurrencyFormatter extends CurrencyFormatter
