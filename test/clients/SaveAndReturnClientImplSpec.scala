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


import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers.*
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClientImpl
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import java.net.URL
import scala.concurrent.Future


class SaveAndReturnClientImplSpec extends SpecBase {


  trait TestSetup {

    val mockHttp: HttpClientV2 = mock[HttpClientV2]
    val mockConfig: FrontendAppConfig = mock[FrontendAppConfig]
    val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

    val baseUrl = "http://localhost:1201/securities-transfer-charge-save-and-return"

    when(mockConfig.saveAndReturnUrl).thenReturn(baseUrl)

    val client = new SaveAndReturnClientImpl(mockHttp, mockConfig)

    val testUserId = "user1"
    val testSubmissionId: SubmissionId = SubmissionId("sub1")
    val testUserAnswers: UserAnswers = UserAnswers(testUserId, testSubmissionId)

    when(mockHttp.post(any[URL])(any[HeaderCarrier]))
      .thenReturn(mockRequestBuilder)

    when(mockHttp.get(any[URL])(any[HeaderCarrier]))
      .thenReturn(mockRequestBuilder)

    when(mockRequestBuilder.withBody(any())(any(), any(), any()))
      .thenReturn(mockRequestBuilder)
  }


  "SaveAndReturnClientImpl" - {

    "save" - {

      "returns Unit on successful call" in new TestSetup {
        when(mockRequestBuilder.execute[HttpResponse])
          .thenReturn(Future.successful(HttpResponse(204, "")))

        client.save(testUserAnswers).futureValue shouldBe()
      }


      "fail the future and log an error when the HTTP call fails" in new TestSetup {
        val exception = new RuntimeException("connection failed")

        when(mockRequestBuilder.execute[HttpResponse])
          .thenReturn(Future.failed(exception))

        val result: Future[Unit] = client.save(testUserAnswers)

        result.failed.futureValue mustBe exception
      }
    }

    "retrieve" - {
      "return UserAnswers on successful call" in new TestSetup {


        when(mockRequestBuilder.execute[UserAnswers](any(), any()))
          .thenReturn(Future.successful(testUserAnswers))


        val result: UserAnswers = client.retrieve(testUserId, testSubmissionId).futureValue

        result mustBe testUserAnswers
      }

      "fail the future and log an error when the HTTP call fails" in new TestSetup {
        val exception = new RuntimeException("connection failed")

        when(mockRequestBuilder.execute[UserAnswers](any(), any()))
          .thenReturn(Future.failed(exception))

        val result: Future[UserAnswers] = client.retrieve(testUserId, testSubmissionId)

        result.failed.futureValue mustBe exception
      }
    }

    "list" - {
      "return a list of submission on successful call" in new TestSetup {

        val submissionIds: List[SubmissionId] =
          List(SubmissionId("sub-01"), SubmissionId("sub-02"), SubmissionId("sub-01"))


        when(mockRequestBuilder.execute[List[SubmissionId]](any(), any()))
          .thenReturn(Future.successful(submissionIds))


        val result: List[SubmissionId] = client.list(testUserId).futureValue

        result mustBe submissionIds
      }

      "fail the future and log an error when the HTTP call fails" in new TestSetup {
        val exception = new RuntimeException("connection failed")

        when(mockRequestBuilder.execute[List[SubmissionId]](any(), any()))
          .thenReturn(Future.failed(exception))

        val result: Future[List[SubmissionId]] = client.list(testUserId)

        result.failed.futureValue mustBe exception
      }
    }
  }
}
