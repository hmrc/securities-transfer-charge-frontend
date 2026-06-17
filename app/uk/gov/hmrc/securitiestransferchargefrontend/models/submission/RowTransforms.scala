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
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ValidatedStcRow

object RowTransforms {

  def fromValidatedStcRowToStfRequest(validatedRow: ValidatedStcRow, affinityData: AffinityData): SingleTransferRequest =
    require(!validatedRow.hasBlockingErrors, s"Cannot create SingleTransferRequest from row ${validatedRow.parsedRow.rowNumber} with blocking validation errors")

    val row = validatedRow.parsedRow

    val connectedPersons = required(row.connectedPersons, "connectedPersons", row.rowNumber)
    val marketValue = conditional(connectedPersons)(row.totalMarketValue, "totalMarketValue", row.rowNumber)

    val shareType = required(row.whatTypeOfSecurities, "whatTypeOfSecurities", row.rowNumber)
    val getDescriptionOfSecurity =
      if shareType.equalsIgnoreCase("Shares") then required(row.typeOfShares, "typeOfShares", row.rowNumber) else shareType

    val reliefClaimed = required(row.applyingForRelief, "applyingForRelief", row.rowNumber)
    val reliefPercentage = if reliefClaimed then Some(100) else None // TODO: Get correct percentage based on what relief is being claimed

    val buyerName: String = affinityData match
      case Individual(name, _, _, _, _) => name
      case Organisation(name, _, _, _, _) => name
      case _ => ??? // ToDo: For agents this will be in the spreadsheet in a location tbd

    val buyerAddress: stf.Address = affinityData match
      case Individual(_, address, _, _, _) => address
      case Organisation(_, address, _, _, _) => address
      case _ => ??? // ToDo: ToDo: For agents this will be in the spreadsheet in a location tbd

    val buyerEmail: String = affinityData match
      case Individual(_, _, _, email, _) => email
      case Organisation(_, _, _, email, _) => email
      case _ => ??? // ToDo: For agents this will be in the spreadsheet in a location tbd

    val uniqueId: AffinityData => Option[String] = {
      case Individual(_, _, _, _, nino) => Some(nino)
      case Organisation(_, _, _, _, utr) => Some(utr)
      case _ => None // Agents don't have to provide a unique ID for themselves or their clients, so we return None.
    }

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
        clientReference = "" // ToDo: For agents this will be in the spreadsheet in a location tbd
      )

    SingleTransferRequest(
      recordId = row.rowNumber,
      transactionDetails = SingleTransferTransactionDetails(
        transactionType = TransferType.STF,
        reasonForPurchase = None, // SH03 only
        descriptionOfSecurity = getDescriptionOfSecurity,
        numberOfShares = required(row.securitiesQuantity, "securitiesQuantity", row.rowNumber).toInt,
        nominalValue = None, // SH03 only
        marketValue = marketValue.map(s=> BigDecimal(s)),
        qualifyAsTreasuryShares = None, // SH03 only
        maxPricePaid = None, // // SH03 only
        minPricePaid = None, // // SH03 only
        originalChargingPoint = required(row.chargingPoint.toOption, "chargingPoint", row.rowNumber),
        considerationActual = required(row.amountPaidForSecurities.map(s => BigDecimal(s)), "amountPaidForSecurities", row.rowNumber),
        isConnectedPartiesTransactions = connectedPersons,
        companyName = required(row.securitiesTarget, "securitiesTarget", row.rowNumber),
        companyRegistrationNumber = row.companyRegistrationNumber,
        reliefClaimedName = conditional(reliefClaimed)(row.whatReliefAreYouApplyingFor, "whatReliefAreYouApplyingFor", row.rowNumber),
        reliefPercentage = reliefPercentage
      ),
      contingentDetails = None, // Needed once we do C&D
      mainSellerDetails = SingleTransferSellerDetails(
        sellerName = required(row.sellerName, "sellerName", row.rowNumber),
        addr1 = required(row.sellerAddressLine1, "sellerAddressLine1", row.rowNumber),
        addr2 = row.sellerAddressLine2,
        addr3 = row.sellerAddressLine3,
        addr4 = row.sellerAddressLine4,
        postcode = required(row.sellerPostcode, "sellerPostcode", row.rowNumber),
        country = sellerCountry(row.sellerAddressInUK,row.sellerCountry,row.rowNumber)
      ),
      otherSellers = None, // No way to enter multiple sellers atm
      mainBuyerDetails = SingleTransferBuyerDetails(
        buyerName = buyerName,
        addr1 = buyerAddress.addressLine1,
        addr2 = buyerAddress.addressLine2,
        addr3 = buyerAddress.addressLine3,
        addr4 = None,
        postcode = buyerAddress.postcode,
        country = buyerAddress.countryCode,
        email = buyerEmail,
        uniqueId = uniqueId(affinityData),
        taxRate = taxRateFrom(required(row.taxRate, "taxRate", row.rowNumber)),
        isPLC = None
      ),
      otherBuyers = None, // No way to enter multiple buyers atm
      agentDetails = agentDetails.lift(affinityData)
    )

  private def required[A](value: Option[A], fieldName: String, rowNumber: Int): A =
    value.getOrElse(
      throw new IllegalArgumentException(s"Missing field '$fieldName' on row $rowNumber")
    )
  
  private def conditional[A](
                              required: => Boolean
                            )(
                              value: Option[A],
                              fieldName: String,
                              rowNumber: Int
                            ): Option[A] = {

    value match {
      case Some(v) => Some(v)

      case None if !required =>
        None

      case None =>
        throw new IllegalArgumentException(
          s"Missing field '$fieldName' on row $rowNumber"
        )
    }
  }

  private def sellerCountry(sellerAddressInUK: Option[Boolean], sellerCountry: Option[String], rowNumber: Int): String =
    if (sellerAddressInUK.contains(true)) "United Kingdom" else required(sellerCountry, "sellerCountry", rowNumber)
    
  private def taxRateFrom(rate: BigDecimal): BuyerTaxRate =
    if rate == BigDecimal("1.5") then BuyerTaxRate.OneAndHalfPercent else BuyerTaxRate.HalfPercent

}
