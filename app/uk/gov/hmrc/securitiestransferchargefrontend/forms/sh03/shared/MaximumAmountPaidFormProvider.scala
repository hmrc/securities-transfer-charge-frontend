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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.shared

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings

import javax.inject.Inject

class MaximumAmountPaidFormProvider @Inject() extends Mappings {

  private val minValue = BigDecimal(0.01)
  private val maxValue = BigDecimal("999999999")

  def apply(affinityKey: String): Form[BigDecimal] =
    Form(
      "value" -> currency(
        s"$affinityKey.sh03.maximumAmountPaid.error.required",
        s"$affinityKey.sh03.maximumAmountPaid.error.invalidNumeric",
        s"$affinityKey.sh03.maximumAmountPaid.error.invalidNumeric",
        s"$affinityKey.sh03.maximumAmountPaid.error.negative",
      )
        .verifying(
          firstError(
            minimumCurrency(minValue, s"$affinityKey.sh03.maximumAmountPaid.error.belowMaximum"),
            maximumCurrency(maxValue, s"$affinityKey.sh03.maximumAmountPaid.error.aboveMaximum")
          )
        )
    )
}