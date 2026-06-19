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

package forms.sh03.agents.single

import forms.behaviours.BooleanFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.TreasurySharesFormProvider

class TreasurySharesFormProviderSpec extends BooleanFieldBehaviours {

  val requiredKey = "agent.sh03.treasuryShares.error.required"
  val invalidKey = "error.boolean"

  val form = new TreasurySharesFormProvider()()

  ".value" - {

    val fieldName = "value"

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
