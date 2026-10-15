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

package controllers.stf.organisations.single

import base.Fixtures.confirmableAddress
import base.SpecBase
import base.stubs.StubSubscriptionService
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, times, verify, when}
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.organisations.single.routes as orgSingleRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.NormalMode
import uk.gov.hmrc.securitiestransferchargefrontend.services.{AddressService, SubscriptionService}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.organisations.single.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {

  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSubscriptionService = mock[SubscriptionService]
      val mockAddressService = mock[AddressService]

      when(mockSubscriptionService.getSubscription(any())(any()))
        .thenReturn(Future.successful(subscription))

      when(mockAddressService.extractConfirmableAddress(subscription))
        .thenReturn(confirmableAddress)

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(
          inject.bind[SubscriptionService].toInstance(mockSubscriptionService),
          inject.bind[AddressService].toInstance(mockAddressService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(GET, orgSingleRoutes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value
        val view = application.injector.instanceOf[ConfirmAddressView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(confirmableAddress)(request, messages(application)).toString
      }
    }

    "must redirect to the error page when GET subscription retrieval fails" in {

      val mockSubscriptionService = mock[SubscriptionService]

      when(mockSubscriptionService.getSubscription(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Subscription error")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(
          inject.bind[SubscriptionService].toInstance(mockSubscriptionService)
        )
        .build()

      running(application) {
        val request =
          FakeRequest(GET, orgSingleRoutes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must reuse existing user answers and redirect on successful POST" in {

      val mockSubscriptionService = mock[SubscriptionService]
      val mockAddressService = mock[AddressService]
      val mockIdClient = mock[SubmissionIdClient]

      when(mockSubscriptionService.getSubscription(any())(any()))
        .thenReturn(Future.successful(subscription))

      when(mockAddressService.extractConfirmableAddress(subscription))
        .thenReturn(confirmableAddress)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
          .overrides(
            inject.bind[SubscriptionService].toInstance(mockSubscriptionService),
            inject.bind[AddressService].toInstance(mockAddressService),
            inject.bind[SubmissionIdClient].toInstance(mockIdClient)
          )
          .build()

      running(application) {
        val request =
          FakeRequest(POST, orgSingleRoutes.ConfirmAddressController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockIdClient, never()).nextSubmissionId()
      }
    }

    "must generate a submission ID when no user answers exist" in {

      val mockAddressService = mock[AddressService]
      val mockIdClient = mock[SubmissionIdClient]

      when(mockAddressService.extractConfirmableAddress(subscription))
        .thenReturn(confirmableAddress)

      when(mockIdClient.nextSubmissionId()(any[HeaderCarrier]))
        .thenReturn(Future.successful(SubmissionId("STC-000000001")))

      val application = applicationBuilder(userAnswers = None, affinityGroup = orgAffinity)
        .overrides(
          inject.bind[SubscriptionService].toInstance(StubSubscriptionService()),
          inject.bind[AddressService].toInstance(mockAddressService),
          inject.bind[SubmissionIdClient].toInstance(mockIdClient)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, orgSingleRoutes.ConfirmAddressController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

        verify(mockIdClient, times(1)).nextSubmissionId()(any[HeaderCarrier])
      }
    }

    "must fail when subscription retrieval fails on POST" in {

      val mockSubscriptionService = mock[SubscriptionService]

      when(mockSubscriptionService.getSubscription(any())(any()))
        .thenReturn(Future.failed(new RuntimeException("Subscription error")))

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers), affinityGroup = orgAffinity)
        .overrides(
          inject.bind[SubscriptionService].toInstance(mockSubscriptionService)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, orgSingleRoutes.ConfirmAddressController.onSubmit(NormalMode).url)

        val result = route(application, request).value

        intercept[RuntimeException] {
          status(result)
        }
      }
    }
  }
}