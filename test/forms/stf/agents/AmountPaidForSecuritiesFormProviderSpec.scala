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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.agents.AmountPaidForSecuritiesFormProvider

import scala.math.BigDecimal.RoundingMode

class AmountPaidForSecuritiesFormProviderSpec extends CurrencyFieldBehaviours {

  val form = new AmountPaidForSecuritiesFormProvider()()

  ".value" - {

    val fieldName = "value"

    val minimum = 0
    val maximum = BigDecimal("999999999")

    val validDataGenerator =
      Gen.choose[BigDecimal](minimum, maximum)
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
      nonNumericError     = FormError(fieldName, "agent.amountPaidForSecurities.error.nonNumeric"),
      invalidNumericError = FormError(fieldName, "agent.amountPaidForSecurities.error.invalidNumeric")
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum,
      FormError(fieldName, "agent.amountPaidForSecurities.error.aboveMaximum", Seq(currencyFormat(maximum)))
    )

    behave like currencyFieldWithMinimum(
      form,
      fieldName,
      minimum,
      FormError(fieldName, "agent.amountPaidForSecurities.error.negative")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "agent.amountPaidForSecurities.error.required")
    )
  }
}
