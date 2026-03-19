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

import play.api.Logging
import play.api.libs.json.{JsError, JsSuccess, Json}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.upscan.{UpscanInitiateRequest, UpscanInitiateResponse}
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


trait UpscanInitiateConnector {

  def initiate()(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse]
}

class UpscanInitiateConnectorImpl @Inject()(http: HttpClientV2,
                                            config: FrontendAppConfig,
                                           )
                                           (implicit ec: ExecutionContext) extends UpscanInitiateConnector with Logging {

  private final class UpscanException(msg: String) extends RuntimeException(msg)

  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)
  
  val request: UpscanInitiateRequest = UpscanInitiateRequest(config.upscanCallbackUrl,
    Some(config.upscanUploadSuccessfulUrl),
    Some(config.upscanUploadFailureUrl))


  def initiate()(implicit hc: HeaderCarrier): Future[UpscanInitiateResponse] =
    http
      .post(url"${config.upscanBaseUrl}/upscan/v2/initiate")
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap(handleUpscanResponse)
  
  
  
  private def handleUpscanResponse(response: HttpResponse): Future[UpscanInitiateResponse] =
    response.status match {

      case 200 =>
        response.json.validate[UpscanInitiateResponse] match {
          case JsSuccess(value, _) => Future.successful(value)
          case JsError(errors) => logInfoAndFail(UpscanException(s"Failed to parse UpscanInitiateResponse: $errors. Body: ${response.body}"))
        }

      case status => logInfoAndFail(UpstreamErrorResponse(s"Upscan initiate failed with status $status and body ${response.body}", status))
    }
}