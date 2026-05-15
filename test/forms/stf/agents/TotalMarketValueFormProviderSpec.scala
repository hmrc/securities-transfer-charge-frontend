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

package forms.stf.agents

import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.TotalMarketValueFormProvider

import scala.math.BigDecimal.RoundingMode

class TotalMarketValueFormProviderSpec extends CurrencyFieldBehaviours {

  private val form = new TotalMarketValueFormProvider()()

  private val minimum = BigDecimal(0)
  private val maximum = BigDecimal("999999999")

  ".value" - {

    val fieldName = "value"

    val validDataGenerator: Gen[String] =
      Gen
        .choose[BigDecimal](minimum, maximum)
        .map(_.setScale(2, RoundingMode.HALF_UP))
        .map(_.toString)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError     = FormError(fieldName, "agent.totalMarketValue.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "agent.totalMarketValue.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(
        fieldName,
        "agent.totalMarketValue.error.aboveMaximum",
        Seq(currencyFormat(maximum))
      )
    )

    behave like currencyFieldWithMinimum(
      form,
      fieldName,
      minimum,
      FormError(
        fieldName,
        "agent.totalMarketValue.error.negative"
      )
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "agent.totalMarketValue.error.required")
    )
  }
}