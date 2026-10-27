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
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Overdue, Paid, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ReasonForPurchase
import EtmpSuccessResponseExtensions.*

import java.time.LocalDate

final case class SttTransfer(
  utrn              : String,
  buyerNames        : String,
  sellerNames       : Option[String],
  companyName       : String,
  charges           : Seq[EtmpChargeDetail],
  numberOfShares    : Int = 0, // TODO: ETMP do not currently supply this via transaction summary
  reasonForPurchase : Option[ReasonForPurchase] = None // TODO: ETMP do not currently supply this via transaction summary
) {
  import SttTransfer.*

  def status: TransferStatus =
    if charges.exists(_.isOverdue)
    then Overdue
    else if charges.exists(_.isUnpaid)
    then ReadyToPay
    else Paid

  def originalTotalAmount: BigDecimal = totalOriginal(charges)

  private val toPay: (EtmpChargeDetail => Boolean) => BigDecimal =
    chargeType => totalPending(charges.filter(chargeType))

  def taxToPay                  : BigDecimal = toPay(_.isTaxCharge)
  def lateFilingPenaltiesToPay  : BigDecimal = toPay(_.isLateFilingPenalty)
  def latePaymentPenaltiesToPay : BigDecimal = toPay(_.isLatePaymentPenalty)
  def latePaymentInterestToPay  : BigDecimal = toPay(_.isLatePaymentInterest)
  def totalToPay                : BigDecimal = toPay(_ => true)

  def paymentDueByDate: LocalDate = charges.map(_.chargeDueDate).min
}

object SttTransfer:
  val totalPending: Seq[EtmpChargeDetail] => BigDecimal = _.map(_.chargeAmountPending).sum
  val totalOriginal: Seq[EtmpChargeDetail] => BigDecimal = _.map(_.chargeAmountTotal).sum

  def statusPriority(transfer: SttTransfer): Int = transfer.status match
    case Overdue => 0
    case ReadyToPay => 1
    case _ => 2

  def sorted(transfers: Seq[SttTransfer]): Seq[SttTransfer] =
    transfers.sortBy(transfer => (statusPriority(transfer), transfer.paymentDueByDate))


  given OFormat[SttTransfer] = Json.format[SttTransfer]

