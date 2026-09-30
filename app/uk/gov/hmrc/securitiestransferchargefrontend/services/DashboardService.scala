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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.{DashboardClient, SaveAndReturnClient}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubscriptionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswersSummary
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.{DashboardCounts, EtmpChargeDetail, EtmpTransactionSummaryResponse, SubmissionStatus, SubmissionSummary}
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpSuccessResponseExtensions.*
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.SubmissionStatus.{Draft, Overdue, Paid, ReadyToPay}
import uk.gov.hmrc.securitiestransferchargefrontend.services.DashboardService.draftToSubmissionSummary
import uk.gov.hmrc.securitiestransferchargefrontend.utils.DateTimeFormats

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

trait DashboardService:
  def getCounts(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[DashboardCounts]
  def getRecent(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]

  def getDrafts(userId: UserId, groupId: GroupIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]
  def getReadyToPay(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]
  def getOverdue(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]]

final class DashboardServiceImpl @Inject()(
  saveAndReturnClient: SaveAndReturnClient,
  dashboardClient: DashboardClient
)(using
  ec: ExecutionContext
) extends DashboardService {

  import DashboardService.*
  import SubmissionSummary.sorted

  override def getDrafts(userId: UserId, groupId: GroupIdentifier)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] = for {
    drafts    <- saveAndReturnClient.getDraftSummaries(userId, groupId)
    summaries  = drafts.map(draftToSubmissionSummary)
  } yield sorted(summaries)

  override def getReadyToPay(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] = for {
    txs      <- dashboardClient.getReadyToPayTransactions(subscriptionId)
    summaries = toSummaryList(txs, Some(ReadyToPay))
  } yield sorted(summaries)

  override def getOverdue(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] = for {
    txs      <- dashboardClient.getOverdueTransactions(subscriptionId)
    summaries = toSummaryList(txs, Some(Overdue))
  } yield sorted(summaries)

  override def getRecent(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Seq[SubmissionSummary]] = for {
    recent   <- dashboardClient.getRecentTransactions(subscriptionId)
    drafts   <- getDrafts(userId, groupId)
    summaries = toSummaryList(recent)
  } yield sorted(drafts ++ summaries)

  override def getCounts(userId: UserId, groupId: GroupIdentifier, subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[DashboardCounts] = for {
    drafts          <- getDrafts(userId, groupId)
    overdueCount    <- dashboardClient.getOverdueTransactionsCount(subscriptionId)
    readyToPayCount <- dashboardClient.getReadyToPayTransactionsCount(subscriptionId)
  } yield DashboardCounts(
    drafts     = drafts.length,
    readyToPay = readyToPayCount,
    overdue    = overdueCount
  )
}

object DashboardService:

  private[services] val draftToSubmissionSummary: UserAnswersSummary => SubmissionSummary = uas =>
    SubmissionSummary(
      submissionId = uas.submissionId.value,
      maybePaymentDueBy = None,
      status = Draft,
      sortDate = LocalDate.ofInstant(uas.lastUpdated, DateTimeFormats.ukZoneId)
    )

  private[services] val submissionDueBy: Seq[EtmpChargeDetail] => Option[LocalDate] =
    charges =>
      charges
        .map(_.chargeDueDate)
        .minOption


  private[services] def toSummaryList(txs: EtmpTransactionSummaryResponse, maybeStatus: Option[SubmissionStatus] = None): Seq[SubmissionSummary] = {
    val submissions =
      txs
      .success
      .toSttSubmissions
    for {
      submission <- submissions
      charges = submission.transfers.flatMap(_.charges)
    } yield
        SubmissionSummary(
          submissionId = submission.submissionId,
          maybePaymentDueBy = submissionDueBy(charges).map(_.format(DateTimeFormatter.ISO_DATE)),
          status = maybeStatus.getOrElse(getSubmissionStatus(charges)),
          sortDate = submission.submissionDate
        )
  }

  def getSubmissionStatus: Seq[EtmpChargeDetail] => SubmissionStatus = charges =>
    if charges.exists(_.isOverdue) then Overdue
    else if charges.exists(_.isUnpaid) then ReadyToPay
    else Paid
