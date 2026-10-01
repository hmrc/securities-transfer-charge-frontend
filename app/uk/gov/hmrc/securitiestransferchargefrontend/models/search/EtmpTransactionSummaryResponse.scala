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

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class EtmpChargeDetail(
                             utrn: String,
                             chargeTypeDescription: String,
                             chargeReference: String,
                             chargeType: String,
                             chargeAmountTotal: BigDecimal,
                             chargeDueDate: LocalDate,
                             chargeAmountPending: BigDecimal
                           )
object EtmpChargeDetail {
  implicit val format: OFormat[EtmpChargeDetail] = Json.format[EtmpChargeDetail]
}

case class EtmpTransactionDetail(
                                  submissionId: String,
                                  submissionDate: LocalDate,
                                  clientReference: Option[String],
                                  declareeName: String,
                                  utrn: String,
                                  buyerNames: String,
                                  sellerNames: Option[String],
                                  companyName: String
                                )
object EtmpTransactionDetail {
  implicit val format: OFormat[EtmpTransactionDetail] = Json.format[EtmpTransactionDetail]
}

case class EtmpSuccessResponse(
                                processingDate: String,
                                transactionsCount: Int,
                                message: Option[String],
                                transactionDetails: Option[Seq[EtmpTransactionDetail]],
                                charges: Option[Seq[EtmpChargeDetail]]
                              )
object EtmpSuccessResponse {
  implicit val format: OFormat[EtmpSuccessResponse] = Json.format[EtmpSuccessResponse]
}

case class EtmpTransactionSummaryResponse(success: EtmpSuccessResponse)
object EtmpTransactionSummaryResponse {
  implicit val format: OFormat[EtmpTransactionSummaryResponse] = Json.format[EtmpTransactionSummaryResponse]
}