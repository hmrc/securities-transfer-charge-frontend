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

package repositories

import org.mockito.Mockito.when
import org.scalatest.OptionValues
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.ProcessingStatus
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.Status.*
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ProcessingStatusRepositoryImpl

import scala.concurrent.ExecutionContext.Implicits.global

class ProcessingStatusRepositorySpec
  extends AnyFreeSpec
    with Matchers
    with DefaultPlayMongoRepositorySupport[ProcessingStatus]
    with ScalaFutures
    with IntegrationPatience
    with OptionValues
    with MockitoSugar {

  private val mockAppConfig = mock[FrontendAppConfig]
  when(mockAppConfig.processingStatusTtl).thenReturn(1L)

  override protected val repository: ProcessingStatusRepositoryImpl =
    new ProcessingStatusRepositoryImpl(mongoComponent, mockAppConfig)

  // See note below
  override protected val checkIndexedQueries: Boolean = false

  private val group        = GroupIdentifier("group-1")
  private val otherGroup   = GroupIdentifier("group-2")
  private val submissionA  = SubmissionId("submission-A")
  private val submissionB  = SubmissionId("submission-B")

  "findBySubmissionId" - {

    "must return the status of the requested submission when multiple submissons are present in the group" in {
      repository.insert(group, submissionA, Processing).futureValue
      repository.insert(group, submissionB, Failed).futureValue

      repository.findBySubmissionId(group, submissionB).futureValue mustBe Some(Failed)
    }

    "must return None when the submission does not exist" in {
      repository.insert(group, submissionA, Processing).futureValue

      repository.findBySubmissionId(group, submissionB).futureValue mustBe None
    }

    "must return None when the submission exists but belongs to a different group" in {
      repository.insert(otherGroup, submissionA, Processing).futureValue

      repository.findBySubmissionId(group, submissionA).futureValue mustBe None
    }
  }

  "update" - {
    "must store PartiallyFailed so it can be read back" in {
      val failures = Map(3 -> "Failed", 7 -> "Missing field")

      repository.insert(group, submissionA, Processing).futureValue
      repository.update(group, submissionA, PartiallyFailed(failures)).futureValue

      repository.findBySubmissionId(group, submissionA).futureValue mustBe Some(PartiallyFailed(failures))
    }
  }
}