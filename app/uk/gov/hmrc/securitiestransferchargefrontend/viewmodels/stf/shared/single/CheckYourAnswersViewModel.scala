/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.shared.single

import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList

case class SummarySection(
                           headingKey: String,
                           summaryList: SummaryList
                         )

case class CheckYourAnswersViewModel(
                                      sections: Seq[SummarySection],
                                      taxDueFormatted: Option[String],
                                      paymentDueDateFormatted: Option[String]
                                    )

object CheckYourAnswersViewModel {

  object MessageKeys {
    val title = "checkYourAnswers.title"
    val heading = "checkYourAnswers.heading"

    // Section headings
    val yourDetailsHeading = "checkYourAnswers.yourDetails.heading"
    val sellerDetailsHeading = "checkYourAnswers.sellerDetails.heading"
    val buyerDetailsHeading = "checkYourAnswers.buyerDetails.heading"
    val transferDetailsHeading = "checkYourAnswers.transferDetails.heading"
    val securitiesDetailsHeading = "checkYourAnswers.securitiesDetails.heading"

    // Tax due section
    val taxDueHeading = "checkYourAnswers.taxDue.heading"
    val taxDueBody = "checkYourAnswers.taxDue.body"
    val paymentDueBy = "checkYourAnswers.paymentDueBy"

    // Declaration section
    val declarationHeading = "checkYourAnswers.declaration.heading"
    val declarationBody = "checkYourAnswers.declaration.body"
    val acceptAndSend = "checkYourAnswers.acceptAndSend"

    // Print section
    val printHeading = "checkYourAnswers.print.heading"

    // Agent Declaration section
    val declarationP1 = "agent.checkYourAnswers.declaration.p1"
    val declarationBullet1 = "agent.checkYourAnswers.declaration.bullet1"
    val declarationBullet2 = "agent.checkYourAnswers.declaration.bullet2"
  }

  def fromSummaryLists(
                        summaryLists: Seq[SummaryList],
                        taxDueFormatted: Option[String],
                        paymentDueDateFormatted: Option[String]
                      ): CheckYourAnswersViewModel = {
    val sectionHeadings = Seq(
      MessageKeys.sellerDetailsHeading,
      MessageKeys.transferDetailsHeading,
      MessageKeys.securitiesDetailsHeading
    )

    val sections = summaryLists.zip(sectionHeadings).map { case (list, heading) =>
      SummarySection(heading, list)
    }

    CheckYourAnswersViewModel(sections, taxDueFormatted, paymentDueDateFormatted)
  }

  def fromAgentSummaryLists(
                        summaryLists: Seq[SummaryList],
                        taxDueFormatted: Option[String],
                        paymentDueDateFormatted: Option[String]
                      ): CheckYourAnswersViewModel = {
    val sectionHeadings = Seq(
      MessageKeys.yourDetailsHeading,
      MessageKeys.buyerDetailsHeading,
      MessageKeys.sellerDetailsHeading,
      MessageKeys.transferDetailsHeading,
    )

    val sections = summaryLists.zip(sectionHeadings).map { case (list, heading) =>
      SummarySection(heading, list)
    }

    CheckYourAnswersViewModel(sections, taxDueFormatted, paymentDueDateFormatted)
  }
}