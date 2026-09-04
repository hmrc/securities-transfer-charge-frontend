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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionCreateResponseSuccess

import javax.inject.Inject
import scala.concurrent.Future

trait TransactionResponseRepository:
  def store(key: SubmissionId, value: SubmissionCreateResponseSuccess): Future[Unit]
  def retrieve(key: SubmissionId): Future[SubmissionCreateResponseSuccess]
  
// TODO: This needs to be implemented as part of the first NRS ticket.  
final class TransactionResponseRepositoryImpl @Inject() extends TransactionResponseRepository {

  def store(key: SubmissionId, value: SubmissionCreateResponseSuccess): Future[Unit] = Future.failed(new NotImplementedError())

  def retrieve(key: SubmissionId): Future[SubmissionCreateResponseSuccess] = Future.failed(new NotImplementedError())

}