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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SubmissionIdClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import scala.concurrent.{ExecutionContext, Future}

class SubmissionIdClientImplSpec extends AnyWordSpec with Matchers with ScalaFutures {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier     = HeaderCarrier()

  "SubmissionIdClientImpl" should {

    "return submission id when service responds successfully" in {

      val expected = SubmissionId("STC-000000123")

      val client = new SubmissionIdClient {
        override def nextSubmissionId()(implicit hc: HeaderCarrier): Future[SubmissionId] =
          Future.successful(expected)
      }

      val result = client.nextSubmissionId().futureValue

      result mustBe expected
    }

    "propagate failure when service fails" in {

      val client = new SubmissionIdClient {
        override def nextSubmissionId()(implicit hc: HeaderCarrier): Future[SubmissionId] =
          Future.failed(new RuntimeException("exception"))
      }

      assertThrows[RuntimeException] {
        client.nextSubmissionId().futureValue
      }
    }
  }
}
