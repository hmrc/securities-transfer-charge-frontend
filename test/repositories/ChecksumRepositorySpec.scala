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