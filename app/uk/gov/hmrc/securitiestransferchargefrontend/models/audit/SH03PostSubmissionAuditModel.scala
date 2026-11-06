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

import play.api.libs.json.{JsObject, Json, OFormat, OWrites}

case class SH03PostSubmissionAuditDetail(
  declarantRole1: String,
  declarantRole2: String,
  reference: String,
  declarantName: String,
  declarantAddressLine1: String,
  declarantAddressLine2: String,
  declarantAddressLine3: String,
  declarantAddressCity: String,
  declarantPostcode: String,
  declarantCountry: String,
  affinityGroup: String,
  selfDeclarationAgent: Boolean,
  isCorrectInfo: Boolean,
  transferMethod: String,
  subscriptionId: String,
  submissionId: String,
  credentialId: String,
  totalBatches: Int,
  totalTransfers: Int,
  currentBatch: Int,
  versionNumber: Int,
  transfers: Seq[SH03Transfer]
)

object SH03PostSubmissionAuditDetail {
  implicit val writes: OWrites[SH03PostSubmissionAuditDetail] = Json.writes[SH03PostSubmissionAuditDetail]
}

case class SH03PostSubmissionAuditModel(
  auditDetail: SH03PostSubmissionAuditDetail
) extends JsonAuditModel {
  override val auditType: String = "PurchaseOfOwnSharesSubmission"
  override val detail: JsObject = Json.toJsObject(auditDetail)
}

object SH03PostSubmissionAuditModel {
  implicit val writes: OWrites[SH03PostSubmissionAuditModel] = Json.writes[SH03PostSubmissionAuditModel]
}

case class SH03Transfer(
  transferNumber: Int,
  transactionDetails: SH03TransactionDetails,
  mainBuyerDetails: SH03MainBuyerDetails,
  transferCharges: Seq[SH03TransferCharge]
)

object SH03Transfer {
  implicit val format: OFormat[SH03Transfer] = Json.format[SH03Transfer]
}

case class SH03TransactionDetails(
  transactionType: Int,
  reasonForPurchase: String,
  typeOfSecurity: String,
  numberOfShares: BigDecimal,
  nominalValue: String,
  marketValue: BigDecimal,
  qualifyAsTreasuryShares: Boolean,
  maxPricePaid: BigDecimal,
  minPricePaid: BigDecimal,
  originalChargingPoint: String,
  considerationActual: BigDecimal,
  isConnectedPartiesTransactions: Boolean,
  companyName: String,
  companyRegistrationNumber: String,
  reliefClaimedName: String,
  reliefPercentage: Int
)

object SH03TransactionDetails {
  implicit val format: OFormat[SH03TransactionDetails] = Json.format[SH03TransactionDetails]
}

case class SH03MainBuyerDetails(
  mainBuyerName: String,
  mainBuyerAddressLine1: String,
  mainBuyerAddressLine2: String,
  mainBuyerAddressLine3: String,
  mainBuyerAddressCity: String,
  postcode: String,
  country: String,
  email: String,
  uniqueId: String,
  taxRate: Int,
  isPublicLimitedCompany: Boolean
)

object SH03MainBuyerDetails {
  implicit val format: OFormat[SH03MainBuyerDetails] = Json.format[SH03MainBuyerDetails]
}

case class SH03TransferCharge(
  transferNumber: Int,
  uniqueTransferReferenceNumber: String,
  chargeAmount: BigDecimal,
  chargeTypeDescription: String,
  chargeReference: String,
  chargeDueDate: String
)

object SH03TransferCharge {
  implicit val format: OFormat[SH03TransferCharge] = Json.format[SH03TransferCharge]
}