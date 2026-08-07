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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.individuals

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.DetailsOfThisTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object DetailsOfThisTransferSummary {

  def rows(answers: UserAnswers, showMarketValue: Boolean = true)(implicit messages: Messages): Seq[SummaryListRow] = {
    val details = answers.get(DetailsOfThisTransferPage)
    val numberOfShares = details.map(_.numberOfShares.toString).getOrElse(messages("site.notProvided"))
    val typeOfShares = details.map(_.typeOfShares).getOrElse(messages("site.notProvided"))
    val amountPaid = details.map(d => currencyFormat(d.amountPaid)).getOrElse(messages("site.notProvided"))
    val marketValue = details.flatMap(_.marketValue).map(currencyFormat).getOrElse(messages("site.notProvided"))

    val baseRows = Seq(
      SummaryListRowViewModel(
        key     = "checkYourAnswers.numberOfShares",
        value   = ValueViewModel(numberOfShares),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("detailsOfThisTransfer.numberOfShares.change.hidden"))
        )
      ),
      SummaryListRowViewModel(
        key     = "checkYourAnswers.typeOfShares",
        value   = ValueViewModel(typeOfShares),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("detailsOfThisTransfer.typeOfShares.change.hidden"))
        )
      ),
      SummaryListRowViewModel(
        key     = "checkYourAnswers.amountPaidForShares",
        value   = ValueViewModel(amountPaid),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("detailsOfThisTransfer.amountPaid.change.hidden"))
        )
      )
    )
    
    val marketValueRow = if (showMarketValue) {
      Seq(SummaryListRowViewModel(
        key     = "checkYourAnswers.marketValueOfShares",
        value   = ValueViewModel(marketValue),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("detailsOfThisTransfer.marketValue.change.hidden"))
        )
      ))
    } else {
      Seq.empty
    }
    
    baseRows ++ marketValueRow
  }
}
