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

package uk.gov.hmrc.securitiestransferchargefrontend.models.audit

import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.*
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.HowToNotifyAboutShareBuybackPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.format.DateTimeFormatter

object SH03PostSubmissionAuditModelBuilder {

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  def buildSubmissionAuditModel(
    declaration: SingleTransferDeclaration,
    agentDetails: Option[SingleTransferAgentDetails],
    sh03Requests: Seq[SingleTransferRequest],
    etmpResponses: Seq[SubmissionCreateResponseSuccess],
    request: StcDataRequest[_],
    totalBatches: Int = 1,
    totalTransfers: Int = 1,
    currentBatch: Int = 1,
    batchStartIndex: Int = 0,
    versionNumber: Int = 0
  ): SH03PostSubmissionAuditModel = {
    
    val transfers = sh03Requests.zip(etmpResponses).zipWithIndex.map { case ((sh03Request, etmpResponse), index) =>
      buildTransfer(
        transferNumber = batchStartIndex + index + 1,
        sh03Request = sh03Request,
        etmpResponse = etmpResponse
      )
    }

    val transferMethod = request.userAnswers
      .get(HowToNotifyAboutShareBuybackPage)
      .map(_.toString)
      .getOrElse("")

    val auditDetail = SH03PostSubmissionAuditDetail(
      declarantRole1 = declaration.role1.map(_.toString).getOrElse(""),
      declarantRole2 = declaration.role2.getOrElse(""),
      reference = agentDetails.flatMap(_.clientReference).getOrElse(""),
      declarantName = declaration.name,
      declarantAddressLine1 = declaration.addr1,
      declarantAddressLine2 = declaration.addr2.getOrElse(""),
      declarantAddressLine3 = declaration.addr3.getOrElse(""),
      declarantAddressLine4 = declaration.addr4.getOrElse(""),
      declarantPostcode = declaration.postcode,
      declarantCountry = declaration.country,
      affinityGroup = request.request.affinityGroup.toString,
      selfDeclarationAgent = declaration.selfDeclarationAgent.getOrElse(false),
      isCorrectInfo = declaration.isCorrectInfo,
      transferMethod = transferMethod,
      subscriptionId = request.request.subscriptionId.value,
      submissionId = request.userAnswers.submissionId.value,
      credentialId = request.request.credentialId.value,
      totalBatches = totalBatches,
      totalTransfers = totalTransfers,
      currentBatch = currentBatch,
      versionNumber = versionNumber,
      transfers = transfers
    )

    SH03PostSubmissionAuditModel(auditDetail = auditDetail)
  }

  private def buildTransfer(
    transferNumber: Int,
    sh03Request: SingleTransferRequest,
    etmpResponse: SubmissionCreateResponseSuccess
  ): SH03Transfer = {
    
    val transactionDetails = buildTransactionDetails(sh03Request)
    val mainBuyerDetails = buildMainBuyerDetails(sh03Request)
    val transferCharges = buildTransferCharges(transferNumber, etmpResponse)

    SH03Transfer(
      transferNumber = transferNumber,
      transactionDetails = transactionDetails,
      mainBuyerDetails = mainBuyerDetails,
      transferCharges = Seq(transferCharges)
    )
  }

  private def buildTransactionDetails(sh03Request: SingleTransferRequest): SH03TransactionDetails = {
    val txnDetails = sh03Request.transactionDetails
    
    SH03TransactionDetails(
      transactionType = 1,
      reasonForPurchase = txnDetails.reasonForPurchase.map(_.toString).getOrElse(""),
      typeOfSecurity = txnDetails.typeOfSecurity,
      numberOfShares = BigDecimal(txnDetails.numberOfShares),
      nominalValue = "",
      marketValue = txnDetails.marketValue.getOrElse(BigDecimal(0)),
      qualifyAsTreasuryShares = txnDetails.qualifyAsTreasuryShares.getOrElse(false),
      maxPricePaid = txnDetails.maxPricePaid.getOrElse(BigDecimal(0)),
      minPricePaid = txnDetails.minPricePaid.getOrElse(BigDecimal(0)),
      originalChargingPoint = txnDetails.originalChargingPoint.format(dateFormatter),
      considerationActual = txnDetails.considerationActual,
      isConnectedPartiesTransactions = txnDetails.isConnectedPartiesTransactions,
      companyName = txnDetails.companyName,
      companyRegistrationNumber = txnDetails.companyRegistrationNumber.getOrElse(""),
      reliefClaimedName = txnDetails.reliefClaimedName.getOrElse(""),
      reliefPercentage = txnDetails.reliefPercentage.getOrElse(0)
    )
  }
  
  private def buildMainBuyerDetails(sh03Request: SingleTransferRequest): SH03MainBuyerDetails = {
    val buyerDetails = sh03Request.mainBuyerDetails
    
    SH03MainBuyerDetails(
      mainBuyerName = buyerDetails.buyerName,
      mainBuyerAddressLine1 = buyerDetails.addr1,
      mainBuyerAddressLine2 = buyerDetails.addr2.getOrElse(""),
      mainBuyerAddressLine3 = buyerDetails.addr3.getOrElse(""),
      mainBuyerAddressLine4 = buyerDetails.addr4.getOrElse(""),
      postcode = buyerDetails.postcode,
      country = buyerDetails.country,
      email = buyerDetails.email,
      uniqueId = buyerDetails.uniqueId.getOrElse(""),
      taxRate = getTaxRateValue(buyerDetails.taxRate),
      isPublicLimitedCompany = buyerDetails.isPLC.getOrElse(false)
    )
  }

  private def buildTransferCharges(
    transferNumber: Int,
    etmpResponse: SubmissionCreateResponseSuccess
  ): SH03TransferCharge = {
    SH03TransferCharge(
      transferNumber = transferNumber,
      uniqueTransferReferenceNumber = etmpResponse.utrn,
      chargeAmount = etmpResponse.taxDue,
      chargeTypeDescription = "STT",
      chargeReference = etmpResponse.chargeReferences.headOption.getOrElse(""),
      chargeDueDate = etmpResponse.paymentDueBy.format(dateFormatter)
    )
  }

  private def getTaxRateValue(taxRate: BuyerTaxRate): Int = {
    taxRate match {
      case BuyerTaxRate.HalfPercent => 1
      case BuyerTaxRate.OneAndHalfPercent => 2
    }
  }
}