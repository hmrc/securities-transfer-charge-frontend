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

package forms.stf.fileUpload

import forms.behaviours.CurrencyFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.fileUpload.AmountPaidForSecuritiesFormProvider

import scala.math.BigDecimal.RoundingMode

class AmountPaidForSecuritiesFormProviderSpec extends CurrencyFieldBehaviours {

  private val affinityKeys: Seq[String] = Seq("agent","org","individual")

  ".value" - {

    affinityKeys.foreach { key =>

      s"when affinityKey is $key" - {

        val form = new AmountPaidForSecuritiesFormProvider()(affinityKey = key)
        val fieldName = "value"

        val minimum = BigDecimal("0.01")
        val maximum = BigDecimal("999999999")

        val validDataGenerator =
          Gen
            .chooseNum(minimum.toInt, maximum.toInt)
            .map(BigDecimal(_).setScale(2, RoundingMode.HALF_UP))
            .map(_.toString)

        behave like fieldThatBindsValidData(
          form,
          fieldName,
          validDataGenerator
        )

        behave like currencyField(
          form,
          fieldName,
          nonNumericError =
            FormError(fieldName, s"fileUpload.$key.amountPaidForSecurities.error.nonNumeric"),
          invalidNumericError =
            FormError(fieldName, s"fileUpload.$key.amountPaidForSecurities.error.invalidNumeric")
        )

        behave like currencyFieldWithMaximum(
          form,
          fieldName,
          maximum,
          FormError(
            fieldName,
            s"fileUpload.$key.amountPaidForSecurities.error.aboveMaximum",
            Seq(currencyFormat(maximum))
          )
        )

        behave like mandatoryField(
          form,
          fieldName,
          requiredError =
            FormError(fieldName, s"fileUpload.$key.amountPaidForSecurities.error.required")
        )

        behave like currencyFieldWithMinimum(
          form,
          fieldName,
          minimum = minimum,
          expectedError =
            FormError(fieldName, s"fileUpload.$key.amountPaidForSecurities.error.belowMinimum",Seq(currencyFormat(minimum)))
        )
      }
    }
  }
}