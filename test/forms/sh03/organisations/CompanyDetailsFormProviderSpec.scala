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

package forms.sh03.organisations

import forms.behaviours.StringFieldBehaviours
import org.scalacheck.Gen
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.organisations.CompanyDetailsFormProvider

class CompanyDetailsFormProviderSpec extends StringFieldBehaviours {

  val requiredKey = "org.sh03.companyDetails.error.required"
  val invalidKey = "org.sh03.companyDetails.error.invalid"
  val lengthKey = "org.sh03.companyDetails.error.length"

  val form = new CompanyDetailsFormProvider()()

  ".value" - {

    val fieldName = "value"

    behave like fieldThatBindsValidData(
      form,
      fieldName,
      Gen.oneOf("AB123456", "12345678", "SN898989")
    )

    "must bind valid 8-character alphanumeric CRNs" in {
      val validCRNs = Seq("AB123456", "12345678", "ABCDEFGH", "SN898989")
      validCRNs.foreach { crn =>
        val result = form.bind(Map(
          fieldName -> crn
        ))
        result.errors mustBe empty
        result.value.value mustBe crn
      }
    }

    "must not bind strings with invalid characters" in {
      val invalidCRNs = Seq("AB12-456", "AB12 456", "AB12@456", "AB12.456")
      invalidCRNs.foreach { crn =>
        val result = form.bind(Map(
          "companyName" -> "Test Company",
          fieldName -> crn,
          "isPlc" -> "true"
        )).apply(fieldName)
        result.errors.map(_.message) must contain(invalidKey)
      }
    }

    "must not bind strings shorter than 8 characters" in {
      val shortCRNs = Seq("A", "AB", "ABC1234", "1234567")
      shortCRNs.foreach { crn =>
        val result = form.bind(Map(
          fieldName -> crn
        )).apply(fieldName)
        result.errors.map(_.message) must contain(lengthKey)
      }
    }

    "must not bind strings longer than 8 characters" in {
      val longCRNs = Seq("AB1234567", "123456789", "ABCDEFGHIJ")
      longCRNs.foreach { crn =>
        val result = form.bind(Map(
          fieldName -> crn
        )).apply(fieldName)
        result.errors.map(_.message) must contain(lengthKey)
      }
    }

    "must show invalid error before length error when both validations fail" in {
      val result = form.bind(Map(
        fieldName -> "AB-123"
      )).apply(fieldName)
      result.errors.head.message mustBe invalidKey
    }

  }
}
