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

import base.SpecBase
import forms.behaviours.{CurrencyFieldBehaviours, IntFieldBehaviours, StringFieldBehaviours}
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.DetailsOfThisTransferFormProvider

class DetailsOfThisTransferFormProviderSpec
  extends StringFieldBehaviours
    with CurrencyFieldBehaviours
    with IntFieldBehaviours with SpecBase{

  private val affinityKey = affinityGroupKeyAgent

  val form = new DetailsOfThisTransferFormProvider()(
    requireMarketValue = true,
    affinityKey = affinityKey
  )

  ".numberOfShares" - {

    val fieldName = "numberOfShares"
    val requiredKey = s"$affinityKey.detailsOfThisTransfer.error.numberOfShares.required"
    val nonNumericKey = s"$affinityKey.detailsOfThisTransfer.error.numberOfShares.nonNumeric"
    val wholeNumberKey = "detailsOfThisTransfer.error.numberOfShares.wholeNumber"
    val minimumKey = "detailsOfThisTransfer.error.numberOfShares.min"
    val maximumKey = "detailsOfThisTransfer.error.numberOfShares.max"

    val max = 999999999
    val min = 1

    behave like intFieldWithMinimum(
      form,
      fieldName,
      minimum = min,
      expectedError = FormError(fieldName, minimumKey)
    )

    behave like intFieldWithMaximum(
      form,
      fieldName,
      maximum = max,
      expectedError = FormError(fieldName, maximumKey)
    )

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
      s"$affinityKey.detailsOfThisTransfer.error.typeOfShares.required"

    val lengthKey =
      s"$affinityKey.detailsOfThisTransfer.error.typeOfShares.length"

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

    val maximum = 999999999

    val validDataGenerator =
      intsInRangeWithCommas(0, maximum)

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
          s"$affinityKey.detailsOfThisTransfer.error.amountPaid.nonNumeric"
        ),
      invalidNumericError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.amountPaid.invalidNumeric"
        )
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum = maximum,
      expectedError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.amountPaid.aboveMaximum",
          Seq("£999,999,999")
        )
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.amountPaid.required"
        )
    )
  }

  ".marketValue" - {

    val fieldName = "marketValue"

    val maximum = 999999999

    val validDataGenerator =
      intsInRangeWithCommas(0, maximum)

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
          s"$affinityKey.detailsOfThisTransfer.error.marketValue.nonNumeric"
        ),
      invalidNumericError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.marketValue.invalidNumeric"
        )
    )

    behave like currencyFieldWithMaximum(
      form,
      fieldName,
      maximum = maximum,
      expectedError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.marketValue.aboveMaximum",
          Seq("£999,999,999")
        )
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError =
        FormError(
          fieldName,
          s"$affinityKey.detailsOfThisTransfer.error.marketValue.required"
        )
    )
  }

  ".marketValue when requireMarketValue = false" - {

    val optionalForm =
      new DetailsOfThisTransferFormProvider()(
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
