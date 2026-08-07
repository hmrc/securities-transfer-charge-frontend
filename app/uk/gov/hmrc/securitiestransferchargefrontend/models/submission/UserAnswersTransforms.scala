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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.{Address, AlfAddress, TaxRate}
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.shared.ReasonForPurchase
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ReasonForPurchase as etmpReasonForPurchase

object UserAnswersTransforms {

  def toStfRequest(stfTransaction: StfTransaction, affinityData: AffinityData): SingleTransferRequest = {

    val agentDetails: PartialFunction[AffinityData, SingleTransferAgentDetails] =
      case Agent(name, address, phone, email) => SingleTransferAgentDetails(
        name = name,
        addr1 = address.addressLine1,
        addr2 = address.addressLine2,
        addr3 = address.addressLine3,
        addr4 = None,
        postcode = address.postcode,
        country = address.countryCode,
        phone = phone,
        email = email,
        clientReference = stfTransaction.agentReference.flatMap(_.agentReference)
      )

    val buyerName: String = affinityData match
      case Individual(name, _, _, _, _) => name
      case Organisation(name, _, _, _, _) => name
      case Agent(_, _, _, _) => getBuyerName(stfTransaction)

    val buyerAddress: AlfAddress = getBuyerAddress(stfTransaction)

    val buyerEmail: String = affinityData match
      case Individual(_, _, _, email, _) => email
      case Organisation(_, _, _, email, _) => email
      case Agent(_, _, _, _) => ??? // ToDo: We currently do not capture the buyer's email for agents. We need to capture this info.

    val uniqueId: AffinityData => Option[String] = {
      case Individual(_, _, _, _, nino) => Some(nino)
      case Organisation(_, _, _, _, utr) => Some(utr)
      case _ => None // Agents don't have to provide a unique ID for themselves or their clients, so we return None.
    }

    SingleTransferRequest(
      recordId = 1,
      transactionDetails = SingleTransferTransactionDetails(
        transactionType = STF,
        reasonForPurchase = None,
        descriptionOfSecurity = getDescriptionOfShares(stfTransaction),
        numberOfShares = getNumberOfShares(stfTransaction), //Need to capture the quantity if the user answer No to purchasing shares
        nominalValue = None,
        marketValue = stfTransaction.totalMarketValue,
        qualifyAsTreasuryShares = None,
        maxPricePaid = None,
        minPricePaid = None,
        originalChargingPoint = stfTransaction.chargingPoint,
        considerationActual = getConsideration(stfTransaction),
        isConnectedPartiesTransactions = stfTransaction.connectedPersons,
        companyName = stfTransaction.securitiesTarget.businessName,
        companyRegistrationNumber = stfTransaction.securitiesTarget.crn,
        reliefClaimedName = stfTransaction.whatReliefAreYouApplyingFor,
        reliefPercentage = if (stfTransaction.applyingForRelief) Some(100) else None // TODO: Get correct percentage based on what relief is being claimed
      ),
      contingentDetails = None,
      mainSellerDetails = Some(SingleTransferSellerDetails(
        sellerName = stfTransaction.nameOfSeller,
        addr1 = getAddressLine1(stfTransaction.sellerAddress.address),
        addr2 = stfTransaction.sellerAddress.address.lines.lift(1),
        addr3 = stfTransaction.sellerAddress.address.lines.lift(2),
        addr4 = stfTransaction.sellerAddress.address.lines.lift(3),
        postcode = stfTransaction.sellerAddress.address.postcode,
        country = stfTransaction.sellerAddress.address.country.name)),
      otherSellers = None,
      mainBuyerDetails = SingleTransferBuyerDetails(buyerName = buyerName,
        addr1 = getAddressLine1(buyerAddress),
        addr2 = buyerAddress.lines.lift(1),
        addr3 = buyerAddress.lines.lift(2),
        addr4 = buyerAddress.lines.lift(3),
        postcode = buyerAddress.postcode,
        country = buyerAddress.country.name,
        email = buyerEmail,
        uniqueId = uniqueId(affinityData),
        taxRate = getTaxRate(stfTransaction.taxRate),
        isPLC = None),
      otherBuyers = None,
      agentDetails = agentDetails.lift(affinityData)
    )
  }

