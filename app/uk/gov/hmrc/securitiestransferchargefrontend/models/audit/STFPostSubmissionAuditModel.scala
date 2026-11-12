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

import play.api.libs.json.{JsObject, Json, OFormat, OWrites}

case class MainBuyersDetails(
  firstName: String,
  lastName: String,
  nino: String,
  buyerCountry: String,
  buyerAddressLine1: String,
  buyerAddressLine2: String,
  buyerAddressLine3: String,
  buyerAddressLine4: String,
  buyerAddressPostcode: String
)

object MainBuyersDetails {
  implicit val format: OFormat[MainBuyersDetails] = Json.format[MainBuyersDetails]
}

case class MainSellerDetails(
  sellerName: String,
  sellerInUk: Boolean,
  sellerCountry: String,
  sellerAddressLine1: String,
  sellerAddressLine2: String,
  sellerAddressLine3: String,
  sellerAddressLine4: String,
  sellerAddressPostcode: String
)

object MainSellerDetails {
  implicit val format: OFormat[MainSellerDetails] = Json.format[MainSellerDetails]
}

case class TransferDetails(
  connectedPersons: Boolean,
  taxRelief: Boolean,
  typeOfTaxRelief: String,
  businessName: String,
  companyRegistrationNumber: String,
  chargingPointDate: String,
  taxRate: BigDecimal,
  buyerPurchasingShares: Boolean,
  typeOfSecurity: String,
  numberOfShares: BigDecimal,
  typeOfShares: String,
  amountPaid: BigDecimal,
  marketValue: BigDecimal
)

object TransferDetails {
  implicit val format: OFormat[TransferDetails] = Json.format[TransferDetails]
}

case class TransferCharges(
  transferNumber: Int,
  uniqueTransferReferenceNumber: String,
  chargeAmount: BigDecimal,
  chargeType: String,
  chargeTypeDescription: String,
  chargeReference: String,
  chargeDueDate: String
)

object TransferCharges {
  implicit val format: OFormat[TransferCharges] = Json.format[TransferCharges]
}

case class Transfer(
  transferNumber: Int,
  mainBuyersDetails: MainBuyersDetails,
  mainSellerDetails: MainSellerDetails,
  transferDetails: TransferDetails,
  transferCharges: Seq[TransferCharges]
)

object Transfer {
  implicit val format: OFormat[Transfer] = Json.format[Transfer]
}

case class STFPostSubmissionAuditDetail(
  declarantName: String,
  declarantAddressLine1: String,
  declarantAddressLine2: String,
  declarantAddressLine3: String,
  declarantAddressLine4: String,
  declarantAddressPostcode: String,
  declarantCountry: String,
  transferMethod: String,
  subscriptionId: String,
  submissionId: String,
  credentialId: String,
  affinityGroup: String,
  totalBatches: Int,
  totalTransfers: Int,
  currentBatch: Int,
  versionNumber: Int,
  declarantRole1: String,
  declarantRole2: String,
  reference: String,
  transfers: Seq[Transfer]
)

object STFPostSubmissionAuditDetail {
  implicit val writes: OWrites[STFPostSubmissionAuditDetail] = Json.writes[STFPostSubmissionAuditDetail]
}

case class STFPostSubmissionAuditModel(
  auditDetail: STFPostSubmissionAuditDetail
) extends JsonAuditModel {
  override val auditType: String = "StockTransferFormSubmission"
  override val detail: JsObject = Json.toJsObject(auditDetail)
}

object STFPostSubmissionAuditModel {
  implicit val writes: OWrites[STFPostSubmissionAuditModel] = Json.writes[STFPostSubmissionAuditModel]
}