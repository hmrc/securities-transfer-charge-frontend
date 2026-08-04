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

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.PurchasingSharesFormProvider

class PurchasingSharesFormProviderSpec extends BooleanFieldBehaviours {

  private val affinityKeys: Seq[String] = Seq("org", "agent","individual")

  val fieldName = "value"

  val invalidKey = "error.boolean"

  ".value" - {

    affinityKeys.foreach { key =>

      s"when affinityKey is $key" - {

        val form = new PurchasingSharesFormProvider()(affinityKey = key)
        val requiredKey = s"$key.purchasingShares.error.required"

        behave like booleanField(
          form,
          fieldName,
          invalidError = FormError(fieldName, invalidKey)
        )

        behave like mandatoryField(
          form,
          fieldName,
          requiredError = FormError(fieldName, requiredKey)
        )
      }
    }
  }
}
