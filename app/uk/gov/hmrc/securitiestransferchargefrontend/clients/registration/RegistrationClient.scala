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

package uk.gov.hmrc.securitiestransferchargefrontend.clients.registration

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.SubscriptionResponse.AddressUpdateSuccessful
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.SubscriptionStatus.SubscriptionActive
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.{Subscription, SubscriptionStatusResult}
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.Address

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.Future

trait RegistrationClient:
  def getSubscriptionDetails(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[Subscription]
  def getSubscriptionStatus(subscriptionId: SubscriptionId)(implicit hc: HeaderCarrier): Future[SubscriptionStatusResult]
  def updateAddress(subscriptionId: SubscriptionId, address: Address)(implicit hc: HeaderCarrier): Future[SubscriptionResult]

// DUMMY IMPL until we have a real BE implementation for this.
class RegistrationClientImpl @Inject() extends RegistrationClient {

  val subscription: Subscription = Subscription(
    subsValidTo = LocalDateTime.now(),
    contactName = "John Doe",
    addressLine1 = "1 high street",
    addressLine2 = Some("Town"),
    addressLine3 = None,
    postcode = "ZZ1 1ZZ",
    countryCode = "GB",
    telephoneNumber = "07777777777",
    emailAddress = "some@email.com"
  )

  override def getSubscriptionDetails(subscriptionId: SubscriptionId)
                                     (implicit hc: HeaderCarrier): Future[Subscription] =
    Future.successful(subscription)

  override def getSubscriptionStatus(subscriptionId: SubscriptionId)
                                    (implicit hc: HeaderCarrier): Future[SubscriptionStatusResult] =
    Future.successful(Right(SubscriptionActive))

  override def updateAddress(subscriptionId: SubscriptionId, address: Address)(implicit hc: HeaderCarrier): Future[SubscriptionResult] =
    Future.successful(Right(AddressUpdateSuccessful))
}
