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

package uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload

sealed trait StcUploadColumn {
  def index: Int
}

object StcUploadColumn {
  case object AddressLine1 extends StcUploadColumn                   { val index = 0 }
  case object AddressLine2 extends StcUploadColumn                   { val index = 1 }
  case object AddressLine3 extends StcUploadColumn                   { val index = 2 }
  case object AddressLine4 extends StcUploadColumn                   { val index = 3 }
  case object Postcode extends StcUploadColumn                       { val index = 4 }
  case object Country extends StcUploadColumn                        { val index = 5 }
  case object SellerName extends StcUploadColumn                     { val index = 6 }
  case object SellerAddressInUK extends StcUploadColumn              { val index = 7 }
  case object SellerAddressLine1 extends StcUploadColumn             { val index = 8 }
  case object SellerAddressLine2 extends StcUploadColumn             { val index = 9 }
  case object SellerAddressLine3 extends StcUploadColumn             { val index = 10 }
  case object SellerAddressLine4 extends StcUploadColumn             { val index = 11 }
  case object SellerPostcode extends StcUploadColumn                 { val index = 12 }
  case object SellerCountry extends StcUploadColumn                  { val index = 13 }
  case object ConnectedPersons extends StcUploadColumn               { val index = 14 }
  case object ApplyingForRelief extends StcUploadColumn              { val index = 15 }
  case object WhatReliefAreYouApplyingFor extends StcUploadColumn    { val index = 16 }
  case object SecuritiesTarget extends StcUploadColumn               { val index = 17 }
  case object ChargingPoint extends StcUploadColumn                  { val index = 18 }
  case object TaxRate extends StcUploadColumn                        { val index = 19 }
  case object WhatTypeOfSecurities extends StcUploadColumn           { val index = 20 }
  case object OtherSecuritiesType extends StcUploadColumn            { val index = 21 }
  case object AmountPaidForSecurities extends StcUploadColumn        { val index = 22 }
  case object TotalMarketValue extends StcUploadColumn               { val index = 23 }
}