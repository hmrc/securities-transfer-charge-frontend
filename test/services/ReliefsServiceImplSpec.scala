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

package services

import base.SpecBase
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{never, reset, verify, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{ReliefsClient, ReliefsUnavailableException}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.{CachedReliefs, ReliefsCache}
import uk.gov.hmrc.securitiestransferchargefrontend.services.ReliefsServiceImpl

import java.time.{Clock, Duration, Instant, ZoneOffset}
import scala.concurrent.Future

class ReliefsServiceImplSpec extends SpecBase with MockitoSugar with BeforeAndAfterEach {

  private implicit val hc: HeaderCarrier = HeaderCarrier()
  private implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  private val now   = Instant.parse("2026-11-27T12:00:00Z")
  private val clock = Clock.fixed(now, ZoneOffset.UTC)

  private val cachedReliefs   = Seq(Relief("Cached Relief", 10))
  private val refreshedReliefs = Seq(Relief("Refreshed Relief", 20))

  private val client    = mock[ReliefsClient]
  private val cache     = mock[ReliefsCache]
  private val appConfig = mock[FrontendAppConfig]

  private val service = new ReliefsServiceImpl(client, cache, appConfig, clock)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(client, cache, appConfig)
    when(appConfig.reliefsCacheTtl).thenReturn(Duration.ofDays(1))
    when(cache.store(any(), any())).thenReturn(Future.successful(()))
  }

  private def cachedAt(fetchedAt: Instant): Unit =
    when(cache.retrieve()).thenReturn(Future.successful(Some(CachedReliefs(cachedReliefs, fetchedAt))))

  "ReliefsServiceImpl.getReliefs" - {

    "must return the cached reliefs without calling the submissions service when they are less than 24 hours old" in {
      cachedAt(now.minus(Duration.ofHours(23)))

      service.getReliefs().futureValue mustBe cachedReliefs

      verify(client, never()).getReliefs()(any())
    }

    "must reload and cache the reliefs when the cached reliefs are more than 24 hours old" in {
      cachedAt(now.minus(Duration.ofHours(25)))
      when(client.getReliefs()(any())).thenReturn(Future.successful(refreshedReliefs))

      service.getReliefs().futureValue mustBe refreshedReliefs

      verify(cache).store(refreshedReliefs, now)
    }

    "must fetch and cache the reliefs when nothing is cached" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.successful(refreshedReliefs))

      service.getReliefs().futureValue mustBe refreshedReliefs

      verify(cache).store(refreshedReliefs, now)
    }

    "must use the expired cached reliefs as a backup when they cannot be reloaded" in {
      cachedAt(now.minus(Duration.ofDays(30)))
      when(client.getReliefs()(any())).thenReturn(Future.failed(new ReliefsUnavailableException("down")))

      service.getReliefs().futureValue mustBe cachedReliefs

      verify(cache, never()).store(any(), any())
    }

    "must fail when nothing is cached and the reliefs cannot be fetched" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.failed(new ReliefsUnavailableException("down")))

      service.getReliefs().failed.futureValue mustBe a[ReliefsUnavailableException]
    }

    "must still return the reloaded reliefs when saving them to the cache fails" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.successful(refreshedReliefs))
      when(cache.store(any(), any())).thenReturn(Future.failed(new RuntimeException("mongo down")))

      service.getReliefs().futureValue mustBe refreshedReliefs
    }

    "must go to the submissions service when reading the cache fails" in {
      when(cache.retrieve()).thenReturn(Future.failed(new RuntimeException("mongo down")))
      when(client.getReliefs()(any())).thenReturn(Future.successful(refreshedReliefs))

      service.getReliefs().futureValue mustBe refreshedReliefs
    }

    "must honour a different cache TTL" in {
      when(appConfig.reliefsCacheTtl).thenReturn(Duration.ofHours(1))
      cachedAt(now.minus(Duration.ofHours(2)))
      when(client.getReliefs()(any())).thenReturn(Future.successful(refreshedReliefs))

      service.getReliefs().futureValue mustBe refreshedReliefs
    }
  }
}
