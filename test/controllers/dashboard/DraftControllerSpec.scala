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
import org.mockito.Mockito.{never, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.dashboard.routes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.SH03
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftViewModel
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.dashboard.DraftView

import java.time.{Clock, Instant, ZoneOffset}
import scala.concurrent.Future

class DraftControllerSpec extends SpecBase {

  private val fixedClock: Clock = Clock.fixed(Instant.parse("2026-11-19T10:00:00Z"), ZoneOffset.UTC)
  private val draftAnswers = testUserAnswers.copy(journeyType = SH03, createdAt = Instant.parse("2026-11-01T09:00:00Z"))

  private val otherGroupsDraft = draftAnswers.copy(userId = UserId("another-user"), groupIdentifier = GroupIdentifier("another-group"))
  private val sameGroupOtherUsersDraft = draftAnswers.copy(userId = UserId("another-user"))

  private val userIdOnly = "microservice.save-and-return-uses-user-id-only"

  private val draftNotFound = UpstreamErrorResponse("draft not found", NOT_FOUND)

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
            val viewModel = DraftViewModel.build(draftAnswers, affinityKey, fixedClock, draftTtlDays = 30)(messages(application))

            val result = route(application, request).value

            status(result) mustEqual OK
            contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
          }
        }
      }

      "must use the configured draft time to live" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(draftAnswers))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient)
          .configure("microservice.services.securities-transfer-charge-save-and-return.draftTimeToLiveInDays" -> 60)
          .overrides(bind[Clock].toInstance(fixedClock))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val view = application.injector.instanceOf[DraftView]
          val viewModel = DraftViewModel.build(draftAnswers, affinityGroupKeyInd, fixedClock, draftTtlDays = 60)(messages(application))

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual view(viewModel)(request, messages(application)).toString
        }
      }

      "must return NOT_FOUND when the draft belongs to another group" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(otherGroupsDraft))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }

      "must return OK for another user's draft in the same group" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(sameGroupOtherUsersDraft))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient)
          .overrides(bind[Clock].toInstance(fixedClock))
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual OK
        }
      }

      "must return NOT_FOUND for another user's draft in the same group when drafts are per user" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(sameGroupOtherUsersDraft))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient)
          .configure(userIdOnly -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }

      "must return NOT_FOUND when the draft has been deleted or has expired" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(any())(any()))
          .thenReturn(Future.failed(draftNotFound))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
        }
      }

      "must fail when the draft service returns another error" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(any())(any()))
          .thenReturn(Future.failed(UpstreamErrorResponse("service unavailable", SERVICE_UNAVAILABLE)))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onPageLoad(testSubmissionId.value).url)

          val result = route(application, request).value

          result.failed.futureValue mustBe an[UpstreamErrorResponse]
        }
      }
    }

    "onDeleteDraft" - {

      "must return OK and not delete the draft" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(draftAnswers))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onDeleteDraft(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual OK
          contentAsString(result) mustEqual "success"
          verify(mockSaveAndReturnClient, never()).deleteDraft(any())(any())
        }
      }

      "must return NOT_FOUND and not delete when the draft has been deleted or has expired" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(any())(any()))
          .thenReturn(Future.failed(draftNotFound))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onDeleteDraft(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
          verify(mockSaveAndReturnClient, never()).deleteDraft(any())(any())
        }
      }

      "must return NOT_FOUND and not delete a draft that belongs to another group" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(otherGroupsDraft))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient).build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onDeleteDraft(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
          verify(mockSaveAndReturnClient, never()).deleteDraft(any())(any())
        }
      }

      "must return NOT_FOUND and not delete another user's draft when drafts are per user" in {

        val mockSaveAndReturnClient = mock[SaveAndReturnClient]

        when(mockSaveAndReturnClient.retrieve(eqTo(testSubmissionId))(any()))
          .thenReturn(Future.successful(sameGroupOtherUsersDraft))

        val application = applicationBuilder(saveAndReturnClient = mockSaveAndReturnClient)
          .configure(userIdOnly -> true)
          .build()

        running(application) {
          val request = FakeRequest(GET, routes.DraftController.onDeleteDraft(testSubmissionId.value).url)

          val result = route(application, request).value

          status(result) mustEqual NOT_FOUND
          verify(mockSaveAndReturnClient, never()).deleteDraft(any())(any())
        }
      }
    }
  }
}
