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

package forms.sh03.agents

import forms.behaviours.StringFieldBehaviours
import play.api.data.FormError

class CompanyDetailsFormProviderSpec extends StringFieldBehaviours {

  val form = new CompanyDetailsFormProvider()()

  ".companyName" - {

    val fieldName = "companyName"
    val requiredKey = "agent.sh03.companyDetails.companyName.error.required"
    val lengthKey = "agent.sh03.companyDetails.companyName.error.length"
    val maxLength = 160

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      stringsWithMaxLength(maxLength)
    )

    behave like fieldWithMaxLength(
      form,
      fieldName,
      maxLength = maxLength,
      lengthError = FormError(fieldName, lengthKey)
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }

  ".companyRegistrationNumber" - {

    val fieldName = "companyRegistrationNumber"
    val requiredKey = "agent.sh03.companyDetails.crn.error.required"
    val lengthKey = "agent.sh03.companyDetails.crn.error.length"
    val invalidKey = "agent.sh03.companyDetails.crn.error.invalid"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Seq("AB123456", "12345678", "SN898989")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must not bind strings that are not exactly 8 characters" in {
      forAll(stringsLongerThan(8) -> "longString") { string =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        result.errors must contain(FormError(fieldName, lengthKey))
      }

      forAll(stringsShorterThan(8) -> "shortString") { string =>
        val result = form.bind(Map(fieldName -> string)).apply(fieldName)
        result.errors must contain(FormError(fieldName, lengthKey))
      }
    }

    "must not bind strings with invalid characters" in {
      val result = form.bind(Map(fieldName -> "AB12-456")).apply(fieldName)
      result.errors must contain(FormError(fieldName, invalidKey))
    }
  }

  ".isPlc" - {

    val fieldName = "isPlc"
    val requiredKey = "agent.sh03.companyDetails.isPlc.error.required"

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )

    "must bind true" in {
      val result = form.bind(Map(fieldName -> "true"))
      result.value.value.isPlc mustBe true
    }

    "must bind false" in {
      val result = form.bind(Map(fieldName -> "false"))
      result.value.value.isPlc mustBe false
    }
  }
}
