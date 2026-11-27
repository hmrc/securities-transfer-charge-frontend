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
import play.api.http.Status.OK
import play.api.libs.functional.syntax.*
import play.api.libs.json.{JsError, JsSuccess, Json, Reads, __}
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, StringContextOps}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

final class ReliefsUnavailableException(message: String) extends RuntimeException(message)

trait ReliefsClient:
  /** Gets the current list of reliefs from the submissions service. Fails with [[ReliefsUnavailableException]]. */
  def getReliefs()(implicit hc: HeaderCarrier): Future[Seq[Relief]]

class ReliefsClientImpl @Inject()(
  http: HttpClientV2,
  appConfig: FrontendAppConfig
)(implicit ec: ExecutionContext) extends ReliefsClient with Logging:

  import ReliefsClientImpl.reliefsReads

  override def getReliefs()(implicit hc: HeaderCarrier): Future[Seq[Relief]] =
    http
      .get(url"${appConfig.submissionsServiceUrl}/reliefs")
      .execute[HttpResponse]
      .map { response =>
        if response.status != OK then
          logger.warn(s"Submissions service returned ${response.status} for reliefs")
          throw new ReliefsUnavailableException(s"Submissions service returned ${response.status} for reliefs")
        else
          Json.parse(response.body).validate[Seq[Relief]] match
            case JsSuccess(reliefs, _) => reliefs
            case JsError(errors) =>
              logger.warn(s"Could not parse reliefs response: $errors")
              throw new ReliefsUnavailableException("Could not parse reliefs response")
      }

object ReliefsClientImpl:
  // The submissions service sends {"name", "percentage"}; the frontend calls the percentage "rate".
  private given reliefReads: Reads[Relief] =
    (
      (__ \ "name").read[String] and
        (__ \ "percentage").read[Int]
    )(Relief.apply)

  given reliefsReads: Reads[Seq[Relief]] = Reads.seq(using reliefReads)
