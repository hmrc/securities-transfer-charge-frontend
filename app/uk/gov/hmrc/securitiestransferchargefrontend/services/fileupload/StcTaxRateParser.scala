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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

object StcTaxRateParser {

  sealed trait ParsedTaxRate {
    def value: BigDecimal
  }

  object ParsedTaxRate {
    case object HalfPercent extends ParsedTaxRate {
      override val value: BigDecimal = BigDecimal("0.5")
    }

    case object OneAndHalfPercent extends ParsedTaxRate {
      override val value: BigDecimal = BigDecimal("1.5")
    }
  }


  def parse(value: BigDecimal): Option[ParsedTaxRate] =
    value match {
      case v if v == BigDecimal("0.5") => Some(ParsedTaxRate.HalfPercent)
      case v if v == BigDecimal("1.5") => Some(ParsedTaxRate.OneAndHalfPercent)
      case _                           => None
    }

  
}