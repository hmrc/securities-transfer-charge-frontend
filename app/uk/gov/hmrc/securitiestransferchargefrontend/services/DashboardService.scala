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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubscriptionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.services.SubmissionStatus.{Draft, Overdue}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.Future

enum SubmissionStatus:
  case Draft, Processing, ReadyToPay, Paid, Overdue, PartialFailure, Failed
  override def toString: String =
    this match
      case ReadyToPay     => "Ready to pay"
      case PartialFailure => "Partial failure"
      case _              => super.toString


final case class SubmissionSummary(
                                    submissionId: String,
                                    paymentDueBy: String,
                                    status: SubmissionStatus,
                                    sortDate: LocalDate
                                  )

final case class DashboardCounts(
                                  drafts: Int,
                                  readyToPay: Int,
                                  overdue: Int
                                )

trait DashboardService:
  def getCounts(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[DashboardCounts]
  def getDrafts(userId: UserId, groupId: GroupIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]
  def getReadyToPay(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]
  def getOverdue(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]
  def getRecent(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]

final class DashboardServiceImpl @Inject()(

                                          ) extends DashboardService {


  private val sorted = (summaries: Seq[SubmissionSummary]) =>
    summaries.sortBy(_.sortDate)(Ordering[LocalDate].reverse)

  private val overdue: Seq[SubmissionSummary] = List(
    SubmissionSummary(
      submissionId = "STC-00000008",
      paymentDueBy = "2026-10-10",
      status = Overdue,
      sortDate = LocalDate.parse("2026-09-10")
    ),
    SubmissionSummary(
      submissionId = "STC-00000009",
      paymentDueBy = "2026-10-11",
      status = Overdue,
      sortDate = LocalDate.parse("2026-09-11")
    ),
    SubmissionSummary(
      submissionId = "STC-00000012",
      paymentDueBy = "2026-10-12",
      status = Overdue,
      sortDate = LocalDate.parse("2026-09-12")
    )
  )

  private val readyToPay: Seq[SubmissionSummary] = List(
    SubmissionSummary(
      submissionId = "STC-00000018",
      paymentDueBy = "2026-10-08",
      status = Overdue,
      sortDate = LocalDate.parse("2026-09-08")
    ),
    SubmissionSummary(
      submissionId = "STC-00000019",
      paymentDueBy = "2026-10-19",
      status = Overdue,
      sortDate = LocalDate.parse("2026-09-19")
    )
  )

  private val drafts: Seq[SubmissionSummary] = List(
    SubmissionSummary(
      submissionId = "STC-00000219",
      paymentDueBy = "N/A",
      status = Draft,
      sortDate = LocalDate.parse("2026-10-19")
    )
  )

  override def getCounts(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[DashboardCounts] =
    if (subscriptionId.value.toLowerCase.contains("empty")) {
      Future.successful(DashboardCounts(drafts = 0, readyToPay = 0, overdue = 0))
    } else {
      Future.successful(
        DashboardCounts(
          drafts = 1,
          readyToPay = 2,
          overdue = 3
        )
      )
    }

  override def getDrafts(userId: UserId, groupId: GroupIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] =
    Future.successful(sorted(drafts))

  override def getReadyToPay(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] =
    Future.successful(sorted(readyToPay))

  override def getOverdue(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] =
    Future.successful(sorted(overdue))

  override def getRecent(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] =
    if (subscriptionId.value.toLowerCase.contains("empty")) {
      Future.successful(Seq.empty)
    } else {
      Future.successful(sorted(readyToPay ++ overdue ++ drafts))
    }
}