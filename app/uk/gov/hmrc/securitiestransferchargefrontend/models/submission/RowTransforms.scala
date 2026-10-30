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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.models.sh03.bulk.CompanyDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow

object RowTransforms {

  import DataExtractors.*

  def fromValidatedStcRowToStfRequest(parsedRow: ParsedStcRow, affinityData: AffinityData,agentReference:Option[String]): SingleTransferRequest =

    def getDescriptionOfSecurity: String =
      val shType = shareType(parsedRow)
      if shType.equalsIgnoreCase("Shares")
        then required(parsedRow.typeOfShares, "typeOfShares", parsedRow.rowNumber)
        else shType

    SingleTransferRequest(
      recordId = parsedRow.rowNumber,
      transactionDetails = SingleTransferTransactionDetails(
        transactionType = TransferType.STF,
        reasonForPurchase = None, // SH03 only
        typeOfSecurity = getDescriptionOfSecurity,
        numberOfShares = getNumberOfShares(parsedRow),
        nominalValue = None, // SH03 only
        marketValue = marketValue(parsedRow),
        qualifyAsTreasuryShares = None, // SH03 only
        maxPricePaid = None, // // SH03 only
        minPricePaid = None, // // SH03 only
        originalChargingPoint = getOriginalChargingPoint(parsedRow),
        considerationActual = getConsiderationActual(parsedRow),
        isConnectedPartiesTransactions = connectedPersons(parsedRow),
        companyName = required(parsedRow.securitiesTarget, "securitiesTarget", parsedRow.rowNumber),
        companyRegistrationNumber = parsedRow.companyRegistrationNumber,
        reliefClaimedName = getReliefClaimedName(parsedRow),
        reliefPercentage = reliefPercentage(parsedRow)
      ),
      contingentDetails = None, // Needed once we do C&D
      mainSellerDetails = Some(getMainSellerDetails(parsedRow)),
      otherSellers = None, // No way to enter multiple sellers atm
      mainBuyerDetails = SingleTransferBuyerDetails(
        buyerName = buyerName(affinityData),
        addr1 = buyerAddress(affinityData).addressLine1,
        addr2 = buyerAddress(affinityData).addressLine2,
        addr3 = buyerAddress(affinityData).addressLine3,
        addr4 = None,
        postcode = buyerAddress(affinityData).postcode,
        country = buyerAddress(affinityData).countryCode,
        email = buyerEmail(affinityData),
        uniqueId = uniqueId(affinityData),
        taxRate = taxRateFrom(required(parsedRow.taxRate, "taxRate", parsedRow.rowNumber)),
        isPLC = None
      ),
      otherBuyers = None, // No way to enter multiple buyers atm
      agentDetails = agentDetails(agentReference).lift(affinityData)
    )

  def fromValidatedStcRowToSh03Request(companyDetails: CompanyDetails)(parsedRow: ParsedStcRow, affinityData: AffinityData,agentReference: Option[String] = None): SingleTransferRequest =

    SingleTransferRequest(
      recordId = parsedRow.rowNumber,
      transactionDetails = SingleTransferTransactionDetails(
        transactionType = TransferType.SH03,
        reasonForPurchase = parsedRow.sharePurchaseReason.map { reason =>
          reasonForPurchaseFrom(reason, required(parsedRow.purchaseForCancellation, "purchaseForCancellation", parsedRow.rowNumber))
        },
        typeOfSecurity = shareType(parsedRow),
        numberOfShares = getNumberOfShares(parsedRow),
        nominalValue = None,
        marketValue = marketValue(parsedRow),
        qualifyAsTreasuryShares = parsedRow.purchaseForCancellation,
        maxPricePaid = parsedRow.maxSharePrice.map(s => BigDecimal(s)),
        minPricePaid = parsedRow.minSharePrice.map(s => BigDecimal(s)),
        originalChargingPoint = getOriginalChargingPoint(parsedRow),
        considerationActual = getConsiderationActual(parsedRow),
        isConnectedPartiesTransactions = connectedPersons(parsedRow),
        companyName = companyDetails.companyName,
        companyRegistrationNumber = Some(companyDetails.companyRegistrationNumber),
        reliefClaimedName = getReliefClaimedName(parsedRow),
        reliefPercentage = reliefPercentage(parsedRow)
      ),
      contingentDetails = None, // Needed once we do C&D
      mainSellerDetails = Some(getMainSellerDetails(parsedRow)),
      otherSellers = None, 
      mainBuyerDetails = SingleTransferBuyerDetails(
        buyerName = buyerName(affinityData),
        addr1 = buyerAddress(affinityData).addressLine1,
        addr2 = buyerAddress(affinityData).addressLine2,
        addr3 = buyerAddress(affinityData).addressLine3,
        addr4 = None,
        postcode = buyerAddress(affinityData).postcode,
        country = buyerAddress(affinityData).countryCode,
        email = buyerEmail(affinityData),
        uniqueId = uniqueId(affinityData),
        taxRate = BuyerTaxRate.HalfPercent,
        isPLC = None // this will come from the company details page
      ),
      otherBuyers = None,
      agentDetails = agentDetails(agentReference).lift(affinityData)
    )
  
}
