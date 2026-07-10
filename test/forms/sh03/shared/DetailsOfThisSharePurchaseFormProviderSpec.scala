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
import forms.behaviours.{CurrencyFieldBehaviours, IntFieldBehaviours, StringFieldBehaviours}
import org.scalacheck.Gen
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.DetailsOfThisSharePurchaseFormProvider


class DetailsOfThisSharePurchaseFormProviderSpec
  extends StringFieldBehaviours
    with CurrencyFieldBehaviours
    with IntFieldBehaviours
    with SpecBase {
  
  ".value" - {

    Seq("agent", "org").foreach {affinityKey =>

      s"when affinity key is $affinityKey" - {

        val form = new DetailsOfThisSharePurchaseFormProvider()(affinityKey = affinityKey)
        val requiredKey = s"$affinityKey.sh03.detailsOfSharePurchase.error.numberOfShares.required"
        val nonNumericKey = s"$affinityKey.sh03.detailsOfSharePurchase.error.numberOfShares.nonNumeric"
        val wholeNumberKey = s"$affinityKey.sh03.detailsOfSharePurchase.error.numberOfShares.wholeNumber"
        val minimumKey = s"$affinityKey.sh03.detailsOfSharePurchase.error.numberOfShares.min"
        val maximumKey = s"$affinityKey.sh03.detailsOfSharePurchase.error.numberOfShares.max"
        val min = 1

        ".numberOfShares" - {
          val fieldName = "numberOfShares"

          behave like intFieldWithMinimum(
            form,
            fieldName,
            minimum = min,
            expectedError = FormError(fieldName, minimumKey)
          )

          "reject values above maximum" in {

            val result = form.bind(Map(fieldName -> "1000000000"))

            result.errors must contain(
              FormError(fieldName, maximumKey)
            )
          }

          behave like mandatoryField(
            form,
            fieldName,
            requiredError = FormError(fieldName, requiredKey)
          )

          behave like intField(
            form,
            fieldName,
            FormError(fieldName, nonNumericKey),
            FormError(fieldName, wholeNumberKey)
          )
        }

        ".typeOfShares" - {

          val fieldName = "typeOfShares"

          val requiredKey =
            s"$affinityKey.sh03.detailsOfSharePurchase.error.typeOfShares.required"

          val lengthKey =
            s"$affinityKey.sh03.detailsOfSharePurchase.error.typeOfShares.length"

          val maxLength = 100

          behave like fieldThatBindsValidData(
            form,
            fieldName,
            stringsWithMaxLength(maxLength)
          )

          behave like fieldWithMaxLength(
            form,
            fieldName,
            maxLength = maxLength,
            lengthError = FormError(fieldName, lengthKey, Seq(maxLength))
          )

          behave like mandatoryField(
            form,
            fieldName,
            requiredError = FormError(fieldName, requiredKey)
          )
        }

        ".amountPaid" - {

          val fieldName = "amountPaid"

          val maximum = BigDecimal(999999999)
          val minimum = BigDecimal(1.00)

          val validDataGenerator =
            Gen
              .chooseNum(minimum, maximum)
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
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.amountPaid.nonNumeric"
              ),
            invalidNumericError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.amountPaid.invalidNumeric"
              )
          )

          behave like currencyFieldWithMaximum(
            form,
            fieldName,
            maximum = maximum,
            expectedError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.amountPaid.aboveMaximum",
                Seq(currencyFormat(maximum))
              )
          )

          behave like currencyFieldWithMinimum(
            form,
            fieldName,
            minimum = minimum,
            expectedError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.amountPaid.belowMinimum",
                Seq(currencyFormat(minimum))
              )
          )

          behave like mandatoryField(
            form,
            fieldName,
            requiredError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.amountPaid.required"
              )
          )
        }

        ".marketValue" - {

          val fieldName = "marketValue"

          val maximum = BigDecimal(999999999)
          val minimum = BigDecimal(0.01)

          val validDataGenerator =
            Gen
              .chooseNum(minimum, maximum)
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
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.marketValue.nonNumeric"
              ),
            invalidNumericError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.marketValue.invalidNumeric"
              )
          )

          behave like currencyFieldWithMaximum(
            form,
            fieldName,
            maximum = maximum,
            expectedError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.marketValue.aboveMaximum",
                Seq(currencyFormat(maximum))
              )
          )

          behave like mandatoryField(
            form,
            fieldName,
            requiredError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.marketValue.required"
              )
          )

          behave like currencyFieldWithMinimum(
            form,
            fieldName,
            minimum = minimum,
            expectedError =
              FormError(
                fieldName,
                s"$affinityKey.sh03.detailsOfSharePurchase.error.marketValue.belowMinimum",
                Seq(currencyFormat(minimum))
              )
          )
        }

        ".marketValue when requireMarketValue = false" - {

          val optionalForm =
            new DetailsOfThisSharePurchaseFormProvider()(
              requireMarketValue = false,
              affinityKey = affinityKey
            )

          "not require marketValue" in {

            val result = optionalForm.bind(
              Map(
                "numberOfShares" -> "10",
                "typeOfShares" -> "Ordinary",
                "amountPaid" -> "100"
              )
            )

            result.errors mustBe empty
          }
        }
      }
    }
  }
}