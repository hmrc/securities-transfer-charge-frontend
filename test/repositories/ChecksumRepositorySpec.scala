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
import play.api.test.Helpers.await
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.UploadedFileChecksum
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ChecksumRepositoryImpl
import play.api.test.Helpers.defaultAwaitTimeout

import java.time.Instant

class ChecksumRepositorySpec extends SpecBase with BeforeAndAfterEach {

  protected val databaseName: String = "checksums-test"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application =
    applicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )
      .build()

  private lazy val repository = application.injector.instanceOf[ChecksumRepositoryImpl]

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    await(repository.dropCollection())
  }

  "insert" - {

    "store a checksum record" in {

      val record =
        UploadedFileChecksum(
          checksum = "checksum-123",
          uploadedAt = Instant.now()
        )

      await(repository.insert(record)) mustBe()

    }
  }

  "exists" - {

    "return true when checksum exists" in {

      val checksum = "existing-checksum"

      await(
        repository.insert(
          UploadedFileChecksum(
            checksum = checksum
          )
        )
      )

      await(repository.exists(checksum)) mustBe true

    }

    "return false when checksum does not exist" in {

      await(repository.exists("missing-checksum")) mustBe false

    }
  }
}