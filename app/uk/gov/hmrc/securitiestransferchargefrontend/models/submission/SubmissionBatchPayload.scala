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

import play.api.libs.json.{Json, OWrites, Reads}

final case class SubmissionBatchPayload private(
  declaration: Option[SingleTransferDeclaration],
  transfers: Seq[SingleTransferRequest]
)

object SubmissionBatchPayload:
  import SingleTransferRequest.given
  given Reads[SubmissionBatchPayload] = Json.reads[SubmissionBatchPayload]
  given OWrites[SubmissionBatchPayload] = Json.writes[SubmissionBatchPayload]

  /*
   * STOSB-2607: While we wait for the ETMP EPID to be updated to allow the declaration
   * to be empty for STF, we are introducing a temporary declaration as a placeholder.
   * Once the EPID and OAD are updated, this can be removed and the stfPayload constructor
   * will be able to pass None as the declaration.
   */
  private val tempDeclaration =
    SingleTransferDeclaration(
      role1 = None,
      role2 = None,
      name = "Unknown",
      addr1 = "Unknown",
      addr2 = None,
      addr3 = None,
      addr4 = None,
      postcode = "Unknown",
      country = "Unknown",
      selfDeclarationAgent = None,
      isCorrectInfo = true
    )
  
  def stfPayload(transfers: Seq[SingleTransferRequest]): SubmissionBatchPayload =
    new SubmissionBatchPayload(Some(tempDeclaration), transfers)

  def sh03Payload(declaration: SingleTransferDeclaration, transfers: Seq[SingleTransferRequest]) =
    new SubmissionBatchPayload(Some(declaration), transfers)
