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

package uk.gov.hmrc.securitiestransferchargefrontend.clients

import base.{Fixtures, SpecBase}
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import java.io.FileNotFoundException
import scala.concurrent.Future
import org.mockito.Mockito.{times, verify, when, never}

class DashboardClientCacheSpec extends SpecBase with MockitoSugar with ScalaFutures {

  trait TestSetup {
    val dashboardClient: DashboardClient = mock[DashboardClient]
    val repo: DashboardClientDataCache = mock[DashboardClientDataCache]
    val cache: DashboardClientCache = new DashboardClientCache(dashboardClient, repo)

    val testKey = ReadyToPay(Fixtures.testSubscriptionId)

    val testSummaryResponse: EtmpTransactionSummaryResponse =
      EtmpTransactionSummaryResponse(
        success = Fixtures.buildEtmpSuccessResponse(
          transactionDetails = List(
            Fixtures.buildSubmissionDetails(
              submissionId = Fixtures.testSubmissionId,
              isPast = false,
              utrn = Fixtures.testUtrn
            )
          ),
          chargeDetails = List(
            Fixtures.buildTaxCharge(Fixtures.testUtrn, false, false)
          )
        )
      )
  }

  "The cache should" - {
    "return an item found in the cache" in new TestSetup {
      when(repo.retrieve(testKey))
        .thenReturn(Future.successful(Some(testSummaryResponse)))

      val outcome: Future[EtmpTransactionSummaryResponse] =
        cache.getReadyToPayTransactions(Fixtures.testSubscriptionId)(Fixtures.hc)

      whenReady(outcome) { resp =>
        verify(repo, never).store(any[String], any[EtmpTransactionSummaryResponse])
        resp mustBe testSummaryResponse
      }
    }
    "call the underlying service if an item is not found in the cache" in new TestSetup {
      when(repo.retrieve(testKey))
        .thenReturn(Future.successful(None))

      when(dashboardClient.getReadyToPayTransactions(any[SubscriptionId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testSummaryResponse))

      val outcome: Future[EtmpTransactionSummaryResponse] =
        cache.getReadyToPayTransactions(Fixtures.testSubscriptionId)(Fixtures.hc)

      whenReady(outcome) { resp =>
        verify(repo, times(1)).store(any[String], any[EtmpTransactionSummaryResponse])
        resp mustBe testSummaryResponse
      }
    }
    "call the underlying service if the call to the cache fails" in new TestSetup {
      when(repo.retrieve(testKey))
        .thenReturn(Future.failed(new FileNotFoundException("oops")))

      when(dashboardClient.getReadyToPayTransactions(any[SubscriptionId])(any[HeaderCarrier]))
        .thenReturn(Future.successful(testSummaryResponse))

      val outcome: Future[EtmpTransactionSummaryResponse] =
        cache.getReadyToPayTransactions(Fixtures.testSubscriptionId)(Fixtures.hc)

      whenReady(outcome) { resp =>
        verify(repo, times(1)).store(any[String], any[EtmpTransactionSummaryResponse])
        resp mustBe testSummaryResponse
      }
    }
  }
}
