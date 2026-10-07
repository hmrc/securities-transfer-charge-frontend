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

import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers.isInThePast

object EtmpSuccessResponseExtensions:

  extension (resp: EtmpSuccessResponse)

    /*
     * An STT submission can generate more than one ETMP Transaction.
     * An ETMP Transaction can generate more than one ETMP charge. 
     */

    def allTransactionDetails: Seq[EtmpTransactionDetail] = resp.transactionDetails.getOrElse(List.empty)
    def allChargeDetails: Seq[EtmpChargeDetail] = resp.charges.getOrElse(List.empty)

    def allSubmissionIds: Seq[String] =
      allTransactionDetails
        .map(_.submissionId)
        .distinct

    def toSttSubmissions: Seq[SttSubmission] = for {
      submissionId  <- allSubmissionIds
      transactions   = allTransactionDetails.filter(_.submissionId == submissionId) // >= 1
      submissionData = transactions.head
    } yield {
      SttSubmission(
        submissionId    = submissionData.submissionId,
        submissionDate  = submissionData.submissionDate,
        clientReference = submissionData.clientReference,
        declareeName    = Some(submissionData.declareeName), // TODO: ETMP needs to make declaree name optional.
        transfers       = transactions.map(transfersForSubmission)
      )
    }

    private def transfersForSubmission(tx: EtmpTransactionDetail): SttTransfer = {
      val charges = allChargeDetails.filter(_.utrn == tx.utrn)
      SttTransfer(
        utrn        = tx.utrn,
        buyerNames  = tx.buyerNames,
        sellerNames = tx.sellerNames,
        companyName = tx.companyName,
        charges     = charges
      )
    }

  object ChargeTypes {
    val tax                 = "STT"
    val lateFiling          = "LFP"
    val latePayment         = "LPP"
    val latePaymentInterest = "LPI"
  }

  extension (charge: EtmpChargeDetail)

    private def hasPendingAmount: EtmpChargeDetail => Boolean = chg => chg.chargeAmountPending > 0

    def isPaid    : Boolean = charge.chargeAmountPending == 0
    def isUnpaid  : Boolean = hasPendingAmount(charge) && !isInThePast(charge.chargeDueDate)
    def isOverdue : Boolean = hasPendingAmount(charge) && isInThePast(charge.chargeDueDate)

    private def chargeTypeIs: String => Boolean = _ == charge.chargeType
    
    def isTaxCharge           : Boolean = chargeTypeIs(ChargeTypes.tax)
    def isLateFilingPenalty   : Boolean = chargeTypeIs(ChargeTypes.lateFiling)
    def isLatePaymentPenalty  : Boolean = chargeTypeIs(ChargeTypes.latePayment)
    def isLatePaymentInterest : Boolean = chargeTypeIs(ChargeTypes.latePaymentInterest)
