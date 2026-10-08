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

import java.time.LocalDate
import EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SttTransfer.totalPending
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.*

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
  
  def submissionType: TransferType = if declareeName.isDefined then SH03 else STF
  def paymentDueByDate: LocalDate = transfers.map(_.paymentDueByDate).min
  def numberOfTransfers: Int = transfers.length
  def submissionStatus: SubmissionStatus = SubmissionStatus.aggregateStatus(transfers.map(_.status))
}


final case class SttTransfer(
  utrn        : String,
  buyerNames  : String,
  sellerNames : Option[String],
  companyName : String,
  charges     : Seq[EtmpChargeDetail]
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
  val totalPending  : Seq[EtmpChargeDetail] => BigDecimal = _.map(_.chargeAmountPending).sum
  val totalOriginal : Seq[EtmpChargeDetail] => BigDecimal = _.map(_.chargeAmountTotal).sum
