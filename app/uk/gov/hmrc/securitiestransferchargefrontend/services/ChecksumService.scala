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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import uk.gov.hmrc.securitiestransferchargefrontend.domain.GroupIdentifier
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.UploadedFileChecksum
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.ChecksumRepository

import javax.inject.Inject
import scala.concurrent.Future

trait ChecksumService {
  def storeHash(userGroup: GroupIdentifier, hash: String): Future[Unit]
  def isDuplicate(userGroup: GroupIdentifier, hash: String): Future[Boolean]
}

final class ChecksumServiceImpl @Inject()(checksumRepository: ChecksumRepository) extends ChecksumService {

  override def storeHash(userGroup: GroupIdentifier, hash: String): Future[Unit] =
    checksumRepository.insert(UploadedFileChecksum(checksum = hash, userGroup = userGroup))

  override def isDuplicate(userGroup: GroupIdentifier, hash: String): Future[Boolean] =
    checksumRepository.exists(userGroup, hash)
}
