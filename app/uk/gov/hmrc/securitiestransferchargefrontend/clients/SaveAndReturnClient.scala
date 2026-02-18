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

package uk.gov.hmrc.securitiestransferchargefrontend.clients

import play.api.Logging
import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import scala.util.Failure

import scala.concurrent.ExecutionContext
import javax.inject.Inject
import scala.concurrent.Future


trait SaveAndReturnClient:
  def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]

  def retrieve(userId: String, submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers]

  def list(userId: String)(implicit hc: HeaderCarrier): Future[List[SubmissionId]]


class SaveAndReturnClientImpl @Inject(http: HttpClientV2, config: FrontendAppConfig)(implicit ec: ExecutionContext) extends SaveAndReturnClient with Logging {

  override def save(userAnswers: UserAnswers)
                   (implicit hc: HeaderCarrier): Future[Unit] = {

    val url = url"${config.saveUserAnswersUrl}"

    http.post(url)
      .withBody(Json.toJson(userAnswers))
      .execute[HttpResponse]
      .map(_ => ())
      .andThen {
        case Failure(e) => logger.error(s"Failed to save UserAnswers for userId=${userAnswers.userId}", e)
      }
  }

  override def retrieve(
                         userId: String,
                         submissionId: SubmissionId
                       )(implicit hc: HeaderCarrier): Future[UserAnswers] = {

    val url = url"${config.retrieveUserAnswersUrl}/$userId/$submissionId"

    http.get(url)
      .execute[UserAnswers]
      .andThen {
        case Failure(e) =>
          logger.error(s"Failed to retrieve UserAnswers for userId=$userId, submissionId=$submissionId", e)
      }
  }

  override def list(userId: String)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] = {

    val url = url"${config.retrieveUserAnswersUrl}/$userId"

    http.get(url)
      .execute[List[SubmissionId]]
      .recover {
        _ =>
          logger.info(s"No submission Ids available for user ${userId}")
          List.empty[SubmissionId]
      }

  }
}
