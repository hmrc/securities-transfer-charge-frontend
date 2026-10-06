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
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse

import javax.inject.{Inject, Named}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal
import scala.util.Failure

trait DashboardClientDataCache:
  def store(key: String, value: EtmpTransactionSummaryResponse): Future[Unit]
  def retrieve(key: String): Future[Option[EtmpTransactionSummaryResponse]]
  
sealed trait DashboardClientDataCacheKey:
  def apply(id: SubscriptionId): String

case object Recent extends DashboardClientDataCacheKey:
  def apply(id: SubscriptionId): String = s"Recent:${id.value}"

case object ReadyToPay extends DashboardClientDataCacheKey:
  def apply(id: SubscriptionId): String = s"ReadyToPay:${id.value}"

case object Overdue extends DashboardClientDataCacheKey:
  def apply(id: SubscriptionId): String = s"Overdue:${id.value}"

class DashboardClientCache @Inject() (
  @Named("etmp") dashboardClient: DashboardClient,
  cache: DashboardClientDataCache
)(implicit val ec: ExecutionContext) extends DashboardClient with Logging {
  
  private type ServiceCall = () => Future[EtmpTransactionSummaryResponse]
  
  override def getRecentTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    checkCache(
      key         = Recent(subscriptionId),
      serviceCall = () => dashboardClient.getRecentTransactions(subscriptionId)
    )

  override def getReadyToPayTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    checkCache(
      key         = ReadyToPay(subscriptionId),
      serviceCall = () => dashboardClient.getReadyToPayTransactions(subscriptionId)
    )

  override def getOverdueTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    checkCache(
      key         = Overdue(subscriptionId),
      serviceCall = () => dashboardClient.getOverdueTransactions(subscriptionId)
    )
   
  private[clients] val cacheHit: EtmpTransactionSummaryResponse => Future[EtmpTransactionSummaryResponse] = Future.successful
  private[clients] def cacheMiss(key: String, serviceCall: ServiceCall): Future[EtmpTransactionSummaryResponse] = {
    serviceCall().map { resp =>
      cache.store(key, resp)
      resp
    }.andThen {
      case Failure(exception) =>
        logger.warn("Dashboard client cache failed to call the dashboard service.")
        Future.failed(exception)
    }
  }
  
  private[clients] def checkCache(key: String, serviceCall: ServiceCall): Future[EtmpTransactionSummaryResponse] =
    cache.retrieve(key).recover {
    case NonFatal(_) =>
      logger.warn("Dashboard client cache: call to repository failed.")
      None // treat failure like a cache miss
  }.flatMap {
    case Some(value) => cacheHit(value)
    case None        => cacheMiss(key, serviceCall)
  }

}
