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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import javax.inject.Inject
import scala.concurrent.Future
import scala.util.Random

trait SubmissionIdClient:
  def nextSubmissionId(): Future[SubmissionId]

class SubmissionIdClientImpl @Inject() extends SubmissionIdClient {
  private val rnd = new Random()

  // TODO: Stubbed implementation - replace with call to S&R service.
  override def nextSubmissionId(): Future[SubmissionId] = {
    val x = rnd.nextInt(1_000_000_000).abs
    val submissionId = SubmissionId(f"STC-$x%09d")
    Future.successful(submissionId)
  }

}
