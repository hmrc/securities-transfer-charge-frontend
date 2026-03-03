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

package controllers.stf.individuals

import base.Fixtures.confirmableAddress
import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SubscriptionData, SubscriptionDataRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AddressService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {
  
  val subscriptionData: SubscriptionData = SubscriptionData(subscriptionId = SubscriptionId("Sub-01"), subscriptionDetails = subscription)



  "ConfirmAddress Controller" - {

    "must return OK and the correct view for a GET" in {

      val mockSubscriptionConnector = mock[SubscriptionConnector]
      val mockAddressService = mock[AddressService]

      when(mockSubscriptionConnector.getValidSubscription(any())(any()))
        .thenReturn(Future.successful(subscription))
      when(mockAddressService.extractConfirmableAddress(subscription)).thenReturn(confirmableAddress)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionConnector].toInstance(mockSubscriptionConnector),
            inject.bind[AddressService].toInstance(mockAddressService),
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, individualRoutes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(confirmableAddress)(request, messages(application)).toString
      }
    }

    "must store address and redirect on successful POST" in {

      val mockSubscriptionDataRepository = mock[SubscriptionDataRepository]
      val mockAddressService = mock[AddressService]
      val saveAndReturnClient = mock[SaveAndReturnClient]

      when(mockSubscriptionDataRepository.getSubscriptionData(any()))
        .thenReturn(Future.successful(Some(subscriptionData)))

      when(
        mockAddressService.extractConfirmableAddress(
          subscriptionData.subscriptionDetails
        )
      ).thenReturn(confirmableAddress)

      when(saveAndReturnClient.save(any[UserAnswers]())(any[HeaderCarrier]()))
        .thenReturn(Future.successful(()))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionDataRepository].toInstance(mockSubscriptionDataRepository),
            inject.bind[AddressService].toInstance(mockAddressService),
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, individualRoutes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER

      }
    }

    "must redirect to journey recovery when no subscription is found" in {

      val mockSubscriptionDataRepository = mock[SubscriptionDataRepository]

      when(mockSubscriptionDataRepository.getSubscriptionData(any()))
        .thenReturn(Future.successful(None))

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionDataRepository].toInstance(mockSubscriptionDataRepository)
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, individualRoutes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value.contains(routes.JourneyRecoveryController.onPageLoad().url) mustEqual true
      }
    }
  }
}
