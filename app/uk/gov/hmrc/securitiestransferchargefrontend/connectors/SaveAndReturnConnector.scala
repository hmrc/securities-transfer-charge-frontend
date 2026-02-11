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

package uk.gov.hmrc.securitiestransferchargefrontend.connectors

import com.google.inject.Inject
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import scala.concurrent.Future

trait SaveAndReturnConnector {

  def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]

  def retrieve(userId: String, submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers]

  def list(userId: String)(implicit hc: HeaderCarrier): Future[List[SubmissionId]]

}

class SaveAndReturnConnectorImpl @Inject()(saveAndReturnClient: SaveAndReturnClient)
  extends SaveAndReturnConnector {

  override def retrieve(userId: String, submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers] =
    saveAndReturnClient.retrieve(userId, submissionId)

  override def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit] =
    saveAndReturnClient.save(userAnswers)

  override def list(userId: String)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] =
    saveAndReturnClient.list(userId)
}
