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

package forms.stf.shared

import forms.behaviours.{CurrencyFieldBehaviours, StringFieldBehaviours}
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.DetailsOfThisTransferFormProvider

class DetailsOfThisTransferFormProviderSpec extends StringFieldBehaviours with CurrencyFieldBehaviours {

  val form = new DetailsOfThisTransferFormProvider()()

  ".numberOfShares" - {

    val fieldName = "numberOfShares"
    val requiredKey = "detailsOfThisTransfer.error.numberOfShares.required"
    val lengthKey = "detailsOfThisTransfer.error.numberOfShares.length"
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

  ".typeOfShares" - {

    val fieldName = "typeOfShares"
    val requiredKey = "detailsOfThisTransfer.error.typeOfShares.required"
    val lengthKey = "detailsOfThisTransfer.error.typeOfShares.length"
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

    val minimum = 0
    val maximum = 100000000

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "detailsOfThisTransfer.error.amountPaid.nonNumeric"),
      invalidNumericError = FormError(fieldName, "detailsOfThisTransfer.error.amountPaid.invalidNumeric")
    )

    behave like currencyFieldWithRange(
      form,
      fieldName,
      maximum = maximum,
      expectedError =
        FormError(fieldName, "detailsOfThisTransfer.error.amountPaid.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "detailsOfThisTransfer.error.amountPaid.required")
    )
  }

  ".marketValue" - {

    val fieldName = "marketValue"

    val minimum = 0
    val maximum = 100000000

    val validDataGenerator = intsInRangeWithCommas(minimum, maximum)

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      validDataGenerator
    )

    behave like currencyField(
      form,
      fieldName,
      nonNumericError = FormError(fieldName, "detailsOfThisTransfer.error.marketValue.nonNumeric"),
      invalidNumericError = FormError(fieldName, "detailsOfThisTransfer.error.marketValue.invalidNumeric")
    )

    behave like currencyFieldWithRange(
      form,
      fieldName,
      maximum = maximum,
      expectedError =
        FormError(fieldName, "detailsOfThisTransfer.error.marketValue.outOfRange", Seq(minimum, maximum))
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, "detailsOfThisTransfer.error.marketValue.required")
    )
  }
}
