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

import base.SpecBase
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief

import java.time.Instant

class ReliefsCacheRepositorySpec extends SpecBase with BeforeAndAfterEach {

  private val mongoUri: String = "mongodb://127.0.0.1:27017/reliefs-cache-test"

  private lazy val application: Application =
    applicationBuilder().configure("mongodb.uri" -> mongoUri).build()

  private lazy val repository = application.injector.instanceOf[ReliefsCacheRepository]

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.dropCollection())
  }

  private val fetchedAt = Instant.parse("2026-11-27T12:00:00Z")
  private val reliefs   = Seq(Relief("Charities Relief", 50), Relief("Group Relief", 100))

  "The reliefs cache repository must" - {

    "return nothing when no reliefs have been stored" in {
      await(repository.retrieve()) mustBe None
    }

    "return the reliefs that were stored, with when they were fetched" in {
      await(repository.store(reliefs, fetchedAt))

      await(repository.retrieve()) mustBe Some(CachedReliefs(reliefs, fetchedAt))
    }

    "replace the stored reliefs rather than adding to them" in {
      await(repository.store(reliefs, fetchedAt))
      val later = fetchedAt.plusSeconds(3600)
      await(repository.store(Seq(Relief("PISCES", 100)), later))

      await(repository.retrieve()) mustBe Some(CachedReliefs(Seq(Relief("PISCES", 100)), later))
    }
  }
}
