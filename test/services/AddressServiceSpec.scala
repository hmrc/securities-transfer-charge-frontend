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

package services


import base.SpecBase
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CountriesList, Country}
import uk.gov.hmrc.securitiestransferchargefrontend.services.AddressService

class AddressServiceSpec extends SpecBase {

  private val countriesList = mock[CountriesList]
  private val service = new AddressService(countriesList)
  
  "AddressService" - {

    "build a ConfirmableAddress from a full subscription" in {


      when(countriesList.fromCode("GB"))
        .thenReturn(Some(Country("United Kingdom", "GB")))

      val result = service.extractConfirmableAddress(subscription)

      result.lines mustBe Seq("1 high street", "Town")
      result.postcode mustBe "ZZ1 1ZZ"
      result.country.value.name mustBe "United Kingdom"
    }

    "return None for unknown country codes" in {

      when(countriesList.fromCode("XX")).thenReturn(None)

      val result = service.extractConfirmableAddress(subscription.copy(countryCode = "XX"))

      result.lines mustBe Seq("1 high street", "Town")
      result.postcode mustBe "ZZ1 1ZZ"
      result.country mustBe None
    }


  }
}

