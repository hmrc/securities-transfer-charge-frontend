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

package forms

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.SecuritiesTargetFormProvider

class SecuritiesTargetFormProviderSpec extends StringFieldBehaviours {

  val form = new SecuritiesTargetFormProvider()()

  ".BusinessName" - {

    val fieldName = "businessName"
    val requiredKey = "securitiesTarget.error.businessName.required"
    val lengthKey = "securitiesTarget.error.businessName.length"
    val maxLength = 160


    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

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
  }

  ".CRN" - {

    val fieldName = "crn"
    val lengthKey = "securitiesTarget.error.crn.length"
    val maxLength = 8

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength,
      lengthError = FormError(fieldName, lengthKey)
    )

  }
}
