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

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ReasonForPurchase.{Both, PurchasedForCancellation, PurchasedToPlaceIntoTreasury}

import java.time.LocalDate

object DataExtractors {

  private[submission] def required[A](value: Option[A], fieldName: String, rowNumber: Int): A =
    value.getOrElse(
      throw new IllegalArgumentException(s"Missing field '$fieldName' on row $rowNumber")
    )

  private[submission] def conditional[A](required: => Boolean)
                            (value: Option[A], fieldName: String, rowNumber: Int): Option[A] = {

    value match {
      case Some(v) => Some(v)
      case None if !required => None
      case None =>
        throw new IllegalArgumentException(
          s"Missing field '$fieldName' on row $rowNumber"
        )
    }
  }

  private type DataExtractor[A] = ParsedStcRow => A
  private type OptionalDataExtractor[A] = ParsedStcRow => Option[A]
  
  private[submission] val connectedPersons: DataExtractor[Boolean] =
    row => required(row.connectedPersons, "connectedPersons", row.rowNumber)
    
  private[submission] val marketValue: OptionalDataExtractor[BigDecimal] =
    row => conditional(connectedPersons(row))(row.totalMarketValue.map(BigDecimal.apply), "totalMarketValue", row.rowNumber)

  private[submission] val shareType: DataExtractor[String] =
    row => required(row.whatTypeOfSecurities, "whatTypeOfSecurities", row.rowNumber)
    
  private[submission] val reliefClaimed: DataExtractor[Boolean] =
    row => required(row.applyingForRelief, "applyingForRelief", row.rowNumber)
    
  private[submission] val reliefPercentage: OptionalDataExtractor[Int] =
    row => if reliefClaimed(row) then Some(100) else None // TODO: Get correct percentage based on what relief is being claimed

  private[submission] val getNumberOfShares: DataExtractor[Int] =
    row => required(row.securitiesQuantity, "securitiesQuantity", row.rowNumber).toInt

  private[submission] val getOriginalChargingPoint: DataExtractor[LocalDate] =
    row => required(row.chargingPoint.toOption, "chargingPoint", row.rowNumber)

  private[submission] val getConsiderationActual: DataExtractor[BigDecimal] =
    row => required(row.amountPaidForSecurities.map(BigDecimal.apply), "amountPaidForSecurities", row.rowNumber)

  private[submission] val getReliefClaimedName: OptionalDataExtractor[String] =
    row => conditional(reliefClaimed(row))(row.whatReliefAreYouApplyingFor, "whatReliefAreYouApplyingFor", row.rowNumber)

  private[submission] val getMainSellerDetails: DataExtractor[SingleTransferSellerDetails] = row =>
    SingleTransferSellerDetails(
      sellerName = required(row.sellerName, "sellerName", row.rowNumber),
      addr1 = required(row.sellerAddressLine1, "sellerAddressLine1", row.rowNumber),
      addr2 = row.sellerAddressLine2,
      addr3 = row.sellerAddressLine3,
      addr4 = row.sellerAddressLine4,
      postcode = required(row.sellerPostcode, "sellerPostcode", row.rowNumber),
      country = sellerCountry(row.sellerAddressInUK, row.sellerCountry, row.rowNumber)
    )
  
  private[submission] val buyerName: AffinityData => String =
    case Individual(name, _, _, _) => name
    case Organisation(name, _, _, _) => name
    case _ => ??? // ToDo: For agents this will be in the spreadsheet in a location tbd

  private[submission] val buyerAddress: AffinityData => stf.Address =
    case Individual(_, address, _, _) => address
    case Organisation(_, address, _, _) => address
    case _ => ??? // ToDo: ToDo: For agents this will be in the spreadsheet in a location tbd

  private[submission] val buyerEmail: AffinityData => String =
    case Individual(_, _, email, _) => email
    case Organisation(_, _, email, _) => email
    case _ => ??? // ToDo: For agents this will be in the spreadsheet in a location tbd

  private[submission] val uniqueId: AffinityData => Option[String] = {
    case Individual(_, _, _, nino) => Some(nino)
    case Organisation(_, _, _, utr) => Some(utr)
    case _ => None // Agents don't have to provide a unique ID for themselves or their clients, so we return None.
  }

  private[submission] val agentDetails: Option[String] => PartialFunction[AffinityData, SingleTransferAgentDetails] = agentReference =>
    case Agent(name, address, phone, email) => SingleTransferAgentDetails(
      name = name,
      addr1 = Some(address.addressLine1),
      addr2 = address.addressLine2,
      addr3 = address.addressLine3,
      addr4 = None,
      postcode = address.postcode,
      country = address.countryCode,
      phone = phone,
      email = email,
      clientReference = agentReference
    )

  private[submission] def sellerCountry(sellerAddressInUK: Option[Boolean], sellerCountry: Option[String], rowNumber: Int): String =
    if (sellerAddressInUK.contains(true)) "United Kingdom" else required(sellerCountry, "sellerCountry", rowNumber)

  private[submission] def taxRateFrom(rate: BigDecimal): BuyerTaxRate =
    if rate == BigDecimal("1.5") then BuyerTaxRate.OneAndHalfPercent else BuyerTaxRate.HalfPercent

  private[submission] def reasonForPurchaseFrom(reasonForPurchase: String, qualifyAsTreasuryShares: Boolean): ReasonForPurchase =
    (reasonForPurchase.equalsIgnoreCase("cancellation"), qualifyAsTreasuryShares) match {
      case (true, true) => Both
      case (true, false) => PurchasedForCancellation
      case (false, _) => PurchasedToPlaceIntoTreasury
    }


}
