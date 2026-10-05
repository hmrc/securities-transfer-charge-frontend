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

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.search.EtmpTransactionSummaryResponse

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait DashboardClientDataCache:
  def store(key: String, value: EtmpTransactionSummaryResponse): Future[Unit]
  def retrieve(key: String): Future[Option[EtmpTransactionSummaryResponse]]
  
object DashboardClientDataCache:
  val recentKey = "Recent"
  val readyToPayKey = "ReadyToPay"
  val overdueKey = "Overdue"

class DashboardClientCache @Inject() (
  dashboardClient: DashboardClient,
  cache: DashboardClientDataCache
)(implicit val ec: ExecutionContext) extends DashboardClient {
  
  private type ServiceCall = () => Future[EtmpTransactionSummaryResponse]
  
  override def getRecentTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    val serviceCall = () => dashboardClient.getRecentTransactions(subscriptionId)
    checkCache(DashboardClientDataCache.recentKey, serviceCall)

  override def getReadyToPayTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    val serviceCall = () => dashboardClient.getRecentTransactions(subscriptionId)
    checkCache(DashboardClientDataCache.readyToPayKey, serviceCall)

  override def getOverdueTransactions(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[EtmpTransactionSummaryResponse] =
    val serviceCall = () => dashboardClient.getOverdueTransactions(subscriptionId)
    checkCache(DashboardClientDataCache.overdueKey, serviceCall)
   
  val cacheHit: EtmpTransactionSummaryResponse => Future[EtmpTransactionSummaryResponse] = Future.successful
  val cacheMiss: String => ServiceCall => Future[EtmpTransactionSummaryResponse] = key => serviceCall => for {
    resp <- serviceCall()
  } yield {
    cache.store(key, resp)
    resp
  }
  
  def checkCache(key: String, serviceCall: ServiceCall): Future[EtmpTransactionSummaryResponse] = {
    cache
      .retrieve(key)
      .flatMap(
        _.fold(
          cacheMiss(key)(serviceCall))(cacheHit)
      )
  }
}
