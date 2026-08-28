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

import play.api.libs.json.*

sealed trait StcTransactionCreateResponse

final case class StcTransactionCreateProcessed(success: StcTransactionCreateProcessedBody)
  extends StcTransactionCreateResponse

final case class StcTransactionCreateProcessedBody(
  processingDate: String,
  charges: List[StcCharge]
)

sealed trait StcCharge:
  def recordId: Int

final case class StcChargeSuccess(
  recordId: Int,
  utrn: String,
  chargeTypeDescription: String,
  chargeReference: String,
  chargeType: String,
  chargeAmount: BigDecimal,
  chargeDueDate: String
) extends StcCharge

final case class StcChargeFailure(
  recordId: Int,
  errorCode: String,
  errorText: String
) extends StcCharge

final case class StcTransactionCreateBadRequest(error: StcTransactionCreateBadRequestBody)
  extends StcTransactionCreateResponse

final case class StcTransactionCreateBadRequestBody(
  code: String,
  message: String,
  logID: String
)

final case class StcTransactionCreateBusinessError(errors: StcTransactionCreateBusinessErrorBody)
  extends StcTransactionCreateResponse

final case class StcTransactionCreateBusinessErrorBody(
  processingDate: String,
  code: String,
  text: String
)

object StcTransactionCreateResponse:

  given Reads[StcChargeSuccess] = Json.reads[StcChargeSuccess]
  given Reads[StcChargeFailure] = Json.reads[StcChargeFailure]

  given Reads[StcCharge] = Reads { json =>
    (json \ "errorCode", json \ "errorText") match
      case (JsDefined(_), JsDefined(_)) =>
        summon[Reads[StcChargeFailure]].reads(json)
      case _ =>
        summon[Reads[StcChargeSuccess]].reads(json)
  }

  given Reads[StcTransactionCreateProcessedBody] = Json.reads[StcTransactionCreateProcessedBody]
  given Reads[StcTransactionCreateProcessed] = Json.reads[StcTransactionCreateProcessed]

  given Reads[StcTransactionCreateBadRequestBody] = Json.reads[StcTransactionCreateBadRequestBody]
  given Reads[StcTransactionCreateBadRequest] = Json.reads[StcTransactionCreateBadRequest]

  given Reads[StcTransactionCreateBusinessErrorBody] = Json.reads[StcTransactionCreateBusinessErrorBody]
  given Reads[StcTransactionCreateBusinessError] = Json.reads[StcTransactionCreateBusinessError]

  given Reads[StcTransactionCreateResponse] = Json.reads[StcTransactionCreateResponse]