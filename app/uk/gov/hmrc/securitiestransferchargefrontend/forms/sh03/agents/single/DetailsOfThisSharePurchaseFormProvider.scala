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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.sh03.agents.single

import play.api.data.Form
import play.api.data.Forms.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings.Mappings
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.agents.DetailsOfThisSharePurchase

import javax.inject.Inject

class DetailsOfThisSharePurchaseFormProvider @Inject() extends Mappings {

  private val maxNumOfShares = 999999999
  private val minNumOfShares = 1
  private val maxCurrency: BigDecimal = BigDecimal(999999999)
  private val minCurrencyForAmountPaid = 1.00
  private val minCurrencyForMarketValue = 0.01

  def apply(requireMarketValue: Boolean = true, affinityKey: String): Form[DetailsOfThisSharePurchase] =
    Form(
      mapping(
        "numberOfShares" -> int(
          s"${affinityKey}.detailsOfThisTransfer.error.numberOfShares.required",
          "detailsOfThisTransfer.error.numberOfShares.wholeNumber",
          s"${affinityKey}.detailsOfThisTransfer.error.numberOfShares.nonNumeric"
        ).verifying("detailsOfThisTransfer.error.numberOfShares.min",
          _ >= minNumOfShares
        ).verifying("detailsOfThisTransfer.error.numberOfShares.max",
          _ <= maxNumOfShares
        ),
        "typeOfShares" -> text(s"${affinityKey}.detailsOfThisTransfer.error.typeOfShares.required")
          .verifying(maxLength(100, s"${affinityKey}.detailsOfThisTransfer.error.typeOfShares.length")),
        "amountPaid" ->
          currency(s"${affinityKey}.detailsOfThisTransfer.error.amountPaid.required",
            s"${affinityKey}.detailsOfThisTransfer.error.amountPaid.invalidNumeric",
            s"${affinityKey}.detailsOfThisTransfer.error.amountPaid.nonNumeric",
            s"${affinityKey}.sh03.detailsOfSharePurchase.error.amountPaid.belowMinimum")
            .verifying(maximumCurrency(maxCurrency, s"${affinityKey}.detailsOfThisTransfer.error.amountPaid.aboveMaximum"))
            .verifying(minimumCurrency(minCurrencyForAmountPaid, s"${affinityKey}.sh03.detailsOfSharePurchase.error.amountPaid.belowMinimum")),
        "marketValue" -> optional(currency(s"${affinityKey}.detailsOfThisTransfer.error.marketValue.required",
          s"${affinityKey}.detailsOfThisTransfer.error.marketValue.invalidNumeric",
          s"${affinityKey}.detailsOfThisTransfer.error.marketValue.nonNumeric",
          s"${affinityKey}.sh03.detailsOfSharePurchase.error.marketValue.belowMinimum")
          .verifying(maximumCurrency(maxCurrency, s"${affinityKey}.detailsOfThisTransfer.error.marketValue.aboveMaximum"))
          .verifying(minimumCurrency(minCurrencyForMarketValue, s"${affinityKey}.sh03.detailsOfSharePurchase.error.marketValue.belowMinimum")))
          .verifying(requiredIf(requireMarketValue, affinityKey))
      )(DetailsOfThisSharePurchase.apply)(x => Some((x.numberOfShares, x.typeOfShares, x.amountPaid, x.marketValue)))
    )
}
