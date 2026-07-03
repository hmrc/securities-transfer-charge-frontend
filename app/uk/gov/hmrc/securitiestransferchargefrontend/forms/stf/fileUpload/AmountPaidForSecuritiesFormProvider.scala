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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.fileUpload

import play.api.data.Form
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings

import javax.inject.Inject

class AmountPaidForSecuritiesFormProvider @Inject() extends Mappings {

  private val max = BigDecimal("999999999")
  private val min = BigDecimal("0.01")

  def apply(affinityKey: String): Form[BigDecimal] =
    Form(
      "value" -> currency(
        s"fileUpload.$affinityKey.amountPaidForSecurities.error.required",
        s"fileUpload.$affinityKey.amountPaidForSecurities.error.invalidNumeric",
        s"fileUpload.$affinityKey.amountPaidForSecurities.error.nonNumeric",
        s"fileUpload.$affinityKey.amountPaidForSecurities.error.negative"
      ).verifying(maximumCurrency(max, s"fileUpload.$affinityKey.amountPaidForSecurities.error.aboveMaximum"))
        .verifying(minimumCurrency(min, s"fileUpload.$affinityKey.amountPaidForSecurities.error.belowMinimum"))
    )
}
