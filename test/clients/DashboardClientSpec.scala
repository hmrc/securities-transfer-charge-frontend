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

package clients

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.must.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.DashboardClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class DashboardClientSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()

  val subscriptionId: SubscriptionId = SubscriptionId("Sub-123456")
  val client: DashboardClient = new DashboardClient {
    override implicit val ec: ExecutionContext = global

    override def getReadyToPayTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
      Future.successful(1)

    override def getOverdueTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
      Future.successful(5)

    override def getRecentTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
      Future.successful(mock[EtmpTransactionSummaryResponse])

    override def getReadyToPayTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
      Future.successful(mock[EtmpTransactionSummaryResponse])


    override def getOverdueTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
      Future.successful(mock[EtmpTransactionSummaryResponse])

  }

  "DashboardClientClientImpl" should {

    "return the number of ready to pay transactions" in {

      val result = client.getReadyToPayTransactionsCount(subscriptionId).futureValue

      result mustBe 1
    }

    "return the number of overdue transactions" in {

      val result = client.getOverdueTransactionsCount(subscriptionId).futureValue

      result mustBe 5
    }

    "propagate failure when service fails" in {

      val client: DashboardClient = new DashboardClient {
        override implicit val ec: ExecutionContext = global

        override def getReadyToPayTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
          Future.failed(RuntimeException("Exception"))

        override def getOverdueTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
          Future.failed(RuntimeException("Exception"))

        override def getRecentTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
          Future.failed(RuntimeException("Exception"))

        override def getReadyToPayTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
          Future.failed(RuntimeException("Exception"))

        override def getOverdueTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
          Future.failed(RuntimeException("Exception"))
      }

      assertThrows[RuntimeException] {
        client.getReadyToPayTransactionsCount(subscriptionId).futureValue
        client.getOverdueTransactionsCount(subscriptionId).futureValue
      }
    }
  }
}
