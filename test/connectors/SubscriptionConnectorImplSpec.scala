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
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.*
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.{SubscriptionConnectorImpl, SubscriptionStatusErrorException}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SubscriptionDataRepository

import java.time.LocalDate
import scala.concurrent.Future

class SubscriptionConnectorImplSpec
  extends SpecBase
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach {

  override def beforeEach(): Unit = {
    reset(mockRegistrationClient, mockSubscriptionRepo)
    super.beforeEach()
  }

  private val mockRegistrationClient = mock[RegistrationClient]
  private val mockSubscriptionRepo = mock[SubscriptionDataRepository]

  private val connector = new SubscriptionConnectorImpl(
    mockRegistrationClient,
    mockSubscriptionRepo
  )
  

  private val expiredSubscription =
    subscription.copy(subsValidTo = LocalDate.now().minusDays(1))

  "getValidSubscription" - {

    "return the subscription and save it when the subscription is valid" in {
      when(mockRegistrationClient.getSubscriptionDetails(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(subscription))

      when(mockSubscriptionRepo.saveSubscriptionData(any[String], any[Subscription]))
        .thenReturn(Future.successful(()))

      val result = connector.getValidSubscription("STC123")

      whenReady(result) { sub =>
        sub mustBe subscription
        verify(mockSubscriptionRepo).saveSubscriptionData("STC123", subscription)
      }
    }

    "fail with SubscriptionStatusErrorException when the subscription has expired" in {
      when(mockRegistrationClient.getSubscriptionDetails(any[String])(any[HeaderCarrier]))
        .thenReturn(Future.successful(expiredSubscription))

      val result = connector.getValidSubscription("STC123")

      whenReady(result.failed) { ex =>
        ex mustBe a[SubscriptionStatusErrorException]
        verify(mockSubscriptionRepo, never())
          .saveSubscriptionData(any[String], any[Subscription])
      }
    }
  }
}
