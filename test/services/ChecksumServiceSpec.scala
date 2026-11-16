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

package services

import base.SpecBase
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.UploadedFileChecksum
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ChecksumRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.ChecksumServiceImpl

import scala.concurrent.Future

class ChecksumServiceSpec extends SpecBase with MockitoSugar {

  private val hash = "upscan-checksum"

  trait TestSetup {
    val mockChecksumRepository: ChecksumRepository = mock[ChecksumRepository]
    val service = new ChecksumServiceImpl(mockChecksumRepository)
  }

  "ChecksumService" - {

    "storeHash" - {

      "must store the hash against the user group" in new TestSetup {
        when(mockChecksumRepository.insert(any[UploadedFileChecksum])).thenReturn(Future.successful(()))

        service.storeHash(testGroupIdentifier, hash).futureValue mustBe ()

        val captor: ArgumentCaptor[UploadedFileChecksum] = ArgumentCaptor.forClass(classOf[UploadedFileChecksum])
        verify(mockChecksumRepository, times(1)).insert(captor.capture())
        captor.getValue.checksum mustBe hash
        captor.getValue.userGroup mustBe testGroupIdentifier
      }

      "must fail when the hash cannot be stored" in new TestSetup {
        when(mockChecksumRepository.insert(any[UploadedFileChecksum])).thenReturn(Future.failed(new RuntimeException("mongo down")))

        service.storeHash(testGroupIdentifier, hash).failed.futureValue mustBe a[RuntimeException]
      }
    }

    "isDuplicate" - {

      "must return true when the hash has already been stored for the user group" in new TestSetup {
        when(mockChecksumRepository.exists(testGroupIdentifier, hash)).thenReturn(Future.successful(true))

        service.isDuplicate(testGroupIdentifier, hash).futureValue mustBe true
      }

      "must return false when the hash has not been stored for the user group" in new TestSetup {
        when(mockChecksumRepository.exists(testGroupIdentifier, hash)).thenReturn(Future.successful(false))

        service.isDuplicate(testGroupIdentifier, hash).futureValue mustBe false
      }
    }
  }
}
