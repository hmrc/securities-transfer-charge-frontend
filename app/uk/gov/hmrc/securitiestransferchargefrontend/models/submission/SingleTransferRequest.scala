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

import play.api.libs.json.{Json, Reads, Writes}

import java.time.LocalDate

final case class SingleTransferRequest(
  recordId: Int,
  transactionDetails: SingleTransferTransactionDetails,
  contingentDetails: Option[Seq[SingleTransferContingentDetails]],
  mainSellerDetails: SingleTransferSellerDetails,
  otherSellers: Option[Seq[SingleTransferOtherSellerName]],
  mainBuyerDetails: SingleTransferBuyerDetails,
  otherBuyers: Option[Seq[SingleTransferOtherBuyerName]],
  agentDetails: Option[Seq[SingleTransferAgentDetails]]
)

final case class SingleTransferTransactionDetails(
  transactionType: TransferType,
  reasonForPurchase: Option[ReasonForPurchase],
  descriptionOfSecurity: String,
  numberOfShares: Int,
  nominalValue: Option[BigDecimal],
  marketValue: Option[BigDecimal],
  qualifyAsTreasuryShares: Option[Boolean],
  maxPricePaid: Option[BigDecimal],
  minPricePaid: Option[BigDecimal],
  originalChargingPoint: LocalDate,
  considerationActual: BigDecimal,
  isConnectedPartiesTransactions: Boolean,
  companyName: String,
  companyRegistrationNumber: Option[String],
  reliefClaimedName: Option[String],
  reliefPercentage: Option[Int]
)

final case class SingleTransferContingentDetails(
  provisionalDate: LocalDate,
  isAmountUnasertainable: Boolean,
  unascertainableAmount: Option[BigDecimal],
  ascertainableAmount: Option[BigDecimal],
  defermentOfPayment: Boolean,
  originalDefermentDate: Option[LocalDate]
)

final case class SingleTransferSellerDetails(
  sellerName: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: String,
  country: String
)

final case class SingleTransferOtherSellerName(
  sellerName: String
)

final case class SingleTransferBuyerDetails(
  buyerName: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: String,
  country: String,
  email: String,
  uniqueId: Option[String],
  taxRate: BuyerTaxRate,
  isPLC: Option[Boolean]
)

final case class SingleTransferOtherBuyerName(
  buyerName: String
)

final case class SingleTransferAgentDetails(
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: String,
  country: String,
  phone: String,
  email: String,
  clientReference: String
)

final case class SingleTransferDeclaration(
  role1: Option[DeclarationRole],
  role2: Option[String],
  name: String,
  addr1: String,
  addr2: Option[String],
  addr3: Option[String],
  addr4: Option[String],
  postcode: String,
  country: String,
  selfDeclarationAgent: Option[Boolean],
  isCorrectInfo: Boolean
)

object SingleTransferRequest:
  given Reads[SingleTransferDeclaration] = Json.reads[SingleTransferDeclaration]
  given Writes[SingleTransferDeclaration] = Json.writes[SingleTransferDeclaration]

  given Reads[SingleTransferAgentDetails] = Json.reads[SingleTransferAgentDetails]
  given Writes[SingleTransferAgentDetails] = Json.writes[SingleTransferAgentDetails]

  given Reads[SingleTransferOtherBuyerName] = Json.reads[SingleTransferOtherBuyerName]
  given Writes[SingleTransferOtherBuyerName] = Json.writes[SingleTransferOtherBuyerName]

  given Reads[SingleTransferBuyerDetails] = Json.reads[SingleTransferBuyerDetails]
  given Writes[SingleTransferBuyerDetails] = Json.writes[SingleTransferBuyerDetails]

  given Reads[SingleTransferOtherSellerName] = Json.reads[SingleTransferOtherSellerName]
  given Writes[SingleTransferOtherSellerName] = Json.writes[SingleTransferOtherSellerName]

  given Reads[SingleTransferSellerDetails] = Json.reads[SingleTransferSellerDetails]
  given Writes[SingleTransferSellerDetails] = Json.writes[SingleTransferSellerDetails]

  given Reads[SingleTransferContingentDetails] = Json.reads[SingleTransferContingentDetails]
  given Writes[SingleTransferContingentDetails] = Json.writes[SingleTransferContingentDetails]

  given Reads[SingleTransferTransactionDetails] = Json.reads[SingleTransferTransactionDetails]
  given Writes[SingleTransferTransactionDetails] = Json.writes[SingleTransferTransactionDetails]

  given Reads[SingleTransferRequest] = Json.reads[SingleTransferRequest]
  given Writes[SingleTransferRequest] = Json.writes[SingleTransferRequest]
