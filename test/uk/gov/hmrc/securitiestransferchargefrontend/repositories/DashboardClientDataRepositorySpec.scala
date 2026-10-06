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

package uk.gov.hmrc.securitiestransferchargefrontend.repositories

import base.{Fixtures, SpecBase}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.DashboardClientDataRepository

class DashboardClientDataRepositorySpec extends SpecBase with BeforeAndAfterEach {

  protected val databaseName: String = "dashboard-client-cache-store"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application =
    applicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )
      .build()

  private lazy val repository = application.injector.instanceOf[DashboardClientDataRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.dropCollection())
  }

  val testKey = "Overdue:STT887226"
  val testResponse = EtmpTransactionSummaryResponse(
    success = Fixtures.buildEtmpSuccessResponse(
      transactionDetails = List(
        Fixtures.buildSubmissionDetails(
          submissionId = Fixtures.testSubmissionId,
          isPast = true,
          utrn = Fixtures.testUtrn
        )
      ),
      chargeDetails = List(
        Fixtures.buildTaxCharge(Fixtures.testUtrn, true, false)
      )
    )
  )

  "The repository must" - {

    "store a data record" in {
      await(repository.store(key = testKey, value = testResponse))
    }

    "retrieve a data record" in {
      await(repository.store(key = testKey, value = testResponse))
      await(repository.retrieve(testKey)) mustBe Some(testResponse)
    }

    "return false when checksum does not exist" in {
      await(repository.retrieve("missing-checksum")) mustBe None
    }
  }
}
