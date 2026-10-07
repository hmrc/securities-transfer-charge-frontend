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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import base.Fixtures.{buildEtmpSuccessResponse, buildSubmissionDetails, buildTaxCharge}
import base.{Fixtures, SpecBase}
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{DashboardClient, SaveAndReturnClient}
import uk.gov.hmrc.securitiestransferchargefrontend.domain
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswersSummary
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Draft, Overdue, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{DashboardCounts, EtmpTransactionSummaryResponse}

import java.time.Instant
import scala.concurrent.Future

class DashboardServiceSpec extends SpecBase with ScalaFutures with MockitoSugar {

  trait TestSetup {
    val mockSaveAndReturnClient: SaveAndReturnClient = mock[SaveAndReturnClient]
    val mockDashboardClient: DashboardClient = mock[DashboardClient]
    val dashboardService: DashboardService = new DashboardServiceImpl(mockSaveAndReturnClient, mockDashboardClient)
    implicit val mockHc: HeaderCarrier = mock[HeaderCarrier]
  }

  "getCounts should" - {
    "return the correct numbers of items" in new TestSetup {
      when(mockSaveAndReturnClient.getDraftSummaries(Fixtures.testInternalId, Fixtures.testGroupIdentifier)(mockHc))
        .thenReturn(
          Future.successful(
            List(
              UserAnswersSummary(
                submissionId = Fixtures.testSubmissionId,
                journeyType = STF,
                createdAt = Instant.now(),
                lastUpdated = Instant.now()
              )
            )
          )
        )
      when(mockDashboardClient.getReadyToPayTransactionsCount(Fixtures.testSubscriptionId))
        .thenReturn(
          Future.successful(2)
        )
      when(mockDashboardClient.getOverdueTransactionsCount(Fixtures.testSubscriptionId))
        .thenReturn(
          Future.successful(3)
        )

      val counts: Future[DashboardCounts] = dashboardService
        .getCounts(Fixtures.testInternalId, Fixtures.testGroupIdentifier, Fixtures.testSubscriptionId)

      whenReady(counts) { cs =>
        cs.drafts mustBe 1
        cs.readyToPay mustBe 2
        cs.overdue mustBe 3
      }
    }
  }
  "getRecent should" - {
    "include drafts as well as overdue and unpaid items" in new TestSetup {
      val otherSubmissionId = domain.SubmissionId("STC-01020300")
      when(mockSaveAndReturnClient.getDraftSummaries(Fixtures.testInternalId, Fixtures.testGroupIdentifier)(mockHc))
        .thenReturn(
          Future.successful(
            List(
              UserAnswersSummary(
                submissionId = Fixtures.testSubmissionId,
                journeyType = STF,
                createdAt = Instant.now(),
                lastUpdated = Instant.now()
              )
            )
          )
        )
      when(mockDashboardClient.getRecentTransactions(Fixtures.testSubscriptionId))
        .thenReturn(
          Future.successful(
            EtmpTransactionSummaryResponse(
              success = buildEtmpSuccessResponse(
                transactionDetails = List(
                  buildSubmissionDetails(
                    submissionId = otherSubmissionId,
                    isPast = false,
                    utrn = Fixtures.testUtrn
                  )
                ),
                chargeDetails = List(
                  buildTaxCharge(Fixtures.testUtrn, false, false)
                )
              )
            )
          )
        )

      val outcome = dashboardService.getRecent(Fixtures.testInternalId, Fixtures.testGroupIdentifier, Fixtures.testSubscriptionId)
      whenReady(outcome) { summaries =>
        summaries.length mustEqual(2)
        summaries.count(_.status == Draft) mustEqual 1
        summaries.count(_.status == ReadyToPay) mustEqual 1
        summaries.count(_.status == Overdue) mustEqual 0
        summaries.count(_.submissionId == Fixtures.testSubmissionId.value) mustEqual 1
        summaries.count(_.submissionId == otherSubmissionId.value) mustEqual 1

      }
    }
  }
}
