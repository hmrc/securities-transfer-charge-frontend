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

package models.audit

import base.Fixtures.{testAuditType, testCredentialId, testInternalId, testSubmissionId}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, JourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.CredentialId

class AuditModelSpec extends AnyFreeSpec with Matchers {

  private val affinityGroups = Seq(
    Individual,
    Organisation,
    Agent
  )

  "AuditModel.build" - {

    affinityGroups.foreach { affinityGroup =>

      JourneyStatus.values.foreach { journeyStatus =>

        s"must create correct audit model for affinity group [$affinityGroup] and journey type [$journeyStatus]" in {

          val expectedJson = Json.obj(
            "journeyStatus"   -> journeyStatus.toString,
            "internalId"    -> testInternalId,
            "affinityGroup" -> affinityGroup.toString,
            "credentialId"  -> testCredentialId,
            "submissionId"  -> testSubmissionId
          )

          val result = AuditModel.build(
            journeyStatus = journeyStatus,
            internalId = testInternalId,
            affinityGroup = affinityGroup,
            credentialId = testCredentialId,
            submissionId = testSubmissionId
          )

          result.auditType mustBe testAuditType
          result.detail mustBe expectedJson
        }
      }
    }

    affinityGroups.foreach { affinityGroup =>

      JourneyStatus.values.foreach { journeyStatus =>

        s"must populate all fields correctly for affinity group [$affinityGroup] and journey type [$journeyStatus]" in {

          val result = AuditModel(
            journeyStatus = journeyStatus,
            internalId = testInternalId,
            affinityGroup = affinityGroup,
            credentialId = testCredentialId,
            submissionId = testSubmissionId
          )

          result.detail mustBe Json.obj(
            "journeyStatus"   -> journeyStatus.toString,
            "internalId"    -> testInternalId,
            "affinityGroup" -> affinityGroup.toString,
            "credentialId"  -> testCredentialId,
            "submissionId"  -> testSubmissionId
          )
        }
      }
    }
  }
}