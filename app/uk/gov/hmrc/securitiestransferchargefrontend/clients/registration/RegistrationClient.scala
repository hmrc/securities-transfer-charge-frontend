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
import uk.gov.hmrc.securitiestransferchargefrontend.models.Address

import java.time.LocalDateTime
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

trait RegistrationClient:
  def getSubscriptionDetails(stcId: String)(implicit hc: HeaderCarrier): Future[Subscription]

  def getSubscriptionStatus(stcId: String)(implicit hc: HeaderCarrier): Future[SubscriptionStatusResult]

  def updateAddress(stcId: String, address: Address)(implicit hc: HeaderCarrier): Future[SubscriptionResult]

// DUMMY IMPL until we have a real BE implementation for this.
class RegistrationClientImpl @Inject()(implicit ec: ExecutionContext) extends RegistrationClient {

  val subscription: Subscription = Subscription(
    subsValidTo = LocalDateTime.now(),
    contactName = "John Doe",
    addressLine1 = "1 high street",
    addressLine2 = Some("bobbins on sea"),
    addressLine3 = Some("Town"),
    postcode = "ZZ1 1ZZ",
    countryCode = "GB",
    telephoneNumber = "07777777777",
    emailAddress = "some@email.com"
  )

  override def getSubscriptionDetails(stcId: String)
                                     (implicit hc: HeaderCarrier): Future[Subscription] =
    Future.successful(subscription)

  override def getSubscriptionStatus(stcId: String)
                                    (implicit hc: HeaderCarrier): Future[SubscriptionStatusResult] =
    Future.successful(Right(SubscriptionActive))

  override def updateAddress(stcId: String, address: Address)(implicit hc: HeaderCarrier): Future[SubscriptionResult] =
    Future.successful(Right(AddressUpdateSuccessful))
}
