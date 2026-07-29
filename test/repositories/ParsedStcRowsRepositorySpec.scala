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

import base.{FileUploadFixtures, SpecBase}
import org.scalatest.BeforeAndAfterEach
import play.api.Application
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.ParsedStcRow
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ParsedStcRowsRepositoryImpl

class ParsedStcRowsRepositorySpec
  extends SpecBase
    with BeforeAndAfterEach
    with FileUploadFixtures {

  protected val databaseName: String = "parsedStcRows-test"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application =
    applicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )
      .build()

  private lazy val repository = application.injector.instanceOf[ParsedStcRowsRepositoryImpl]

  private val rows: Seq[ParsedStcRow] = Seq(parsedStcRow())
  private val reference = "Ref1234"
  private val fileName = "file.xlsx"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    await(repository.dropCollection())
  }

  "save" - {

    "store the parsed rows" in {
      await(repository.save(reference, rows, fileName))

      await(repository.findByReference(reference)) mustBe rows
    }
  }

  "findByReference" - {

    "return the stored rows when the reference exists" in {
      await(repository.save(reference, rows, fileName))

      await(repository.findByReference(reference)) mustBe rows
    }

    "return an empty sequence when the reference does not exist" in {
      await(repository.findByReference("unknown")) mustBe Seq.empty
    }
  }

  "delete" - {

    "remove the stored rows" in {
      await(repository.save(reference, rows, fileName))

      await(repository.delete(reference))

      await(repository.findByReference(reference)) mustBe Seq.empty
    }
  }
}