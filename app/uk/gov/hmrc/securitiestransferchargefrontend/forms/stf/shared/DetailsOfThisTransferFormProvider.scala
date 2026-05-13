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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared

import play.api.data.Form
import play.api.data.Forms.*
import play.api.data.validation.Constraint
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.DetailsOfThisTransfer

import javax.inject.Inject

class DetailsOfThisTransferFormProvider @Inject() extends Mappings {

  val max = 999999999
  val min = 1

  def apply(requireMarketValue: Boolean = true): Form[DetailsOfThisTransfer] =
    Form(
      mapping(
        "numberOfShares" -> int(
          "detailsOfThisTransfer.error.numberOfShares.required",
          "detailsOfThisTransfer.error.numberOfShares.wholeNumber",
          "detailsOfThisTransfer.error.numberOfShares.nonNumeric"
        ).verifying("detailsOfThisTransfer.error.numberOfShares.min",
          _ >= min
        ).verifying("detailsOfThisTransfer.error.numberOfShares.max",
          _ <= max
        ),
        "typeOfShares" -> text("detailsOfThisTransfer.error.typeOfShares.required")
          .verifying(maxLength(100, "detailsOfThisTransfer.error.typeOfShares.length")),
        "amountPaid" ->
          currency("detailsOfThisTransfer.error.amountPaid.required",
            "detailsOfThisTransfer.error.amountPaid.invalidNumeric",
            "detailsOfThisTransfer.error.amountPaid.nonNumeric")
            .verifying(inRange(BigDecimal(0), BigDecimal(100000000), "detailsOfThisTransfer.error.amountPaid.outOfRange")),
        "marketValue" -> optional(currency("detailsOfThisTransfer.error.marketValue.required",
          "detailsOfThisTransfer.error.marketValue.invalidNumeric",
          "detailsOfThisTransfer.error.marketValue.nonNumeric")
          .verifying(inRange(BigDecimal(0), BigDecimal(100000000), "detailsOfThisTransfer.error.marketValue.outOfRange"))).verifying(requiredIf(requireMarketValue))
      )(DetailsOfThisTransfer.apply)(x => Some((x.numberOfShares, x.typeOfShares, x.amountPaid, x.marketValue)))
    )
}
