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

import java.time.LocalDate

/**
 * A previously submitted transaction, as returned by the submissions service's
 * `GET /submission/:utrn`. Reuses the request models sent to `POST /submission/:submissionId`,
 * so a transaction reads back in the shape it was submitted.
 */
final case class ViewTransferResponse(
  processingDate: String,
  submissionId: String,
  submissionDate: LocalDate,
  recordId: Int,
  transactionDetails: SingleTransferTransactionDetails,
  contingentDetails: Seq[ViewTransferContingentDetails],
  mainSellerDetails: Option[SingleTransferSellerDetails],
  otherSellers: Seq[SingleTransferOtherSellerName],
  mainBuyerDetails: SingleTransferBuyerDetails,
  otherBuyers: Seq[SingleTransferOtherBuyerName],
  agentDetails: Option[SingleTransferAgentDetails],
  declaration: SingleTransferDeclaration,
  charges: Seq[ViewTransferCharge]
)

/**
 * Contingent details as submitted, plus the values only set by a later amend
 * (`provisionalId` identifies the record; `actual*` and the extension date hold amended values).
 */
final case class ViewTransferContingentDetails(
  provisionalId: String,
  provisionalDate: LocalDate,
  isAmountUnascertainable: Boolean,
  unascertainableAmount: Option[BigDecimal],
  ascertainableAmount: Option[BigDecimal],
  defermentOfPayment: Boolean,
  originalDefermentDate: Option[LocalDate],
  actualDate: Option[LocalDate],
  unascertainableAmountActual: Option[BigDecimal],
  ascertainableAmountActual: Option[BigDecimal],
  defermentExtensionDate: Option[LocalDate]
)

final case class ViewTransferCharge(
  chargeTypeDescription: String,
  chargeReference: String,
  chargeType: String,
  chargeAmount: BigDecimal,
  chargeDueDate: String
)

object ViewTransferResponse:
  import SingleTransferRequest.given

  // The submissions service (and ETMP) spell this field without the "c".
  private val BackendUnascertainableKey = "isAmountUnasertainable"

  private val derivedContingentReads: Reads[ViewTransferContingentDetails] = Json.reads[ViewTransferContingentDetails]

  given Reads[ViewTransferContingentDetails] = Reads {
    case obj: JsObject =>
      val renamed = (obj \ BackendUnascertainableKey).toOption.fold(obj) { value =>
        (obj - BackendUnascertainableKey) + ("isAmountUnascertainable" -> value)
      }
      derivedContingentReads.reads(renamed)
    case other => JsError(s"Expected a JSON object for contingent details, got: $other")
  }

  given Reads[ViewTransferCharge] = Json.reads[ViewTransferCharge]
  given Reads[ViewTransferResponse] = Json.reads[ViewTransferResponse]

/** Outcome of looking up a transaction by UTRN. */
enum GetTransactionResult:
  case Found(transaction: ViewTransferResponse)
  case StcIdNotFound
  case UtrnNotFound
