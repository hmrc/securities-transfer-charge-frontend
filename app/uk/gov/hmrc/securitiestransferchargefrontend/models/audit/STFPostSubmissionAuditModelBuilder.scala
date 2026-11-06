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

package uk.gov.hmrc.securitiestransferchargefrontend.models.audit

import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.ChargeType
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.HowToNotifyAboutShareBuyback
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission._
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.format.DateTimeFormatter

object STFPostSubmissionAuditModelBuilder {

  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

  def buildSubmissionAuditModel(
    declaration: SingleTransferDeclaration,
    agentDetails: Option[SingleTransferAgentDetails],
    stfRequests: Seq[SingleTransferRequest],
    etmpResponses: Seq[SubmissionCreateResponseSuccess],
    stfTransaction: StfTransaction,
    request: StcDataRequest[_],
    totalBatches: Int = 1,
    totalTransfers: Int = 1,
    currentBatch: Int = 1,
    batchStartIndex: Int = 0,
    versionNumber: Int = 0,
    chargeType: String = "STC",
    chargeTypeDescription: String = ChargeType.tax
  ): STFPostSubmissionAuditModel = {
    
    val transfers = stfRequests.zip(etmpResponses).zipWithIndex.map { case ((stfRequest, etmpResponse), index) =>
      buildTransfer(
        transferNumber = batchStartIndex + index + 1,
        stfRequest = stfRequest,
        etmpResponse = etmpResponse,
        stfTransaction = stfTransaction,
        chargeType = chargeType,
        chargeTypeDescription = chargeTypeDescription
      )
    }

    val transferMethod = if (totalTransfers == 1) {
      HowToNotifyAboutShareBuyback.OneAtATime.toString
    } else {
      HowToNotifyAboutShareBuyback.MoreThanOneAtATime.toString
    }

    val auditDetail = STFPostSubmissionAuditDetail(
      declarantName = declaration.name,
      declarantAddressLine1 = declaration.addr1,
      declarantAddressLine2 = declaration.addr2.getOrElse(""),
      declarantAddressLine3 = declaration.addr3.getOrElse(""),
      declarantAddressCity = declaration.addr4.getOrElse(""),
      declarantAddressPostcode = declaration.postcode,
      declarantCountry = declaration.country,
      transferMethod = transferMethod,
      subscriptionId = request.request.subscriptionId.value,
      submissionId = request.userAnswers.submissionId.value,
      credentialId = request.request.credentialId.value,
      affinityGroup = request.request.affinityGroup.toString,
      totalBatches = totalBatches,
      totalTransfers = totalTransfers,
      currentBatch = currentBatch,
      versionNumber = versionNumber,
      declarantRole1 = declaration.role1.map(_.toString).getOrElse(""),
      declarantRole2 = declaration.role2.getOrElse(""),
      reference = agentDetails.flatMap(_.clientReference).getOrElse(""),
      transfers = transfers
    )

    STFPostSubmissionAuditModel(auditDetail = auditDetail)
  }

  private def buildTransfer(
    transferNumber: Int,
    stfRequest: SingleTransferRequest,
    etmpResponse: SubmissionCreateResponseSuccess,
    stfTransaction: StfTransaction,
    chargeType: String,
    chargeTypeDescription: String
  ): Transfer = {
    
    val mainBuyersDetails = buildMainBuyersDetails(stfRequest)
    val mainSellerDetails = buildMainSellerDetails(stfRequest)
    val transferDetails = buildTransferDetails(stfRequest, stfTransaction)
    val transferCharges = buildTransferCharges(transferNumber, etmpResponse, chargeType, chargeTypeDescription)

    Transfer(
      transferNumber = transferNumber,
      mainBuyersDetails = mainBuyersDetails,
      mainSellerDetails = mainSellerDetails,
      transferDetails = transferDetails,
      transferCharges = Seq(transferCharges)
    )
  }

