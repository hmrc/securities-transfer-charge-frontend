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
import play.api.http.Status.NO_CONTENT
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure


trait SaveAndReturnClient:
  def save(userAnswers: UserAnswers)(implicit hc: HeaderCarrier): Future[Unit]
  def retrieve(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[UserAnswers]
  def listByUser(userId: UserId)(implicit hc: HeaderCarrier): Future[List[SubmissionId]]
  def listByGroup(groupIdentifier: GroupIdentifier)(implicit hc: HeaderCarrier): Future[List[SubmissionId]]
  def deleteDraft(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[Unit]


class SaveAndReturnClientImpl @Inject(http: HttpClientV2, config: FrontendAppConfig)(implicit ec: ExecutionContext) extends SaveAndReturnClient with Logging {

  private val baseUrl = config.saveAndReturnUrl
  private val userAnswersPath = s"$baseUrl/user-answers"

  override def save(userAnswers: UserAnswers)
                   (implicit hc: HeaderCarrier): Future[Unit] = {

    http.post(url"$userAnswersPath")
      .withBody(Json.toJson(userAnswers))
      .execute[HttpResponse]
      .map {
        case response if response.status == NO_CONTENT => ()
        case otherResponse =>
          logger.error(s"Failed to save UserAnswers for userId=${userAnswers.userId}. Received status ${otherResponse.status}")
          throw new RuntimeException(s"Failed to save UserAnswers. Status: ${otherResponse.status}")
      }
      .andThen {
        case Failure(e) =>
          logger.error(s"Failed to save UserAnswers for userId=${userAnswers.userId}, submissionId=${userAnswers.submissionId}", e)
      }
  }

  override def retrieve(submissionId: SubmissionId
                       )(implicit hc: HeaderCarrier): Future[UserAnswers] = {

    http.get(url"$userAnswersPath/$submissionId")
      .execute[UserAnswers]
      .andThen {
        case Failure(e) =>
          logger.error(s"Failed to retrieve UserAnswers for submissionId=$submissionId", e)
      }
  }

  override def listByGroup(groupIdentifier: GroupIdentifier)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] = {

    http
      .get(url"$userAnswersPath/search/by-group?groupId=$groupIdentifier")
      .execute[List[SubmissionId]]
      .andThen {
        case Failure(e) =>
          logger.error(s"Failed to retrieve submissionIds for groupIdentifier=$groupIdentifier", e)
      }
  }

  override def listByUser(userId: UserId)(implicit hc: HeaderCarrier): Future[List[SubmissionId]] = {

    http
      .get(url"$userAnswersPath/search/by-user?userId=$userId")
      .execute[List[SubmissionId]]
      .andThen {
        case Failure(e) =>
          logger.error(s"Failed to retrieve submissionIds for userId=$userId", e)
      }
  }

  override def deleteDraft(submissionId: SubmissionId)(implicit hc: HeaderCarrier): Future[Unit] =
    http
      .delete(url"$userAnswersPath/${submissionId.value}")
      .execute[HttpResponse]
      .map(_ => ())
  }
