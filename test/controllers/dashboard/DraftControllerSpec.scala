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

package controllers.dashboard

import base.SpecBase
import base.Fixtures.*
import org.mockito.ArgumentMatchers.{any, eq as eqTo}
import org.mockito.Mockito.{times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.mvc.Call
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes as appRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.services.AnswerPersistenceService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DraftView

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class DraftControllerSpec extends SpecBase {

  private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC)
  private val draftAnswers = testUserAnswers.copy(journeyType = SH03, createdAt = Instant.parse("2026-11-01T09:00:00Z"))

  "Draft Controller" - {

    "onPageLoad" - {

      Seq(
        "an organisation" -> (AffinityGroup.Organisation, affinityGroupKeyOrg),
        "an agent" -> (AffinityGroup.Agent, affinityGroupKeyAgent)
      ).foreach { case (userType, (affinityGroup, affinityKey)) =>

        s"must return OK and the correct view for a GET for $userType" in {

          val mockSaveAndReturnClient = mock[SaveAndReturnClient]

          when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
            .thenReturn(Future.successful(draftAnswers))

          val application = applicationBuilder(
            saveAndReturnClient = mockSaveAndReturnClient,
            affinityGroup = affinityGroup
          )
            .overrides(bind[Clock].toInstance(fixedClock))
            .build()

          running(application) {
            val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

            val view = application.injector.instanceOf[DraftView]
            val viewModel = DraftViewModel.build(draftAnswers, affinityKey, fixedClock, SH03)

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          }
        }
      }

      "must fail when the draft cannot be retrieved" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(any())(any()))
          .thenReturn(Future.failed(new RuntimeException("draft not found")))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          result.failed.futureValue mustBe a[RuntimeException]
        }
      }
    }

    "onContinueSubmission" - {

      "must redirect to the next page saved on the draft" in {

        val mockAnswerPersistenceService = mock[AnswerPersistenceService]
        val savedNextPage = Call("GET", "/saved-next-page")

        when(mockAnswerPersistenceService.load(eqTo(testSubmissionId), eqTo(testInternalId))(any()))
          .thenReturn(Future.successful(testUserAnswers.setNextPage(savedNextPage)))

        val application = applicationBuilder()
          .overrides(bind[AnswerPersistenceService].toInstance(mockAnswerPersistenceService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onContinueSubmission(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual savedNextPage.url
        }
      }

      "must redirect to Journey Recovery when the draft has no next page" in {

        val mockAnswerPersistenceService = mock[AnswerPersistenceService]

        when(mockAnswerPersistenceService.load(any(), any())(any()))
          .thenReturn(Future.successful(testUserAnswers))

        val application = applicationBuilder()
          .overrides(bind[AnswerPersistenceService].toInstance(mockAnswerPersistenceService))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onContinueSubmission(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual appRoutes.JourneyRecoveryController.onPageLoad().url
        }
      }
    }

    "onDeleteDraft" - {

      "must delete the draft and redirect to the dashboard" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.deleteDraft(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(()))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onDeleteDraft(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual SEE_OTHER
          redirectLocation(result).value mustEqual routes.DashboardController.onPageLoad().url
          verify(mockSaveAndReturnClient, times(1)).deleteDraft(eqTo(testSubmissionId))(any())
        }
      }
    }
  }
}
