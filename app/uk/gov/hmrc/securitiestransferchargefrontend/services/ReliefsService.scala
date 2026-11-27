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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.ReliefsClient
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CachedReliefs, ReliefsCache}

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

/** Supplies the list of reliefs.
  *
  * Reliefs are cached in Mongo and refreshed from the submissions service once they are older than
  * `appConfig.reliefsCacheTtl`. Expired reliefs are kept: if the refresh fails they are used as a backup. Only when
  * nothing has ever been cached and the submissions service fails does the failure reach the caller.
  */
trait ReliefsService:
  def getReliefs()(implicit hc: HeaderCarrier): Future[Seq[Relief]]

@Singleton
class ReliefsServiceImpl @Inject()(
  client: ReliefsClient,
  cache: ReliefsCache,
  appConfig: FrontendAppConfig,
  clock: Clock
)(implicit ec: ExecutionContext) extends ReliefsService with Logging:

  override def getReliefs()(implicit hc: HeaderCarrier): Future[Seq[Relief]] =
    readCache().flatMap {
      case Some(cached) if isFresh(cached) => Future.successful(cached.reliefs)
      case existing                        => refresh(existing)
    }

  private def isFresh(cached: CachedReliefs): Boolean =
    cached.fetchedAt.plus(appConfig.reliefsCacheTtl).isAfter(Instant.now(clock))

  private def refresh(existing: Option[CachedReliefs])(implicit hc: HeaderCarrier): Future[Seq[Relief]] =
    client.getReliefs().flatMap { reliefs =>
      cache
        .store(reliefs, Instant.now(clock))
        .recover { case NonFatal(e) => logger.warn("Could not save reliefs to the cache", e) }
        .map(_ => reliefs)
    }.recoverWith { case NonFatal(e) =>
      existing match
        case Some(stale) =>
          logger.warn(s"Could not refresh reliefs, using the reliefs fetched at ${stale.fetchedAt}", e)
          Future.successful(stale.reliefs)
        case None =>
          Future.failed(e)
    }

  // A broken cache should not stop us asking the submissions service
  private def readCache(): Future[Option[CachedReliefs]] =
    cache.retrieve().recover { case NonFatal(e) =>
      logger.warn("Could not read reliefs from the cache", e)
      None
    }
