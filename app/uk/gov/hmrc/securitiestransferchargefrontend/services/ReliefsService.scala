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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{ReliefsClient, ReliefsUnavailableException}
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
  *
  * Reliefs with a rate outside 1 to 100 are dropped with a warning. An empty list never replaces the cached reliefs:
  * it is treated like a failed refresh.
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
    client.getReliefs().flatMap { received =>
      val reliefs = validReliefs(received)
      if reliefs.isEmpty then
        Future.failed(new ReliefsUnavailableException("The submissions service returned no usable reliefs"))
      else
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
          logger.warn("Could not get reliefs and none are cached", e)
          Future.failed(e)
    }

  import ReliefsServiceImpl.{MaxRate, MinRate}

  // The OAS allows a relief percentage of 1 to 100. Anything else would give the wrong tax, so drop it.
  private def validReliefs(received: Seq[Relief]): Seq[Relief] =
    received.filter { relief =>
      val valid = relief.rate >= MinRate && relief.rate <= MaxRate
      if !valid then logger.warn(s"Dropping relief '${relief.name}': percentage ${relief.rate} is not between $MinRate and $MaxRate")
      valid
    }

  // A broken cache should not stop us asking the submissions service
  private def readCache(): Future[Option[CachedReliefs]] =
    cache.retrieve().recover { case NonFatal(e) =>
      logger.warn("Could not read reliefs from the cache", e)
      None
    }

object ReliefsServiceImpl:
  val MinRate = 1
  val MaxRate = 100
