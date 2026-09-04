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

import base.Fixtures.{testAuditType, testSubscriptionId}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{times, verify}
import org.scalatest.matchers.must.Matchers.mustBe
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{CredentialId, SubmissionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, JourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService

trait AuditTestSupport {

  def verifyAudit(
                   auditService: AuditService,
                   journeyStatus: JourneyStatus,
                   affinityGroup: AffinityGroup,
                   credentialId: CredentialId,
                   submissionId: SubmissionId,
                 ): Unit = {

    val auditCaptor = ArgumentCaptor.forClass(classOf[AuditModel])

    verify(auditService, times(1)).audit(auditCaptor.capture())(any())

    val event = auditCaptor.getValue

    event.auditType mustBe testAuditType

    auditCaptor.getValue.detail mustBe Json.obj(
      "journeyStatus" -> journeyStatus.toString,
      "subscriptionId" -> testSubscriptionId,
      "affinityGroup" -> affinityGroup.toString,
      "credentialId" -> credentialId,
      "submissionId" -> submissionId,
    )
  }
}