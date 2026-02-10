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

package uk.gov.hmrc.securitiestransferchargefrontend.connectors

import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SubscriptionDataRepository
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionStatusErrorException(msg: String) extends RuntimeException(msg)

trait SubscriptionConnector:

  def getValidSubscription(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Subscription]

class SubscriptionConnectorImpl @Inject()(registrationClient: RegistrationClient,
                                          subscriptionDataRepository: SubscriptionDataRepository,
                                         )
                                         (implicit ec: ExecutionContext) extends SubscriptionConnector with Logging:

  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)

  def getValidSubscription(subscriptionId: SubscriptionId)
                          (implicit hc: HeaderCarrier): Future[Subscription] = {
    for {
      subscription <- registrationClient.getSubscriptionDetails(subscriptionId)
      _ <- if (!subscription.subsValidTo.isBefore(LocalDate.now())) {
        subscriptionDataRepository.saveSubscriptionData(subscriptionId, subscription)
      } else {
        val msg = s"Subscription expired for stcId=$subscriptionId.Valid until: ${subscription.subsValidTo}"
        logInfoAndFail(new SubscriptionStatusErrorException(msg))
      }
    } yield subscription
  }
