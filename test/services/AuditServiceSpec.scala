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

package services

import base.Fixtures.{testAuditType, testCredentialId, testInternalId, testSubmissionId}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito
import org.mockito.Mockito.{times, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup.{Agent, Individual, Organisation}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.{AuditModel, JourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService

import scala.concurrent.ExecutionContext
import scala.concurrent.ExecutionContext.Implicits.global

class AuditServiceSpec extends AnyFreeSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]

  implicit private val headerCarrier: HeaderCarrier = HeaderCarrier()

  private val affinityGroups = Seq(
    Individual,
    Organisation,
    Agent
  )

  private val service = new AuditService(mockAuditConnector)

  override def beforeEach(): Unit = {
    Mockito.reset(mockAuditConnector)
    super.beforeEach()
  }

  ".audit" - {

    affinityGroups.foreach { affinityGroup =>

      JourneyStatus.values.foreach { journeyStatus =>

        s"must send an audit event for affinity group [$affinityGroup] and journey type [$journeyStatus]" in {

          val auditModel = AuditModel.build(
            journeyStatus = journeyStatus,
            internalId = testInternalId,
            affinityGroup = affinityGroup,
            credentialId = testCredentialId,
            submissionId = testSubmissionId
          )

          service.audit(auditModel)

          val expectedJson = Json.obj(
            "journeyStatus" -> journeyStatus.toString,
            "internalId" -> testInternalId,
            "affinityGroup" -> affinityGroup.toString,
            "credentialId" -> testCredentialId,
            "submissionId" -> testSubmissionId
          )


          verify(mockAuditConnector, times(1)).sendExplicitAudit(
            eqTo(testAuditType),
            eqTo(expectedJson)
          )(any(), any())
        }
      }
    }

    "must not throw exception when audit connector fails" in {

      val auditModel = AuditModel.build(
        journeyStatus = JourneyStatus.ContinueSubmission,
        internalId = testInternalId,
        affinityGroup = uk.gov.hmrc.auth.core.AffinityGroup.Individual,
        credentialId = testCredentialId,
        submissionId = testSubmissionId
      )

      when(mockAuditConnector
        .sendExplicitAudit(any[String], any[JsObject])(any[HeaderCarrier], any[ExecutionContext]))
        .thenThrow(new RuntimeException("Failed"))

      noException shouldBe thrownBy {
        service.audit(auditModel)
      }

      verify(mockAuditConnector, times(1)).sendExplicitAudit(any[String], any[JsObject])(any[HeaderCarrier], any[ExecutionContext])
    }
  }
}