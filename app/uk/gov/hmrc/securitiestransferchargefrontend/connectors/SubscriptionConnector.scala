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
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SubscriptionDataRepository
import uk.gov.hmrc.securitiestransferchargefrontend.utils.CommonHelpers

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SubscriptionStatusErrorException(msg: String) extends RuntimeException(msg)

class SubscriptionErrorException(msg: String) extends RuntimeException(msg)

class SubscriptionDataNotFoundException(msg: String) extends RuntimeException(msg)

trait SubscriptionConnector:
  def getSubscriptionDetails(stcId: String)(implicit hc: HeaderCarrier): Future[Subscription]

  def getSubscriptionStatus(stcId: String)(implicit hc: HeaderCarrier): Future[SubscriptionStatus]

class SubscriptionConnectorImpl @Inject()(registrationClient: RegistrationClient,
                                          subscriptionDataRepository: SubscriptionDataRepository,
                                         )
                                         (implicit ec: ExecutionContext) extends SubscriptionConnector with Logging:

  private val logInfoAndFail = CommonHelpers.logInfoAndFail(logger)


  override def getSubscriptionStatus(stcId: String)(implicit hc: HeaderCarrier): Future[SubscriptionStatus] = registrationClient
    .getSubscriptionStatus(stcId)
    .flatMap(subscriptionStatusResultHandler)

  private def subscriptionStatusResultHandler(
                                               subscriptionStatusResult: SubscriptionStatusResult
                                             )(implicit hc: HeaderCarrier): Future[SubscriptionStatus] =
    subscriptionStatusResult match {

      case Left(error) =>
        val msg = s"SubscriptionConnector: Failed to retrieve subscription status.Upstream service returned an error: $error"
        logInfoAndFail(new SubscriptionStatusErrorException(msg))
      case Right(SubscriptionActive) =>
        Future.successful(SubscriptionActive)

      case Right(otherStatus) =>
        val msg = s"SubscriptionConnector: Subscription status was not active.Received status: $otherStatus"
        logInfoAndFail(new SubscriptionStatusErrorException(msg))
    }


  override def getSubscriptionDetails(stcId: String)
                                     (implicit hc: HeaderCarrier): Future[Subscription] = for {
    subscription <- registrationClient.getSubscriptionDetails(stcId)
    _ <- subscriptionDataRepository.saveSubscriptionData(stcId, subscription)
  } yield subscription

    







