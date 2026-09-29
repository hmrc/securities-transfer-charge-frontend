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

package uk.gov.hmrc.securitiestransferchargefrontend.clients.registration

import play.api.libs.json.Json
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.securitiestransferchargefrontend.models.nrs.NrsSingleSubmissionRequest
import play.api.libs.ws.JsonBodyWritables.writeableOf_JsValue
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.http.HttpReads.Implicits.*

import java.net.URL
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait NrsClient:
  def postSinglePayload(request: NrsSingleSubmissionRequest)(using hc: HeaderCarrier): Future[Unit]

final class NrsClientImpl @Inject() (
  http: HttpClientV2,
  config: FrontendAppConfig
)(
  using ec: ExecutionContext
) extends NrsClient:
  
  override def postSinglePayload(request: NrsSingleSubmissionRequest)(using hc: HeaderCarrier): Future[Unit] = {
    val url: URL = url"${config.submissionsServiceUrl}/nrs/single"
    http
      .post(url)
      .withBody(Json.toJson(request))
      .execute[HttpResponse]
      .map(_ => ())
  }

