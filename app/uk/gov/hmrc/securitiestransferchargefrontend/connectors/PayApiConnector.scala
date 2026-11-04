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

import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.pay.{StartPaymentJourneyRequest, StartPaymentJourneyResponse}
import play.api.Logging
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, StringContextOps, UpstreamErrorResponse}
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PayApiConnector @Inject()(
                                 httpClientV2: HttpClientV2,
                                 appConfig: FrontendAppConfig
                               )(implicit ec: ExecutionContext) extends Logging {

  import uk.gov.hmrc.http.HttpReads.Implicits._

  private val startJourneyUrl = url"${appConfig.payApiBaseUrl}/pay-api/stamp-taxes-on-shares/journey/start"

  def startPaymentJourney(request: StartPaymentJourneyRequest)(
    implicit hc: HeaderCarrier
  ): Future[StartPaymentJourneyResponse] = {
    httpClientV2
      .post(startJourneyUrl)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .flatMap { response =>
        response.status match {
          case CREATED =>
            Future.fromTry(response.json.validate[StartPaymentJourneyResponse].asEither.left.map { errors =>
              UpstreamErrorResponse(s"Failed to parse Pay API 201 response: $errors", CREATED)
            }.toTry)
          case status =>
            logger.error(s"Pay API start journey failed with status $status: ${response.body}")
            Future.failed(UpstreamErrorResponse(response.body, status))
        }
      }
  }
}