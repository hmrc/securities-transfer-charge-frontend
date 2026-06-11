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

import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.StreamConverters
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.StringContextOps
import uk.gov.hmrc.http.client.{HttpClientV2, readSource}

import java.io.InputStream
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

final case class UpscanDownloadException(message: String, cause: Throwable)
  extends RuntimeException(message, cause)

trait UpscanFileDownloadConnector {
  def download(downloadUrl: String)(implicit hc: HeaderCarrier): Future[InputStream]
}

@Singleton
class UpscanFileDownloadConnectorImpl @Inject()(
                                                 http: HttpClientV2
                                               )(implicit ec: ExecutionContext, mat: Materializer) extends UpscanFileDownloadConnector with Logging {

  override def download(downloadUrl: String)(implicit hc: HeaderCarrier): Future[InputStream] =
    http
      .get(url"$downloadUrl")
      .stream()
      .map { source =>
        source.runWith(StreamConverters.asInputStream())
      }
      .recoverWith {
        case ex =>
          logger.warn(
            s"[UpscanFileDownloadConnectorImpl][download] Failed to download uploaded file from URL. Reason: ${ex.getMessage}",
            ex
          )
          Future.failed(
            UpscanDownloadException(
              s"Failed to download uploaded file",
              ex
            )
          )
      }
}