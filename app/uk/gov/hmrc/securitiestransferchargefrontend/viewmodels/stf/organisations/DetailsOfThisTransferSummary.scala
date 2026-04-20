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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.organisations

import play.api.i18n.Messages
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryListRow
import uk.gov.hmrc.securitiestransferchargefrontend.config.CurrencyFormatter.currencyFormat
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.individuals.single.DetailsOfThisTransferPage
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*

object DetailsOfThisTransferSummary {

  def row(answers: UserAnswers)(implicit messages: Messages): Option[SummaryListRow] =
    answers.get(DetailsOfThisTransferPage).map {
      answer =>

        val value = Html(
          s"""
             |${HtmlFormat.escape(answer.numberOfShares).body}<br/>
             |${HtmlFormat.escape(answer.typeOfShares).body}<br/>
             |${HtmlFormat.escape(currencyFormat(answer.amountPaid)).body}<br/>
             |${answer.marketValue.map(v => HtmlFormat.escape(currencyFormat(v)).body).getOrElse("")}
             |""".stripMargin
        )

        SummaryListRowViewModel(
          key = "detailsOfThisTransfer.checkYourAnswersLabel",
          value = ValueViewModel(HtmlContent(value)),
          actions = Seq(
            ActionItemViewModel("site.change", routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url)
              .withVisuallyHiddenText(messages("detailsOfThisTransfer.change.hidden"))
          )
        )
    }
}
