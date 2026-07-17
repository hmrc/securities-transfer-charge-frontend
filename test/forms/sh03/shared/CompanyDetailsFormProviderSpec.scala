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

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared.CompanyDetailsFormProvider

class CompanyDetailsFormProviderSpec extends StringFieldBehaviours {

  private val formProvider = new CompanyDetailsFormProvider()

  ".value" - {

    Seq("agent", "org").foreach { affinityKey =>

      s"when affinity key is $affinityKey" - {

        val form = formProvider(affinityKey)

        ".companyName" - {

          val fieldName = "companyName"
          val requiredKey = s"$affinityKey.sh03.companyDetails.companyName.error.required"
          val lengthKey = s"$affinityKey.sh03.companyDetails.companyName.error.length"
          val maxLength = 160

          behave like fieldThatBindsValidData(
            form,
            fieldName,
            stringsWithMaxLength(maxLength)
          )

          behave like mandatoryField(
            form,
            fieldName,
            requiredError = FormError(fieldName, requiredKey)
          )

          "must not bind strings longer than max length" in {
            val result =
              form.bind(Map(fieldName -> ("a" * (maxLength + 1)))).apply(fieldName)

            result.errors.map(_.message) must contain(lengthKey)
          }
        }

        ".companyRegistrationNumber" - {

          val fieldName = "companyRegistrationNumber"
          val requiredKey = s"$affinityKey.sh03.companyDetails.crn.error.required"
          val invalidKey = s"$affinityKey.sh03.companyDetails.crn.error.invalid"
          val lengthKey = s"$affinityKey.sh03.companyDetails.crn.error.length"

          behave like fieldThatBindsValidData(
            form,
            fieldName,
            Gen.oneOf("AB123456", "12345678", "SN898989")
          )

          behave like mandatoryField(
            form,
            fieldName,
            requiredError = FormError(fieldName, requiredKey)
          )

          "must bind valid 8-character alphanumeric CRNs" in {
            Seq("AB123456", "12345678", "ABCDEFGH", "SN898989").foreach { crn =>
              val result = form.bind(
                Map(
                  "companyName" -> "Test Company",
                  fieldName -> crn,
                  "isPlc" -> "true"
                )
              )

              result.errors mustBe empty
              result.value.value.companyRegistrationNumber mustBe crn
            }
          }

          "must not bind strings with invalid characters" in {
            Seq("AB12-456", "AB12 456", "AB12@456", "AB12.456").foreach { crn =>
              val result = form.bind(
                Map(
                  "companyName" -> "Test Company",
                  fieldName -> crn,
                  "isPlc" -> "true"
                )
              ).apply(fieldName)

              result.errors.map(_.message) must contain(invalidKey)
            }
          }

          "must not bind strings shorter than 8 characters" in {
            Seq("A", "AB", "ABC1234", "1234567").foreach { crn =>
              val result = form.bind(
                Map(
                  "companyName" -> "Test Company",
                  fieldName -> crn,
                  "isPlc" -> "true"
                )
              ).apply(fieldName)

              result.errors.map(_.message) must contain(lengthKey)
            }
          }

          "must not bind strings longer than 8 characters" in {
            Seq("AB1234567", "123456789", "ABCDEFGHIJ").foreach { crn =>
              val result = form.bind(
                Map(
                  "companyName" -> "Test Company",
                  fieldName -> crn,
                  "isPlc" -> "true"
                )
              ).apply(fieldName)

              result.errors.map(_.message) must contain(lengthKey)
            }
          }

          "must show invalid error before length error when both validations fail" in {
            val result = form.bind(
              Map(
                "companyName" -> "Test Company",
                fieldName -> "AB-123",
                "isPlc" -> "true"
              )
            ).apply(fieldName)

            result.errors.head.message mustBe invalidKey
          }
        }

        ".isPlc" - {

          val fieldName = "isPlc"
          val requiredKey = s"$affinityKey.sh03.companyDetails.isPlc.error.required"

          behave like mandatoryField(
            form,
            fieldName,
            requiredError = FormError(fieldName, requiredKey)
          )

          "must bind true" in {
            val result = form.bind(
              Map(
                "companyName" -> "Test Company",
                "companyRegistrationNumber" -> "AB123456",
                fieldName -> "true"
              )
            )

            result.value.value.isPlc mustBe true
          }

          "must bind false" in {
            val result = form.bind(
              Map(
                "companyName" -> "Test Company",
                "companyRegistrationNumber" -> "AB123456",
                fieldName -> "false"
              )
            )

            result.value.value.isPlc mustBe false
          }
        }
      }
    }
  }
}
