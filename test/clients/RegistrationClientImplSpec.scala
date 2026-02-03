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

package clients


import base.SpecBase
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.RegistrationClientImpl
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.SubscriptionResponse.AddressUpdateSuccessful
import uk.gov.hmrc.securitiestransferchargefrontend.models.Address
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubscriptionId

class RegistrationClientImplSpec extends SpecBase {

  private val client = new RegistrationClientImpl()
  private val subscriptionId = SubscriptionId("XAST0123456789")

  "RegistrationClientImpl.getSubscriptionDetails" - {

    "return subscription details" in {
      whenReady(client.getSubscriptionDetails(subscriptionId)) { subscription =>
        subscription.contactName mustBe "John Doe"
        subscription.addressLine1 mustBe "1 high street"
        subscription.addressLine2 mustBe Some("Town")
        subscription.addressLine3 mustBe empty
        subscription.postcode mustBe "ZZ1 1ZZ"
        subscription.countryCode mustBe "GB"
        subscription.telephoneNumber mustBe "07777777777"
        subscription.emailAddress mustBe "some@email.com"
      }
    }
  }

  "RegistrationClientImpl.getSubscriptionStatus" - {

    "return an active subscription status" in {
      whenReady(client.getSubscriptionStatus(subscriptionId)) { result =>
        result mustBe Right(SubscriptionActive)
      }
    }
  }


  "RegistrationClientImpl.updateAddress" - {

    "return AddressUpdateSuccessful when updating an address" in {
      val address = Address(
        addressLine1 = "2 low street",
        addressLine2 = Some("Seaside"),
        addressLine3 = Some("Village"),
        postcode = "AA1 1AA",
        countryCode = "GB"
      )

      whenReady(client.updateAddress(subscriptionId, address)) { result =>
        result mustBe Right(AddressUpdateSuccessful)
      }
    }
  }
}
