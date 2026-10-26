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

package uk.gov.hmrc.securitiestransferchargefrontend.models.pay

import play.api.libs.json._

sealed trait ChargeType
object ChargeType {
  case object SecurityTransferCharge extends ChargeType
  case object LatePaymentPenalty extends ChargeType
  case object LatePaymentInterest extends ChargeType

  implicit val writes: Writes[ChargeType] = Writes {
    case SecurityTransferCharge => JsString("SecurityTransferCharge")
    case LatePaymentPenalty     => JsString("LatePaymentPenalty")
    case LatePaymentInterest    => JsString("LatePaymentInterest")
  }
}

final case class BasketItem(
                             buyerName: String,
                             sellerName: String,
                             chargeReference: String,
                             chargeType: ChargeType,
                             amountInPence: Long
                           )

object BasketItem {
  implicit val writes: OWrites[BasketItem] = Json.writes[BasketItem]
}

final case class BasketDetails(
                                basketItems: Seq[BasketItem]
                              )

object BasketDetails {
  implicit val writes: OWrites[BasketDetails] = Json.writes[BasketDetails]
}

final case class StartPaymentJourneyRequest(
                                             returnUrl: String,
                                             backUrl: String,
                                             customerId: String,
                                             submissionId: String,
                                             basketDetails: BasketDetails
                                           )

object StartPaymentJourneyRequest {
  implicit val writes: OWrites[StartPaymentJourneyRequest] = Json.writes[StartPaymentJourneyRequest]
}

final case class StartPaymentJourneyResponse(
                                              journeyId: String,
                                              nextUrl: String
                                            )

object StartPaymentJourneyResponse {
  implicit val reads: Reads[StartPaymentJourneyResponse] = Json.reads[StartPaymentJourneyResponse]
}