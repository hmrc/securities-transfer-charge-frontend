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

final case class UploadFieldMetadata(
                                      fieldName: String,
                                      columnIndex: Int,
                                      questionLabel: String
                                    )

object StcUploadFieldMetadata {

  val byFieldName: Map[String, UploadFieldMetadata] = Seq(
    UploadFieldMetadata("addressLine1", StcUploadColumn.addressLine1, "Your address - line 1"),
    UploadFieldMetadata("addressLine2", StcUploadColumn.addressLine2, "Your address - line 2"),
    UploadFieldMetadata("addressLine3", StcUploadColumn.addressLine3, "Your address - line 3"),
    UploadFieldMetadata("addressLine4", StcUploadColumn.addressLine4, "Your address - line 4"),
    UploadFieldMetadata("postcode", StcUploadColumn.postcode, "Postcode"),
    UploadFieldMetadata("country", StcUploadColumn.country, "Country"),
    UploadFieldMetadata("sellerName", StcUploadColumn.sellerName, "Seller's name"),
    UploadFieldMetadata("sellerAddressInUk", StcUploadColumn.sellerAddressInUK, "Is the seller's address in the UK?"),
    UploadFieldMetadata("sellerAddressLine1", StcUploadColumn.sellerAddressLine1, "Seller's address - line 1"),
    UploadFieldMetadata("sellerAddressLine2", StcUploadColumn.sellerAddressLine2, "Seller's address - line 2"),
    UploadFieldMetadata("sellerAddressLine3", StcUploadColumn.sellerAddressLine3, "Seller's address - line 3"),
    UploadFieldMetadata("sellerAddressLine4", StcUploadColumn.sellerAddressLine4, "Seller's address - line 4"),
    UploadFieldMetadata("sellerPostcode", StcUploadColumn.sellerPostcode, "Seller's postcode"),
    UploadFieldMetadata("sellerCountry", StcUploadColumn.sellerCountry, "Seller's country"),
    UploadFieldMetadata("connectedPersons", StcUploadColumn.connectedPersons, "Are the buyer and seller connected persons?"),
    UploadFieldMetadata("applyingForRelief", StcUploadColumn.applyingForRelief, "Are you applying for a relief?"),
    UploadFieldMetadata("whatReliefAreYouApplyingFor", StcUploadColumn.whatReliefAreYouApplyingFor, "What relief are you applying for?"),
    UploadFieldMetadata("securitiesTarget", StcUploadColumn.securitiesTarget, "What is the name of the business you're buying these securities in?"),
    UploadFieldMetadata("companyRegistrationNumber", StcUploadColumn.whatIsCRN, "Company Registration Number"),
    UploadFieldMetadata("chargingPoint", StcUploadColumn.chargingPoint, "When did you buy these securities?"),
    UploadFieldMetadata("taxRate", StcUploadColumn.taxRate, "What is the tax rate for this transfer?"),
    UploadFieldMetadata("whatTypeOfSecurities", StcUploadColumn.whatTypeOfSecurities, "What type of securities are you buying?"),
    UploadFieldMetadata("otherSecuritiesType", StcUploadColumn.otherSecuritiesType, "If you are buying shares, enter the type of shares"),
    UploadFieldMetadata("securitiesQuantity", StcUploadColumn.securitiesQuantity, "How many securities are you buying?"),
    UploadFieldMetadata("amountPaidForSecurities", StcUploadColumn.amountPaidForSecurities, "How much did you pay for the securities?"),
    UploadFieldMetadata("totalMarketValue", StcUploadColumn.totalMarketValue, "What is the total market value of this transfer?")
  ).map(field => field.fieldName -> field).toMap
}