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

import base.SpecBase
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.Status
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ProcessingStatusRepositoryImpl

class ProcessingStatusRepositorySpec extends SpecBase with BeforeAndAfterEach {

  protected val databaseName: String = "processing-status-test"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application = applicationBuilder().configure("mongodb.uri" -> mongoUri).build()

  private lazy val repository: ProcessingStatusRepositoryImpl = application.injector.instanceOf[ProcessingStatusRepositoryImpl]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.dropCollection())
  }

  "insert" - {
    "store a processing status record" in {
      val submissionId = SubmissionId("submission-123")

      await(repository.insert(submissionId, Status.Processing)) mustBe ()

      await(repository.findBySubmissionIds(Seq(submissionId.value))) mustBe Set(submissionId)
    }

    "store and retrieve a partially failed status with row failures" in {
      val submissionId = SubmissionId("submission-123")

      val rowFailures = Map(4 -> Seq("Agent Details Invalid", "Declaration Details invalid"), 250 -> Seq("Transaction Limit Exceeded"))

      val status = Status.PartiallyFailed(rowFailures)

      await(repository.insert(submissionId, status)) mustBe ()

      val record = await(repository.collection.find().headOption())

      record.map(_.submissionId) mustBe Some(submissionId)
      record.map(_.status) mustBe Some(status)
    }
  }

  "update" - {
    "update the status of an existing submission" in {
      val submissionId = SubmissionId("submission-123")

      await(repository.insert(submissionId, Status.Processing))
      await(repository.update(submissionId, Status.Failed))

      await(repository.findBySubmissionIds(Seq(submissionId.value))) mustBe Set(submissionId)

      val record = await(repository.collection.find().headOption())

      record.map(_.status) mustBe Some(Status.Failed)
    }
  }

  "delete" - {
    "delete an existing processing status record" in {
      val submissionId = SubmissionId("submission-123")

      await(repository.insert(submissionId, Status.Processing))
      await(repository.delete(submissionId))

      await(repository.findBySubmissionIds(Seq(submissionId.value))) mustBe Set.empty
    }
  }

  "findBySubmissionIds" - {
    "return matching submission IDs" in {
      val submissionId1 = SubmissionId("submission-123")
      val submissionId2 = SubmissionId("submission-456")

      await(repository.insert(submissionId1, Status.Processing))
      await(repository.insert(submissionId2, Status.Failed))

      await(repository.findBySubmissionIds(Seq(submissionId1.value, submissionId2.value))) mustBe Set(submissionId1, submissionId2)
    }

    "return an empty set when no submission IDs match" in {
      await(repository.insert(SubmissionId("submission-123"), Status.Processing))

      await(repository.findBySubmissionIds(Seq("missing-submission"))) mustBe Set.empty
    }

    "return an empty set when the input is empty" in {
      await(repository.insert(SubmissionId("submission-123"), Status.Processing))

      await(repository.findBySubmissionIds(Seq.empty)) mustBe Set.empty
    }

    "return distinct submission IDs" in {
      val submissionId = SubmissionId("submission-123")

      await(repository.insert(submissionId, Status.Processing))

      await(repository.findBySubmissionIds(Seq(submissionId.value, submissionId.value))) mustBe Set(submissionId)
    }
  }
}