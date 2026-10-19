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

package uk.gov.hmrc.securitiestransferchargefrontend.models.submission

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.{SummaryList, SummaryListRow}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{Other, SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.services.stf.shared.FormattingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.all.SummaryListRowViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.implicits.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.govuk.summarylist.*


case class SubmissionDetailsSummary(total: String,
                                    paymentDueBy: String,
                                    summaryList: SummaryList
                                   )
object SubmissionDetailsSummary {
  
  private def row(keyMessage: String, value: String)(implicit messages: Messages): SummaryListRow = {
    SummaryListRowViewModel(
      key = KeyViewModel(messages(keyMessage)).withCssClass("govuk-!-width-one-half"),
      value = ValueViewModel(value)
    )
  }
  
  private def summaryList(submission: SttSubmission, formatter: FormattingService)(implicit messages: Messages): SummaryList = {
    val submissionDate = formatter.formatDate(submission.submissionDate)
    val transferType = submission.submissionType match {
      case STF    => messages("submissionDetails.transferType.stf")
      case SH03   => messages("submissionDetails.transferType.sh03")
      case Other  => "TBC" // TODO: what do we display if transfer type is other?
    }
    val numOfTransfers = submission.numberOfTransfers.toString
    val totalTaxDue = formatter.formatCurrency(submission.taxToPay)

    val rows = Seq(
      row("submissionDetails.submission.date", submissionDate),
      row("submissionDetails.submission.type", transferType),
      row("submissionDetails.submission.transfers", numOfTransfers),
      row("submissionDetails.submission.totalTaxDue", totalTaxDue).copy(classes = "no-bottom-border")
    )

    SummaryList(rows = rows)
  }
  
  def getSummary(submission: SttSubmission, formatter: FormattingService)(implicit messages: Messages): SubmissionDetailsSummary =
    SubmissionDetailsSummary(
      total         = formatter.formatCurrency(submission.totalToPay),
      paymentDueBy  = formatter.formatDate(submission.paymentDueByDate),
      summaryList   = summaryList(submission, formatter)
    )
  
}