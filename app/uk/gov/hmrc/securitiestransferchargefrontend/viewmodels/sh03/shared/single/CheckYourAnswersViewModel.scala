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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.sh03.shared.single

import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

case class SummarySection(
                           headingKey: String,
                           summaryList: SummaryList
                         )

case class CheckYourAnswersViewModel(
                                      sections: Seq[SummarySection],
                                      declarationList: SummaryList,
                                      taxDueFormatted: Option[String],
                                      paymentDueDateFormatted: Option[String]
                                    )

object CheckYourAnswersViewModel {

  object MessageKeys {
    val title = "checkYourAnswers.title"
    val heading = "checkYourAnswers.heading"

    // Section headings
    val yourDetailsHeading = "checkYourAnswers.yourDetails.heading"
    val buyerDetailsHeading = "checkYourAnswers.buyerDetails.heading"
    val transferDetailsHeading = "checkYourAnswers.transferDetails.heading"

    // Tax due section
    val taxDueHeading = "checkYourAnswers.taxDue.heading"
    val taxDueBody = "checkYourAnswers.taxDue.body"
    val paymentDueBy = "checkYourAnswers.paymentDueBy"

    // Print section
    val printHeading = "checkYourAnswers.print.heading"
    val printBody = "checkYourAnswers.print.body"

    // Declaration section
    val declarationHeading = "checkYourAnswers.declaration.heading"
    val declarationConfirm = "sh03.checkYourAnswers.declaration.body"
    val declarationBullet1 = "checkYourAnswers.declaration.bullet1"
    val declarationBullet2 = "checkYourAnswers.declaration.bullet2"
    val acceptAndSend = "checkYourAnswers.acceptAndSend"
  }

  def fromSummaryLists(
                        yourDetails: SummaryList,
                        buyerDetails: SummaryList,
                        transferDetails: SummaryList,
                        declarationDetails: SummaryList,
                        taxDueFormatted: Option[String],
                        paymentDueDateFormatted: Option[String]
                      ): CheckYourAnswersViewModel = {

    val sections = Seq(
      SummarySection(MessageKeys.yourDetailsHeading, yourDetails),
      SummarySection(MessageKeys.buyerDetailsHeading, buyerDetails),
      SummarySection(MessageKeys.transferDetailsHeading, transferDetails)
    )

    CheckYourAnswersViewModel(sections, declarationDetails, taxDueFormatted, paymentDueDateFormatted)
  }
}