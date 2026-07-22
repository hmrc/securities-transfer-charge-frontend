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
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.UpscanCallbackRequest.UploadDetails
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanDocument, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepositoryImpl

import java.time.Instant

class UpscanJourneyRepositorySpec extends SpecBase with BeforeAndAfterEach {

  protected val databaseName: String = "upscan-journey-test"

  protected val mongoUri: String = s"mongodb://127.0.0.1:27017/$databaseName"

  private lazy val application: Application =
    applicationBuilder()
      .configure(
        "mongodb.uri" -> mongoUri
      )
      .build()

  private lazy val repository =
    application.injector.instanceOf[UpscanJourneyRepositoryImpl]

  override protected def beforeEach(): Unit = {
    super.beforeEach()

    await(repository.dropCollection())
  }

  private def upscanDocument(
                              reference: String = "ref-123",
                              status: UpscanJourneyStatus = UpscanJourneyStatus.Initiated
                            ): UpscanDocument =
    UpscanDocument(
      _id = reference,
      fileUpload = FileUpload(
        reference = reference,
        status = status,
        journeyType = STF
      )
    )

  "insert" - {

    "store an upscan journey document" in {

      val document = upscanDocument()

      await(repository.insert(document))

      await(repository.find(document.fileUpload.reference)) mustBe Some(document.fileUpload)
    }

    "store and retrieve an upscan journey document with an SH03 JourneyType" in {
      val reference = "sh03-ref"
      val document = UpscanDocument(
        _id = reference,
        fileUpload = FileUpload(
          reference = reference,
          status = UpscanJourneyStatus.Initiated,
          journeyType = JourneyType.SH03
        )
      )

      await(repository.insert(document))

      val retrieved = await(repository.find(reference))
      retrieved mustBe Some(document.fileUpload)
      retrieved.get.journeyType mustBe JourneyType.SH03
    }
  }

  "find" - {

    "return the file upload when it exists" in {

      val document = upscanDocument()

      await(repository.insert(document))

      await(repository.find(document.fileUpload.reference)) mustBe Some(document.fileUpload)
    }

    "return None when it does not exist" in {

      await(repository.find("missing-reference")) mustBe None
    }
  }

  "markUploadAsSuccessful" - {

    "update the file upload as ready with download url and upload details" in {

      val reference = "successful-ref"

      val document = upscanDocument(reference)

      val uploadDetails =
        UploadDetails(
          uploadTimestamp = Instant.now(),
          checksum = "checksum",
          fileName = "test.pdf",
          fileMimeType = "application/pdf",
          size = 1234
        )

      await(repository.insert(document))

      await(
        repository.markUploadAsSuccessful(
          reference,
          "https://download-url",
          uploadDetails
        )
      )

      val result = await(repository.find(reference)).value

      result.status mustBe UpscanJourneyStatus.Ready
      result.downloadUrl mustBe Some("https://download-url")
      result.uploadDetails mustBe Some(uploadDetails)
    }
  }

  "markUploadAsFailed" - {

    "update the upload as failed with failure reason and message" in {

      val reference = "failed-ref"

      await(repository.insert(upscanDocument(reference)))

      await(
        repository.markUploadAsFailed(
          reference,
          "QUARANTINE",
          "File contains a virus"
        )
      )

      val result = await(repository.find(reference)).value

      result.status mustBe UpscanJourneyStatus.Failed
      result.failureReason mustBe Some("QUARANTINE")
      result.message mustBe Some("File contains a virus")
    }
  }

  "updateStatus" - {

    "update the status of an upload" in {

      val reference = "status-ref"

      await(repository.insert(upscanDocument(reference)))

      await(
        repository.updateStatus(
          reference,
          UpscanJourneyStatus.Ready
        )
      )

      val result = await(repository.find(reference)).value

      result.status mustBe UpscanJourneyStatus.Ready
    }
  }

  "delete" - {

    "remove an upload" in {

      val reference = "delete-ref"

      await(repository.insert(upscanDocument(reference)))

      await(repository.delete(reference))

      await(repository.find(reference)) mustBe None
    }
  }
}
