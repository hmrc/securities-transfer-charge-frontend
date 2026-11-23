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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard

import base.Fixtures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.i18n.{DefaultMessagesApi, Lang, Messages, MessagesImpl}
import uk.gov.hmrc.securitiestransferchargefrontend.models.JourneyType.STF
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftDeletion.*
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.dashboard.DraftViewModel

import java.time.{Clock, LocalDateTime, LocalTime, ZoneId}

class DraftsViewModelSpec extends AnyWordSpec with Matchers {

  private val uk = ZoneId.of("Europe/London")

  private val ttlDays = 30

  private def at(dateTime: String) = LocalDateTime.parse(dateTime).atZone(uk).toInstant

  private def clockAt(dateTime: String) = Clock.fixed(at(dateTime), uk)

  private def answersCreatedAt(dateTime: String): UserAnswers =
    UserAnswers(Fixtures.testInternalId, Fixtures.testGroupIdentifier, Fixtures.testSubmissionId, STF, createdAt = at(dateTime))

  private implicit val messages: Messages = MessagesImpl(
    Lang("en"),
    new DefaultMessagesApi(Map("en" -> Map(
      "draft.deletion.p1"       -> "Your draft will be deleted in {0} days.",
      "draft.deletion.today"    -> "Your draft will be deleted today at {0}.",
      "draft.deletion.tomorrow" -> "Your draft will be deleted tomorrow."
    )))
  )

  "deletion" should {

    "show 30 days on the day the draft is saved" in {
      DraftViewModel.deletion(answersCreatedAt("2026-11-01T09:00"), clockAt("2026-11-01T23:00"), ttlDays) shouldBe InDays(30)
    }

    "count full calendar days, not hours" in {
      DraftViewModel.deletion(answersCreatedAt("2026-11-01T23:00"), clockAt("2026-11-02T01:00"), ttlDays) shouldBe InDays(29)
    }

    "show 2 days the day before tomorrow" in {
      DraftViewModel.deletion(answersCreatedAt("2026-11-01T09:00"), clockAt("2026-11-29T09:00"), ttlDays) shouldBe InDays(2)
    }

    "show tomorrow" in {
      val result = DraftViewModel.deletion(answersCreatedAt("2026-11-01T09:00"), clockAt("2026-11-30T15:00"), ttlDays)
      result shouldBe Tomorrow(LocalTime.of(9, 0))
      result.message shouldBe "Your draft will be deleted tomorrow."
    }

    "show today with the deletion time" in {
      val result = DraftViewModel.deletion(answersCreatedAt("2026-11-01T14:30"), clockAt("2026-12-01T08:00"), ttlDays)
      result shouldBe Today(LocalTime.of(14, 30))
      result.message shouldBe "Your draft will be deleted today at 2:30pm."
    }

    "show today rather than a negative number once the deletion time has passed" in {
      DraftViewModel.deletion(answersCreatedAt("2026-11-01T09:00"), clockAt("2026-12-03T10:00"), ttlDays) shouldBe a[Today]
    }

    "use the configured time to live" in {
      DraftViewModel.deletion(answersCreatedAt("2026-11-01T09:00"), clockAt("2026-11-01T23:00"), 14) shouldBe InDays(14)
    }

    "show 30 days message" in {
      InDays(12).message shouldBe "Your draft will be deleted in 12 days."
    }
  }
}
