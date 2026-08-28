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
import play.api.http.Status.CREATED
import play.api.libs.json.*
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.{StcTransactionCreateResponse, SubmissionBatchPayload}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

import uk.gov.hmrc.http.HttpResponse
import uk.gov.hmrc.http.HttpReads.Implicits._

trait EtmpSubmissionClient:
  def singleStfSubmission(payload: SubmissionBatchPayload)(implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse]

class EtmpSubmissionClientImpl @Inject()(
  http: HttpClientV2,
  appConfig: FrontendAppConfig)(
  implicit ec: ExecutionContext                                      
) extends EtmpSubmissionClient with Logging:

  final class SubmissionResponseParsingException(s: String) extends RuntimeException(s)
  final class SubmissionResponseException(s: String) extends RuntimeException(s)

  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger = logger)
  private val logInfoAndFailParsing = (s: String) => logInfoAndFail(new SubmissionResponseParsingException(s))
  private val logInfoAndFailNon201 = (s: String) => logInfoAndFail(new SubmissionResponseException(s))
  
  def singleStfSubmission(payload: SubmissionBatchPayload)(implicit hc: HeaderCarrier): Future[StcTransactionCreateResponse] = {
    http.post(url"${appConfig.submissionsServiceUrl}")
      .withBody(Json.toJson(payload))
      .execute[HttpResponse]
      .flatMap {
        case response if response.status == CREATED =>
          response.json.validate[StcTransactionCreateResponse] match {
            case JsSuccess(createdResponse, _) => Future.successful(createdResponse)
            case JsError(errors) => logInfoAndFailParsing("Parse errors: " + errors.toString())
          }
        case otherResponse => logInfoAndFailNon201(s"Received $otherResponse when submitting to ETMP")
      }
    }

