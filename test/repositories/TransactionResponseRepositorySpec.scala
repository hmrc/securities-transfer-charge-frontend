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
import org.mongodb.scala.SingleObservableFuture
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.TransactionResponseRepositoryImpl
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import java.time.LocalDate

class TransactionResponseRepositorySpec extends SpecBase with BeforeAndAfterEach {

  protected val databaseName: String = "transaction-response-test"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application =
    applicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )
      .build()

  private lazy val repository =
    application.injector.instanceOf[TransactionResponseRepositoryImpl]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.collection.drop().toFuture())
  }

  private def createResponseSuccess(
                                     chargeReferences: Seq[String] = Seq("CHARGE-REF-001"),
                                     taxDue: BigDecimal = BigDecimal("1000.00"),
                                     paymentDueBy: LocalDate = LocalDate.of(2024, 12, 31),
                                     agentReference: Option[String] = None
                                   )(submissionId: SubmissionId): SubmissionCreateResponseSuccess =
    SubmissionCreateResponseSuccess(
      submissionId = submissionId,
      chargeReferences = chargeReferences,
      taxDue = taxDue,
      paymentDueBy = paymentDueBy,
      agentReference = agentReference
    )

  "store" - {

    "must store a transaction response successfully" in {
      val submissionId = SubmissionId("STC-123456789")
      val responseSuccess = createResponseSuccess()(submissionId)

      await(repository.store(submissionId, responseSuccess))

      val retrieved = await(repository.retrieve(submissionId))
      retrieved.submissionId mustEqual submissionId
      retrieved.paymentDueBy mustEqual responseSuccess.paymentDueBy
      retrieved.taxDue mustEqual responseSuccess.taxDue
    }

    "must replace existing transaction response when storing with same submission ID" in {
      val submissionId = SubmissionId("STC-987654321")
      val firstResponse = createResponseSuccess(taxDue = BigDecimal("500.00"))(submissionId)
      val secondResponse = createResponseSuccess(taxDue = BigDecimal("1500.00"))(submissionId)

      await(repository.store(submissionId, firstResponse))
      await(repository.store(submissionId, secondResponse))

      val retrieved = await(repository.retrieve(submissionId))
      retrieved.taxDue mustEqual secondResponse.taxDue
    }
  }

  "retrieve" - {

    "must retrieve a stored transaction response" in {
      val submissionId = SubmissionId("STC-123456789")
      val responseSuccess = createResponseSuccess()(submissionId)

      await(repository.store(submissionId, responseSuccess))

      val retrieved = await(repository.retrieve(submissionId))
      retrieved.submissionId mustEqual submissionId
      retrieved.taxDue mustEqual responseSuccess.taxDue
    }

    "must throw NoSuchElementException when transaction response does not exist" in {
      val submissionId = SubmissionId("STC-NONEXISTENT")

      val exception = intercept[NoSuchElementException] {
        await(repository.retrieve(submissionId))
      }

      exception.getMessage must include("No transaction response found")
    }
  }
}
