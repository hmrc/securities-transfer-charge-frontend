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

package forms.sh03.shared

import base.SpecBase
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.MaximumAmountPaidFormProvider

class MaximumAmountPaidFormProviderSpec extends SpecBase {

  val form = new MaximumAmountPaidFormProvider()()

  ".value" - {

    val fieldName = "value"

    val requiredKey = "agent.sh03.maximumAmountPaid.error.required"
    val invalidNumericKey = "agent.sh03.maximumAmountPaid.error.invalidNumeric"
    val negativeKey = "agent.sh03.maximumAmountPaid.error.negative"
    val aboveMaximumKey = "agent.sh03.maximumAmountPaid.error.aboveMaximum"

    "must bind valid whole number values" in {
      val validValues = Seq(
        "1" -> BigDecimal("1"),
        "30" -> BigDecimal("30"),
        "999" -> BigDecimal("999"),
        "999999" -> BigDecimal("999999"),
        "999999999" -> BigDecimal("999999999")
      )

      validValues.foreach { case (input, expectedValue) =>
        val result = form.bind(Map(fieldName -> input))

        result.errors mustBe empty
        result.value.value mustBe expectedValue
      }
    }

    "must bind valid decimal values up to two decimal places" in {
      val validValues = Seq(
        "0.01" -> BigDecimal("0.01"),
        "28.60" -> BigDecimal("28.60"),
        "999.99" -> BigDecimal("999.99"),
        "999999.99" -> BigDecimal("999999.99")
      )

      validValues.foreach { case (input, expectedValue) =>
        val result = form.bind(Map(fieldName -> input))

        result.errors mustBe empty
        result.value.value mustBe expectedValue
      }
    }

    "must bind valid values containing commas" in {
      val validValues = Seq(
        "999,999" -> BigDecimal("999999"),
        "1,000,000" -> BigDecimal("1000000"),
        "999,999.99" -> BigDecimal("999999.99"),
        "9,9" -> BigDecimal("99")
      )

      validValues.foreach {
        case (input, expectedValue) =>
        val result = form.bind(Map(fieldName -> input))

        result.errors mustBe empty
        result.value.value mustBe expectedValue
      }
    }

    "must not bind when the field is empty" in {
      val result = form.bind(Map(fieldName -> "")).apply(fieldName)

      result.errors must contain(FormError(fieldName, requiredKey))
    }

    "must not bind when the field only contains spaces" in {
      val result = form.bind(Map(fieldName -> "   ")).apply(fieldName)

      result.errors must contain(FormError(fieldName, requiredKey))
    }

    "must not bind non-numeric values" in {
      val invalidValues = Seq(
        "abc",
        "invalid value",
        "30 pounds",
        "30a",
        "30-"
      )

      invalidValues.foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)

        result.errors.map(_.message) must contain(invalidNumericKey)
      }
    }

    "must not bind values with more than two decimal places" in {
      val invalidValues = Seq(
        "1.234",
        "30.999",
        "999999.999",
        "999,999.999"
      )

      invalidValues.foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)

        result.errors.map(_.message) must contain(invalidNumericKey)
      }
    }

    "must not bind invalid decimal formats" in {
      val invalidValues = Seq(
        "1..00",
        "1.2.3",
        "999.",
        "999.",
      )

      invalidValues.foreach { input =>
        val result = form.bind(Map(fieldName -> input))

        result.errors.map(_.message) must contain(invalidNumericKey)
      }
    }

    "must not bind negative values" in {
      val result = form.bind(Map(fieldName -> "-1")).apply(fieldName)

      result.errors.map(_.message) must contain(negativeKey)
    }

    "must not bind zero if the value must be at least 0.01" in {
      val result = form.bind(Map(fieldName -> "0")).apply(fieldName)

      result.errors.map(_.message) must contain(negativeKey)
    }

    "must not bind values above the maximum amount" in {
      val invalidValues = Seq(
        "1000000000",
        "999999999.01",
        "1,000,000,000"
      )

      invalidValues.foreach { input =>
        val result = form.bind(Map(fieldName -> input)).apply(fieldName)

        result.errors.map(_.message) must contain(aboveMaximumKey)
      }
    }
  }
}