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
import uk.gov.hmrc.securitiestransferchargefrontend.services.{BundledReliefs, ReliefsServiceImpl}

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Level, Logger}
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory

import java.time.{Clock, Duration, Instant, ZoneOffset}
import scala.jdk.CollectionConverters.*
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

  // What the bundled file holds in tests: two reliefs, so a fallback is recognisable.
  private val bundledList = Seq(Relief("Bundled One", 100), Relief("Bundled Two", 50))
  private val bundled     = new BundledReliefs { override val reliefs: Seq[Relief] = bundledList }

  private val service = new ReliefsServiceImpl(client, cache, appConfig, bundled, clock)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(client, cache, appConfig)
    when(appConfig.reliefsCacheTtl).thenReturn(Duration.ofDays(1))
    when(cache.store(any(), any())).thenReturn(Future.successful(()))
  }

  private def cachedAt(fetchedAt: Instant): Unit =
    when(cache.retrieve()).thenReturn(Future.successful(Some(CachedReliefs(cachedReliefs, fetchedAt))))

  // Captures what ReliefsServiceImpl logs at WARN or above while `block` runs. The test logback config turns
  // logging off, so the logger's level is set to WARN for the duration and restored afterwards.
  private def logsDuring(block: => Any): Seq[ILoggingEvent] = {
    val logger        = LoggerFactory.getLogger(classOf[ReliefsServiceImpl]).asInstanceOf[Logger]
    val originalLevel = logger.getLevel
    val appender      = new ListAppender[ILoggingEvent]()
    appender.start()
    logger.setLevel(Level.WARN)
    logger.addAppender(appender)
    try {
      block
      appender.list.asScala.toSeq
    } finally {
      logger.detachAppender(appender)
      logger.setLevel(originalLevel)
    }
  }

  private def warnings(events: Seq[ILoggingEvent]): Seq[String] =
    events.filter(_.getLevel == Level.WARN).map(_.getFormattedMessage)

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

    "must use the bundled reliefs, with a warning, when nothing is cached and the reliefs cannot be fetched" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.failed(new ReliefsUnavailableException("down")))

      val events = logsDuring {
        service.getReliefs().futureValue mustBe bundledList
      }

      warnings(events) must contain allOf(
        "Could not get reliefs and none are cached",
        "Using the 2 reliefs bundled with the service"
      )
    }

    "must not cache the bundled reliefs, so the submissions service is asked again next time" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.failed(new ReliefsUnavailableException("down")))

      service.getReliefs().futureValue mustBe bundledList

      verify(cache, never()).store(any(), any())
    }

    "must fail when nothing is cached, the reliefs cannot be fetched and there are no bundled reliefs" in {
      val empty = new BundledReliefs { override val reliefs: Seq[Relief] = Seq.empty }
      val noBundled = new ReliefsServiceImpl(client, cache, appConfig, empty, clock)
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.failed(new ReliefsUnavailableException("down")))

      noBundled.getReliefs().failed.futureValue mustBe a[ReliefsUnavailableException]
    }

    "must drop reliefs whose percentage is outside 1 to 100, with a warning, and cache the rest" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      val received = Seq(
        Relief("Valid Low", 1),
        Relief("Zero", 0),
        Relief("Negative", -5),
        Relief("Valid High", 100),
        Relief("Too High", 101)
      )
      when(client.getReliefs()(any())).thenReturn(Future.successful(received))

      val events = logsDuring {
        service.getReliefs().futureValue mustBe Seq(Relief("Valid Low", 1), Relief("Valid High", 100))
      }

      verify(cache).store(Seq(Relief("Valid Low", 1), Relief("Valid High", 100)), now)
      warnings(events) must contain allOf(
        "Dropping relief 'Zero': percentage 0 is not between 1 and 100",
        "Dropping relief 'Negative': percentage -5 is not between 1 and 100",
        "Dropping relief 'Too High': percentage 101 is not between 1 and 100"
      )
    }

    "must keep the cached reliefs, and not overwrite them, when an empty list is received" in {
      cachedAt(now.minus(Duration.ofHours(25)))
      when(client.getReliefs()(any())).thenReturn(Future.successful(Seq.empty))

      service.getReliefs().futureValue mustBe cachedReliefs

      verify(cache, never()).store(any(), any())
    }

    "must keep the cached reliefs, and not overwrite them, when every received relief is invalid" in {
      cachedAt(now.minus(Duration.ofHours(25)))
      when(client.getReliefs()(any())).thenReturn(Future.successful(Seq(Relief("Zero", 0), Relief("Too High", 101))))

      service.getReliefs().futureValue mustBe cachedReliefs

      verify(cache, never()).store(any(), any())
    }

    "must use the bundled reliefs, and not cache anything, when an empty list is received and nothing is cached" in {
      when(cache.retrieve()).thenReturn(Future.successful(None))
      when(client.getReliefs()(any())).thenReturn(Future.successful(Seq.empty))

      service.getReliefs().futureValue mustBe bundledList

      verify(cache, never()).store(any(), any())
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
