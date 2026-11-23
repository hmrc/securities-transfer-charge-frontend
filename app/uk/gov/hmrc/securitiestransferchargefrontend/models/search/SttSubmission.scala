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

package uk.gov.hmrc.securitiestransferchargefrontend.models.search

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.*

import java.time.LocalDate

final case class SttSubmission(
  submissionId    : String,
  submissionDate  : LocalDate,
  clientReference : Option[String],
  declareeName    : Option[String],
  transfers       : Seq[SttTransfer]
) {

  private val toPay: (SttTransfer => BigDecimal) => BigDecimal =
    extractChargeAmount => transfers.map(extractChargeAmount).sum

  def taxToPay                  : BigDecimal = toPay(_.taxToPay)
  def lateFilingPenaltiesToPay  : BigDecimal = toPay(_.lateFilingPenaltiesToPay)
  def latePaymentPenaltiesToPay : BigDecimal = toPay(_.latePaymentPenaltiesToPay)
  def latePaymentInterestToPay  : BigDecimal = toPay(_.latePaymentInterestToPay)
  def originalTotalAmount       : BigDecimal = toPay(_.originalTotalAmount)
  def totalToPay                : BigDecimal = toPay(_.totalToPay)

  def submissionType    : TransferType      = if declareeName.isDefined then SH03 else STF
  def paymentDueByDate  : LocalDate         = transfers.map(_.paymentDueByDate).min
  def numberOfTransfers : Int               = transfers.length
  def submissionStatus  : SubmissionStatus  = SubmissionStatus.aggregateStatus(transfers.map(_.status))

  /*
   * Transfers are numbered 1, 2, 3... in UTRN order. Every transfer has a unique UTRN, so a transfer keeps
   * the same number every time we fetch the submission, whatever order the search service returns them in
   * (which isn't guaranteed). This means the number in a View link finds the same transfer on the transfer
   * details page and when paying.
   * UTRNs are strings, so sorting them alone would put "10" before "9". Sorting by length first, then by
   * value, puts them in numeric order.
   */
  private lazy val numberedTransfers: Seq[SttTransfer] =
    transfers.sortBy(transfer => (transfer.utrn.length, transfer.utrn))

  def transferNumber(transfer: SttTransfer): Option[Int] =
    Some(numberedTransfers.indexOf(transfer)).filter(_ >= 0).map(_ + 1)

  def transferAt(transferNumber: Int): Option[SttTransfer] =
    Option.when(transferNumber > 0)(numberedTransfers.lift(transferNumber - 1)).flatten
}

object SttSubmission:
  given OFormat[SttSubmission] = Json.format[SttSubmission]
