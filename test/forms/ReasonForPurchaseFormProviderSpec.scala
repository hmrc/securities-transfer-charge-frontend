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

import forms.behaviours.OptionFieldBehaviours
import play.api.data.FormError
import uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single.ReasonForPurchaseFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase

class ReasonForPurchaseFormProviderSpec extends OptionFieldBehaviours {

  val form = new ReasonForPurchaseFormProvider()()

  ".value" - {

    val fieldName = "value"
    val requiredKey = "reasonForPurchase.error.required"

    behave like optionsField[ReasonForPurchase](
      form,
      fieldName,
      validValues  = ReasonForPurchase.values,
      invalidError = FormError(fieldName, "error.invalid")
    )

    behave like mandatoryField(
      form,
      fieldName,
      requiredError = FormError(fieldName, requiredKey)
    )
  }
}
