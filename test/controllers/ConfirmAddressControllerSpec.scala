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

package controllers

import base.SpecBase
import controllers.actions.FakeAuthAction
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject
import play.api.mvc.PlayBodyParsers
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.SubscriptionConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.{ConfirmableAddress, Country}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SessionRepository, SubscriptionData, SubscriptionDataRepository}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AddressService
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.ConfirmAddressView

import scala.concurrent.Future

class ConfirmAddressControllerSpec extends SpecBase {
  
  val subscriptionData: SubscriptionData = SubscriptionData(stcId = "STC1234", subscriptionDetails = subscription)
  val confirmableAddress: ConfirmableAddress = ConfirmableAddress(
    lines = List(
      "1 High Street",
      "Town"
    ),
    postcode = "ZZ1 1ZZ",
    country = Some(Country("United Kingdom", "GB"))
  )


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
            inject.bind[StcAuthEnrolledAction].toInstance(
              new FakeAuthAction(
                applicationBuilder().build().injector.instanceOf[PlayBodyParsers]
              )
            )
          )
          .build()

      running(application) {
        val request = FakeRequest(GET, routes.ConfirmAddressController.onPageLoad().url)

        val result = route(application, request).value

        val view = application.injector.instanceOf[ConfirmAddressView]


        status(result) mustEqual OK
        contentAsString(result) mustEqual view(confirmableAddress)(request, messages(application)).toString
      }
    }

    "must store address and redirect on successful POST" in {

      val mockSubscriptionDataRepository = mock[SubscriptionDataRepository]
      val mockAddressService = mock[AddressService]
      val mockSessionRepository = mock[SessionRepository]

      when(mockSessionRepository.set(any())) thenReturn Future.successful(true)

      when(mockSubscriptionDataRepository.getSubscriptionData(any()))
        .thenReturn(Future.successful(Some(subscriptionData)))

      when(
        mockAddressService.extractConfirmableAddress(
          subscriptionData.subscriptionDetails
        )
      ).thenReturn(confirmableAddress)

      val application =
        applicationBuilder(userAnswers = Some(emptyUserAnswers))
          .overrides(
            inject.bind[SubscriptionDataRepository].toInstance(mockSubscriptionDataRepository),
            inject.bind[AddressService].toInstance(mockAddressService),
            inject.bind[SessionRepository].toInstance(mockSessionRepository),
            inject.bind[StcAuthEnrolledAction].toInstance(
              new FakeAuthAction(
                applicationBuilder().build().injector.instanceOf[PlayBodyParsers]
              )
            )

          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, routes.ConfirmAddressController.onSubmit().url)

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
            inject.bind[SubscriptionDataRepository].toInstance(mockSubscriptionDataRepository),
            inject.bind[StcAuthEnrolledAction].toInstance(
              new FakeAuthAction(
                applicationBuilder().build().injector.instanceOf[PlayBodyParsers]
              )
            )
          )
          .build()

      running(application) {

        val request =
          FakeRequest(POST, routes.ConfirmAddressController.onSubmit().url)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual
          routes.JourneyRecoveryController.onPageLoad().url
      }
    }
  }
}
