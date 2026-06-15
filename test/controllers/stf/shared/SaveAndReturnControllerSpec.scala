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

package controllers.stf.shared

import base.Fixtures.{testCredentialId, testInternalId, testSubmissionId}
import base.stubs.{AgentStubStcAuthEnrolledAction, OrganisationStubStcAuthEnrolledAction, StubStcAuthEnrolledAction}
import base.{AuditTestSupport, SpecBase}
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{doReturn, reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, Call, MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.SaveAndReturnController
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.audit.JourneyStatus.ContinueSubmission
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.PersistentNavigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.shared.SaveAndReturnPage
import uk.gov.hmrc.securitiestransferchargefrontend.services.AuditService

import scala.concurrent.Future

class SaveAndReturnControllerSpec extends SpecBase with MockitoSugar with ScalaFutures with AuditTestSupport with BeforeAndAfterEach {

  implicit val testRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest

  private val testUserAnswers = UserAnswers(testInternalId, testGroupIdentifier, testSubmissionId)
  private val testCall = Call("GET", "/test-page")
  private val mockMessagesApi = mock[MessagesApi]
  private val mockControllerComponents = mock[MessagesControllerComponents]
  private val mockBodyParsers = mock[PlayBodyParsers]
  private val mockNavigator = mock[PersistentNavigator]
  private val mockAuditService = mock[AuditService]

  override def beforeEach(): Unit = {
    reset(mockNavigator, mockAuditService)
    super.beforeEach()
  }

  private def testSetup(affinityGroup: AffinityGroup): SaveAndReturnController = {

    val authAction = affinityGroup match {
      case AffinityGroup.Organisation => OrganisationStubStcAuthEnrolledAction(mockBodyParsers)
      case AffinityGroup.Agent => AgentStubStcAuthEnrolledAction(mockBodyParsers)
      case _ => StubStcAuthEnrolledAction(mockBodyParsers)
    }

    new SaveAndReturnController(mockMessagesApi, mockControllerComponents, authAction, mockNavigator, mockAuditService)(ec)
  }

  "SaveAndReturnController" - {

    "restore" - {

      "must fail when restore fails" in {

        doReturn(Future.failed(new RuntimeException("No user answers"))).when(mockNavigator).restore(any[SubmissionId], any[UserId])(any())

        val result = testSetup(AffinityGroup.Individual).restore(testSubmissionId.value).apply(testRequest)

        whenReady(result.failed) {
          ex => ex.getMessage mustBe "No user answers"
        }
      }

      "must redirect to error page when next page is unavailable" in {

        doReturn(Future.successful(testUserAnswers)).when(mockNavigator).restore(any[SubmissionId], any[UserId])(any())

        when(mockNavigator.errorPage(SaveAndReturnPage)).thenReturn(testCall)

        val result = testSetup(AffinityGroup.Individual).restore(testSubmissionId.value).apply(testRequest)

        whenReady(result) { response =>
          response.header.status mustBe SEE_OTHER
          response.header.headers("Location") mustBe testCall.url
        }
      }

      Seq(
        AffinityGroup.Individual,
        AffinityGroup.Organisation,
        AffinityGroup.Agent
      ).foreach { affinityGroup =>

        s"must audit ContinueSubmission and redirect for $affinityGroup" in {

          val userAnswers = testUserAnswers.setNextPage(testCall)

          doReturn(Future.successful(userAnswers)).when(mockNavigator).restore(eqTo(testSubmissionId), eqTo(testInternalId))(any())

          val result = testSetup(affinityGroup).restore(testSubmissionId.value).apply(testRequest)

          whenReady(result) { response =>

            response.header.status mustBe SEE_OTHER
            response.header.headers("Location") mustBe testCall.url

            verifyAudit(
              mockAuditService,
              ContinueSubmission,
              affinityGroup,
              testCredentialId,
              testSubmissionId
            )
          }
        }
      }
    }
  }
}
