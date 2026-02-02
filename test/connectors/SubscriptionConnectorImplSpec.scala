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

package connectors

import base.SpecBase
import org.mockito.ArgumentMatchers.*
import org.mockito.Mockito.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.*
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnectorImpl, SubscriptionDataNotFoundException, SubscriptionStatusErrorException}
import uk.gov.hmrc.securitiestransferchargefrontend.models.Address
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{SubscriptionData, SubscriptionDataRepository}

import scala.concurrent.Future

class SubscriptionConnectorImplSpec extends SpecBase with ScalaFutures with MockitoSugar {

  private val mockRegistrationClient = mock[RegistrationClient]
  private val mockSubscriptionRepo = mock[SubscriptionDataRepository]

  private val connector = new SubscriptionConnectorImpl(
    mockRegistrationClient,
    mockSubscriptionRepo
  )

  private val subscription = Subscription(
    subsValidTo = java.time.LocalDateTime.now(),
    contactName = "John Doe",
    addressLine1 = "1 high street",
    addressLine2 = Some("Bobbins on Sea"),
    addressLine3 = Some("Town"),
    postcode = "ZZ1 1ZZ",
    countryCode = "GB",
    telephoneNumber = "07777777777",
    emailAddress = "some@email.com"
  )

  private val subscriptionData = SubscriptionData(
    stcId = "STC123",
    subscriptionDetails = subscription
  )

  "getSubscriptionStatus" - {

    "return SubscriptionActive when registration client returns Right(SubscriptionActive)" in {
      when(mockRegistrationClient.getSubscriptionStatus(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Right(SubscriptionStatus.SubscriptionActive)))

      val result = connector.getSubscriptionStatus("STC123")

      whenReady(result) { status =>
        status mustBe SubscriptionStatus.SubscriptionActive
      }
    }

    "fail with SubscriptionStatusErrorException when registration client returns Left" in {

      when(mockRegistrationClient.getSubscriptionStatus(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(Left(SubscriptionClientError("Some client error"))))

      val result = connector.getSubscriptionStatus("STC123")

      whenReady(result.failed) { ex =>
        ex mustBe a[SubscriptionStatusErrorException]
      }
    }

    "getSubscriptionDetails" - {

      "return the subscription and save it to the repository" in {
        when(mockRegistrationClient.getSubscriptionDetails(any[String])(any[HeaderCarrier]))
          .thenReturn(Future.successful(subscription))
        when(mockSubscriptionRepo.saveSubscriptionData(any[String], any[Subscription]))
          .thenReturn(Future.successful(()))

        val result = connector.getSubscriptionDetails("STC123")

        whenReady(result) { sub =>
          sub mustBe subscription
          verify(mockSubscriptionRepo).saveSubscriptionData("STC123", subscription)
        }
      }
    }
    
  }
}