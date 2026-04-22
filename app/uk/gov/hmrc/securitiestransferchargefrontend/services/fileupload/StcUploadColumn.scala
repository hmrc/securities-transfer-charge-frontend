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

object StcUploadColumn {
  val addressLine1                    = 1 
  val addressLine2                    = 2 
  val addressLine3                    = 3 
  val addressLine4                    = 4 
  val postcode                        = 5 
  val country                         = 6 
  val sellerName                      = 7 
  val sellerAddressInUK               = 8 
  val sellerAddressLine1              = 9 
  val sellerAddressLine2              = 10
  val sellerAddressLine3              = 11
  val sellerAddressLine4              = 12
  val sellerPostcode                  = 13
  val sellerCountry                   = 14
  val connectedPersons                = 15
  val applyingForRelief               = 16
  val whatReliefAreYouApplyingFor     = 17
  val securitiesTarget                = 18
  val whatIsCRN                       = 19
  val chargingPoint                   = 20
  val taxRate                         = 21
  val whatTypeOfSecurities            = 22
  val typeOfShares                    = 23
  val securitiesQuantity              = 24  
  val amountPaidForSecurities         = 25
  val totalMarketValue                = 26
}