  def toSh03Request(sh03Transaction: Sh03Transaction, affinityData: AffinityData): SingleTransferRequest = {

    val agentDetails: PartialFunction[AffinityData, SingleTransferAgentDetails] =
      case Agent(name, address, phone, email) => SingleTransferAgentDetails(
        name = name,
        addr1 = address.addressLine1,
        addr2 = address.addressLine2,
        addr3 = address.addressLine3,
        addr4 = None,
        postcode = address.postcode,
        country = address.countryCode,
        phone = phone,
        email = email,
        clientReference = sh03Transaction.agentReference.flatMap(_.agentReference)
      )

    val buyerName: String = affinityData match
      case Organisation(name, _, _, _, _) => name // is this the Org name or the contact name from their Subscription
      case _ => ??? // TODO we need to capture the buyer name for agents

    val buyerAddress: Address = affinityData match {
      case Organisation(_, address, _, _, _) => address
      case _ => ??? // TODO we need to capture the buyer address for agents
    }

    val buyerEmail: String = affinityData match
      case Organisation(_, _, _, email, _) => email
      case _ => ??? // TODO we need to capture the buyer email for agents

    val uniqueId: AffinityData => Option[String] = {
      case Individual(_, _, _, _, nino) => Some(nino)
      case Organisation(_, _, _, _, utr) => Some(utr)
      case _ => None // Agents don't have to provide a unique ID for themselves or their clients, so we return None.
    }

    SingleTransferRequest(
      recordId = 1,
      transactionDetails = SingleTransferTransactionDetails(
        transactionType = SH03,
        reasonForPurchase = Some(getReasonForPurchase(sh03Transaction.reasonForPurchase, sh03Transaction.treasuryShares)),
        descriptionOfSecurity = sh03Transaction.detailsOfThisSharePurchase.typeOfShares,
        numberOfShares = sh03Transaction.detailsOfThisSharePurchase.numberOfShares,
        nominalValue = None, // TODO we might need to capture this for sh03
        marketValue = sh03Transaction.detailsOfThisSharePurchase.marketValue,
        qualifyAsTreasuryShares = sh03Transaction.treasuryShares,
        maxPricePaid = sh03Transaction.maximumAmountPaid,
        minPricePaid = sh03Transaction.minimumAmountPaid,
        originalChargingPoint = sh03Transaction.chargingPoint,
        considerationActual = sh03Transaction.detailsOfThisSharePurchase.amountPaid,
        isConnectedPartiesTransactions = sh03Transaction.connectedPersons,
        companyName = sh03Transaction.companyDetails.companyName,
        companyRegistrationNumber = Some(sh03Transaction.companyDetails.companyRegistrationNumber),
        reliefClaimedName = sh03Transaction.whatReliefAreYouApplyingFor,
        reliefPercentage = if (sh03Transaction.applyingForRelief) Some(100) else None // TODO: Get correct percentage based on what relief is being claimed
      ),
      contingentDetails = None,
      mainSellerDetails = None, //seller details are not required for sh03
      otherSellers = None,
      mainBuyerDetails = SingleTransferBuyerDetails(buyerName = buyerName,
        addr1 = buyerAddress.addressLine1,
        addr2 = buyerAddress.addressLine2,
        addr3 = buyerAddress.addressLine3,
        addr4 = None,
        postcode = buyerAddress.postcode,
        country = buyerAddress.countryCode,
        email = buyerEmail,
        uniqueId = uniqueId(affinityData),
        taxRate = BuyerTaxRate.HalfPercent,
        isPLC = Some(sh03Transaction.companyDetails.isPlc)),
      otherBuyers = None,
      agentDetails = agentDetails.lift(affinityData)
    )
  }

  private def getConsideration(transaction: StfTransaction): BigDecimal =
    transaction.detailsOfThisTransfer.fold {
      transaction.amountPaidForSecurities.getOrElse {
        throw new IllegalArgumentException("Amount paid is missing")
      }
    }(_.amountPaid)

  private def getDescriptionOfShares(transaction: StfTransaction): String =
    transaction.detailsOfThisTransfer.fold {
      transaction.otherSecuritiesType.getOrElse {
        throw new IllegalArgumentException("Description of shares is missing")
      }
    }(_.typeOfShares)

  private def getAddressLine1(address: AlfAddress): String =
    address.lines match {
      case line1 :: _ => line1
      case Nil => throw new IllegalArgumentException("Address line 1 is missing")
    }

  private def getBuyerAddress(transaction: StfTransaction): AlfAddress =
    transaction.confirmedAddress.fold {
      transaction.buyerAddress
        .map(_.address)
        .getOrElse {
          throw new IllegalArgumentException("Buyer address is missing")
        }
    } { confirmedAddress =>
      AlfAddress(
        lines = confirmedAddress.lines,
        postcode = confirmedAddress.postcode,
        country = confirmedAddress.country.getOrElse {
          throw new IllegalArgumentException("Buyer country is missing")
        }
      )
    }


  private def getBuyerName(transaction: StfTransaction): String =
    transaction.nameofBuyer.getOrElse(throw new IllegalArgumentException("Buyer name is missing"))

  private def getTaxRate(rate: TaxRate): BuyerTaxRate =
    rate match {
      case TaxRate.HalfPercent => BuyerTaxRate.HalfPercent
      case TaxRate.OneAndHalfPercent => BuyerTaxRate.OneAndHalfPercent
    }

  private def getNumberOfShares(transaction: StfTransaction): Int =
    transaction.detailsOfThisTransfer.fold(1)(_.numberOfShares)

  private def getReasonForPurchase(reasonForPurchase: ReasonForPurchase, treasuryShares: Option[Boolean]): etmpReasonForPurchase =
    (reasonForPurchase, treasuryShares) match {
      case (ReasonForPurchase.ForCancellation, Some(true)) => etmpReasonForPurchase.Both

      case (ReasonForPurchase.ForCancellation, _) => etmpReasonForPurchase.PurchasedForCancellation

      case (ReasonForPurchase.ToPlaceIntoTreasury, _) => etmpReasonForPurchase.PurchasedToPlaceIntoTreasury
    }
}
