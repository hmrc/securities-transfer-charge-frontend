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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.organisations

import play.api.i18n.Messages
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.sh03.organisations.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.DetailsOfThisSharePurchasePage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object DetailsOfThisSharePurchaseSummary {

  def rows(answers: UserAnswers, showMarketValue: Boolean = false)(implicit messages: Messages): Seq[SummaryListRow] = {
    answers.get(DetailsOfThisSharePurchasePage).toSeq.flatMap { answer =>

      val numberOfSharesRow = SummaryListRowViewModel(
        key = "org.sh03.detailsOfSharePurchase.numberOfShares",
        value = ValueViewModel(HtmlFormat.escape(answer.numberOfShares.toString).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.detailsOfThisTransfer.numberOfShares.change.hidden"))
        )
      )

      val typeOfSharesRow = SummaryListRowViewModel(
        key = "org.sh03.detailsOfSharePurchase.typeOfShares",
        value = ValueViewModel(HtmlFormat.escape(answer.typeOfShares).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.detailsOfThisTransfer.typeOfShares.change.hidden"))
        )
      )

      val amountPaidRow = SummaryListRowViewModel(
        key = "org.sh03.detailsOfSharePurchase.amountPaid",
        value = ValueViewModel(HtmlFormat.escape(currencyFormat(answer.amountPaid)).toString),
        actions = Seq(
          ActionItemViewModel("site.change", routes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode).url)
            .withVisuallyHiddenText(messages("org.sh03.detailsOfThisTransfer.amountPaid.change.hidden"))
        )
      )

      val marketValueRow = if (showMarketValue) {
        answer.marketValue.map { v =>
          SummaryListRowViewModel(
            key = "org.sh03.detailsOfSharePurchase.checkYourAnswersLabel.marketValue",
            value = ValueViewModel(HtmlFormat.escape(currencyFormat(v)).toString),
            actions = Seq(
              ActionItemViewModel("site.change", routes.DetailsOfThisSharePurchaseController.onPageLoad(CheckMode).url)
                .withVisuallyHiddenText(messages("org.sh03.detailsOfThisTransfer.marketValue.change.hidden"))
            )
          )
        }
      } else None

      Seq(numberOfSharesRow, typeOfSharesRow, amountPaidRow) ++ marketValueRow
    }
  }
}