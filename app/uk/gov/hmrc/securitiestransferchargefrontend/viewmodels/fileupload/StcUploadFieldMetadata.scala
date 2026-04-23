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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload

import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUploadColumn

object StcUploadFieldMetadata {

  val columnIndexByFieldName: Map[String, Int] = Map(
    "addressLine1" -> StcUploadColumn.addressLine1,
    "addressLine2" -> StcUploadColumn.addressLine2,
    "addressLine3" -> StcUploadColumn.addressLine3,
    "addressLine4" -> StcUploadColumn.addressLine4,
    "postcode" -> StcUploadColumn.postcode,
    "country" -> StcUploadColumn.country,
    "sellerName" -> StcUploadColumn.sellerName,
    "sellerAddressInUk" -> StcUploadColumn.sellerAddressInUK,
    "sellerAddressLine1" -> StcUploadColumn.sellerAddressLine1,
    "sellerAddressLine2" -> StcUploadColumn.sellerAddressLine2,
    "sellerAddressLine3" -> StcUploadColumn.sellerAddressLine3,
    "sellerAddressLine4" -> StcUploadColumn.sellerAddressLine4,
    "sellerPostcode" -> StcUploadColumn.sellerPostcode,
    "sellerCountry" -> StcUploadColumn.sellerCountry,
    "connectedPersons" -> StcUploadColumn.connectedPersons,
    "applyingForRelief" -> StcUploadColumn.applyingForRelief,
    "whatReliefAreYouApplyingFor" -> StcUploadColumn.whatReliefAreYouApplyingFor,
    "securitiesTarget" -> StcUploadColumn.securitiesTarget,
    "companyRegistrationNumber" -> StcUploadColumn.whatIsCRN,
    "chargingPoint" -> StcUploadColumn.chargingPoint,
    "taxRate" -> StcUploadColumn.taxRate,
    "whatTypeOfSecurities" -> StcUploadColumn.whatTypeOfSecurities,
    "typeOfShares" -> StcUploadColumn.typeOfShares,
    "securitiesQuantity" -> StcUploadColumn.securitiesQuantity,
    "amountPaidForSecurities" -> StcUploadColumn.amountPaidForSecurities,
    "totalMarketValue" -> StcUploadColumn.totalMarketValue
  )
}