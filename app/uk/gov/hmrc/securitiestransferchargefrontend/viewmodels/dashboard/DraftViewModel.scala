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

import play.api.i18n.Messages
import uk.gov.hmrc.govukfrontend.views.viewmodels.summarylist.SummaryList
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Clock, LocalDate, LocalTime, ZoneId}
import java.util.Locale

sealed trait DraftDeletion {
  def message(implicit messages: Messages): String
}

object DraftDeletion {

  private val timeFormatter = DateTimeFormatter.ofPattern("h:mma", Locale.UK)

  private def formatTime(time: LocalTime): String = time.format(timeFormatter).toLowerCase(Locale.UK)

  final case class InDays(days: Long) extends DraftDeletion {
    def message(implicit messages: Messages): String = messages("draft.deletion.p1", days)
  }

  final case class Tomorrow(at: LocalTime) extends DraftDeletion {
    def message(implicit messages: Messages): String = messages("draft.deletion.tomorrow")
  }

  final case class Today(at: LocalTime) extends DraftDeletion {
    def message(implicit messages: Messages): String = messages("draft.deletion.today", formatTime(at))
  }
}

case class DraftViewModel(
                           deletion: DraftDeletion,
                           submissionId: String,
                           sections: Seq[(String, SummaryList)]
                         )

object DraftViewModel {

  private val ukZone: ZoneId      = ZoneId.of("Europe/London")

  def build(userAnswers: UserAnswers, affinityKey: String, clock: Clock, draftTtlDays: Int)(implicit messages: Messages): DraftViewModel =
    DraftViewModel(
      deletion     = deletion(userAnswers, clock, draftTtlDays),
      submissionId = userAnswers.submissionId.value,
      sections     = DraftSummaryList.build(DraftSummary.getSummary(userAnswers), affinityKey, userAnswers.journeyType)
                       .filter { case (_, summaryList) => summaryList.rows.nonEmpty }
    )

  private[dashboard] def deletion(userAnswers: UserAnswers, clock: Clock, draftTtlDays: Int): DraftDeletion = {
    val deletionAt = userAnswers.createdAt.plus(draftTtlDays.toLong, ChronoUnit.DAYS).atZone(ukZone)
    val today      = LocalDate.now(clock.withZone(ukZone))
    val daysLeft   = ChronoUnit.DAYS.between(today, deletionAt.toLocalDate)

    daysLeft match {
      case d if d <= 0 => DraftDeletion.Today(deletionAt.toLocalTime)
      case 1           => DraftDeletion.Tomorrow(deletionAt.toLocalTime)
      case d           => DraftDeletion.InDays(d)
    }
  }
}
