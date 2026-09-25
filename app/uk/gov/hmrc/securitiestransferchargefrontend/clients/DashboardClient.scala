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

import play.api.libs.json.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, StringContextOps}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait DashboardClient:
  def getReadyToPayTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int]

  def getOverdueTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int]

class DashboardClientImpl @Inject()(http: HttpClientV2, appConfig: FrontendAppConfig)(implicit ec: ExecutionContext) extends DashboardClient:


  override def getReadyToPayTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
    http.get(url"${appConfig.dashboardServiceUrl}/dashboard/ready-to-pay/count/${subscriptionId.value}")
      .execute[Int]

  override def getOverdueTransactionsCount(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Int] =
    http.get(url"${appConfig.dashboardServiceUrl}/dashboard/overdue/count/${subscriptionId.value}")
      .execute[Int]

