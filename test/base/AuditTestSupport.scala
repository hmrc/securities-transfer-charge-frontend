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

package base

import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubmissionId, SubscriptionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, AuditType, BulkUploadProcessedAuditModel, JourneyStatus, UpscanValidationAuditModel}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService

trait AuditTestSupport {

  def verifyAudit(
                   auditService: AuditService,
                   journeyStatus: JourneyStatus,
                   affinityGroup: AffinityGroup,
                   subscriptionId:SubscriptionId,
                   credentialId: CredentialId,
                   submissionId: SubmissionId,
                   auditType:AuditType,
                 ): Unit = {

    val auditCaptor = ArgumentCaptor.forClass(classOf[AuditModel])

    verify(auditService, times(1)).audit(auditCaptor.capture())(any())

    val event = auditCaptor.getValue

    event.auditType mustBe auditType.value

    auditCaptor.getValue.detail mustBe Json.obj(
      "journeyStatus" -> journeyStatus.toString,
      "subscriptionId" -> subscriptionId,
      "affinityGroup" -> affinityGroup.toString,
      "credentialId" -> credentialId,
      "submissionId" -> submissionId,
    )
  }

  def verifyUpscanAudit(
                         auditService: AuditService,
                         expectedStatus: String,
                         expectedReference: String,
                         expectedFileName: Option[String] = None,
                         expectedFailureReason: Option[String] = None,
                         expectedFailureMessage: Option[String] = None
                       ): Unit = {
    val auditCaptor = ArgumentCaptor.forClass(classOf[UpscanValidationAuditModel])
    verify(auditService, times(1)).audit(auditCaptor.capture())(any())

    val event = auditCaptor.getValue
    event.upscanStatus mustBe expectedStatus
    event.fileReference mustBe expectedReference
    event.fileName mustBe expectedFileName
    event.stcAuditType mustBe AuditType.UpscanValidation
    event.failureReason mustBe expectedFailureReason
    event.failureMessage mustBe expectedFailureMessage
  }

  def verifyBulkUploadAudit(
                         auditService: AuditService,
                         uploadJourney: String,
                         affinityGroup: AffinityGroup,
                         subscriptionId: SubscriptionId,
                         credentialId: CredentialId,
                         fileType : String,
                         fileUploadStatus : String,
                         fileSize : String,
                         fileValidationTime: Long,
                         fileName : String,
                         fileReference : String,
                         numberOfEntries : Option[Int] = None,
                         errorType : Option[String] = None,
                         volume : Option[String] = None,
                         stcAuditType: AuditType
                       ): Unit = {
    val auditCaptor = ArgumentCaptor.forClass(classOf[BulkUploadProcessedAuditModel])
    verify(auditService, times(1)).audit(auditCaptor.capture())(any())

    val event = auditCaptor.getValue

    event.auditType mustBe stcAuditType.value

    val baseDetail = Json.obj(
      "uploadJourney"      -> uploadJourney,
      "affinityGroup"      -> affinityGroup.toString,
      "subscriptionId"     -> subscriptionId,
      "credentialId"       -> credentialId,
      "fileType"           -> fileType,
      "fileUploadStatus"   -> fileUploadStatus,
      "fileSize"           -> fileSize,
      "fileValidationTime" -> fileValidationTime,
      "fileName"           -> fileName,
      "fileReference"      -> fileReference
    )

    val withNumberOfEntries = numberOfEntries.fold(baseDetail)(n => baseDetail ++ Json.obj("numberOfEntries" -> n))
    val withErrorType = errorType.fold(withNumberOfEntries)(e => withNumberOfEntries ++ Json.obj("errorType" -> e))
    val expectedDetail = volume.fold(withErrorType)(v => withErrorType ++ Json.obj("volume" -> v))

    event.detail mustBe expectedDetail
  }
}