  private def buildMainBuyersDetails(stfRequest: SingleTransferRequest): MainBuyersDetails = {
    val buyerDetails = stfRequest.mainBuyerDetails
    
    MainBuyersDetails(
      firstName = buyerDetails.buyerName,
      lastName = buyerDetails.buyerName,
      nino = buyerDetails.uniqueId.getOrElse(""),
      buyerCountry = buyerDetails.country,
      buyerAddressLine1 = buyerDetails.addr1,
      buyerAddressLine2 = buyerDetails.addr2.getOrElse(""),
      buyerAddressLine3 = buyerDetails.addr3.getOrElse(""),
      buyerAddressCity = buyerDetails.addr4.getOrElse(""),
      buyerAddressPostcode = buyerDetails.postcode
    )
  }

  private def buildMainSellerDetails(stfRequest: SingleTransferRequest): MainSellerDetails = {
    stfRequest.mainSellerDetails match {
      case Some(sellerDetails) =>
        val sellerCountry = sellerDetails.country
        val sellerInUk = sellerCountry.equalsIgnoreCase("UK") || sellerCountry.equalsIgnoreCase("GB")  || sellerCountry.equalsIgnoreCase("United Kingdom")

        MainSellerDetails(
          sellerName = sellerDetails.sellerName,
          sellerInUk = sellerInUk,
          sellerCountry = sellerCountry,
          sellerAddressLine1 = sellerDetails.addr1,
          sellerAddressLine2 = sellerDetails.addr2.getOrElse(""),
          sellerAddressLine3 = sellerDetails.addr3.getOrElse(""),
          sellerAddressCity = sellerDetails.addr4.getOrElse(""),
          sellerAddressPostcode = sellerDetails.postcode
        )
      case None =>
        MainSellerDetails(
          sellerName = "",
          sellerInUk = true,
          sellerCountry = "UK",
          sellerAddressLine1 = "",
          sellerAddressLine2 = "",
          sellerAddressLine3 = "",
          sellerAddressCity = "",
          sellerAddressPostcode = ""
        )
    }
  }

  private def buildTransferDetails(stfRequest: SingleTransferRequest, stfTransaction: StfTransaction): TransferDetails = {
    val txnDetails = stfRequest.transactionDetails
    
    TransferDetails(
      connectedPersons = txnDetails.isConnectedPartiesTransactions,
      taxRelief = txnDetails.reliefPercentage.isDefined,
      typeOfTaxRelief = txnDetails.reliefClaimedName.getOrElse(""),
      businessName = txnDetails.companyName,
      companyRegistrationNumber = txnDetails.companyRegistrationNumber.getOrElse(""),
      chargingPointDate = txnDetails.originalChargingPoint.format(dateFormatter),
      taxRate = getTaxRateValue(stfRequest.mainBuyerDetails.taxRate),
      buyerPurchasingShares = stfTransaction.purchasingShares,
      typeOfSecurity = if(stfTransaction.purchasingShares) "" else txnDetails.typeOfSecurity,
      numberOfShares = BigDecimal(txnDetails.numberOfShares),
      typeOfShares = if(stfTransaction.purchasingShares) txnDetails.typeOfSecurity else "",
      amountPaid = txnDetails.considerationActual,
      marketValue = txnDetails.marketValue.getOrElse(BigDecimal(0))
    )
  }

  private def buildTransferCharges(
    transferNumber: Int,
    etmpResponse: SubmissionCreateResponseSuccess,
    chargeType: String,
    chargeTypeDescription: String
  ): TransferCharges = {
    TransferCharges(
      transferNumber = transferNumber,
      uniqueTransferReferenceNumber = etmpResponse.utrn,
      chargeAmount = etmpResponse.taxDue,
      chargeType = chargeType,
      chargeTypeDescription = chargeTypeDescription,
      chargeReference = etmpResponse.chargeReferences.headOption.getOrElse(""),
      chargeDueDate = etmpResponse.paymentDueBy.format(dateFormatter)
    )
  }

  private def getTaxRateValue(taxRate: BuyerTaxRate): BigDecimal = {
    taxRate match {
      case BuyerTaxRate.HalfPercent => BigDecimal(0.5)
      case BuyerTaxRate.OneAndHalfPercent => BigDecimal(1.5)
    }
  }
}