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

import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubmissionId}

case class AuditModel(
                       journeyStatus: JourneyStatus,
                       internalId: String,
                       affinityGroup: AffinityGroup,
                       credentialId: CredentialId,
                       submissionId: SubmissionId,
                     ) extends JsonAuditModel {

  override val auditType: String = "StockTransferFormStatus"

  override val detail: JsObject = Json.obj(
    "journeyStatus" -> journeyStatus.toString,
    "internalId" -> internalId,
    "affinityGroup" -> affinityGroup,
    "credentialId" -> credentialId,
    "submissionId" -> submissionId
  )
}

object AuditModel {

  def build(
             journeyStatus: JourneyStatus,
             internalId: String,
             affinityGroup: AffinityGroup,
             credentialId: CredentialId,
             submissionId: SubmissionId
           ): AuditModel = AuditModel(journeyStatus, internalId, affinityGroup, credentialId, submissionId)
}