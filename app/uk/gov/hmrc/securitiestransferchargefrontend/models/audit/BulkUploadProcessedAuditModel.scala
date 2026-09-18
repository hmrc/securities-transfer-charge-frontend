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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubscriptionId}

case class BulkUploadProcessedAuditModel(
                                          uploadJourney : String,
                                          affinityGroup : String,
                                          subscriptionId : SubscriptionId,
                                          credentialId : CredentialId,
                                          fileType : String,
                                          fileUploadStatus : String,
                                          fileSize : String,
                                          fileValidationTime: Long,
                                          fileName : String,
                                          fileReference : String,
                                          numberOfEntries : Option[Int] = None,
                                          errorType : Option[String] = None,
                                          volume : Option[String] = None,
                                          stcAuditType : AuditType = AuditType.BulkUploadProcessed
                                        ) extends JsonAuditModel {

  override val auditType: String = stcAuditType.value

  override val detail: JsObject = {

    val baseDetail = Json.obj(
      "uploadJourney"      -> uploadJourney,
      "affinityGroup"      -> affinityGroup,
      "subscriptionId"     -> subscriptionId,
      "credentialId"       -> credentialId,
      "fileType"           -> fileType,
      "fileUploadStatus"   -> fileUploadStatus,
      "fileSize"           -> fileSize,
      "fileValidationTime" -> fileValidationTime,
      "fileName"           -> fileName,
      "fileReference"      -> fileReference
    )

    val withNumberOfEntries = numberOfEntries.fold(baseDetail) { n =>
      baseDetail ++ Json.obj("numberOfEntries" -> n)
    }

    val withErrorType = errorType.fold(withNumberOfEntries) { e =>
      withNumberOfEntries ++ Json.obj("errorType" -> e)
    }

    volume.fold(withErrorType) { v =>
      withErrorType ++ Json.obj("volume" -> v)
    }
  }
}