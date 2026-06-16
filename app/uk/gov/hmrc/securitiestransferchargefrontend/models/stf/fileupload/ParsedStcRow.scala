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

package uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

case class ParsedStcRow(
                         rowNumber: Int,
                         buyerName: Option[String],
                         buyerAddressInUK: Option[Boolean],
                         buyerAddressLine1: Option[String],
                         buyerAddressLine2: Option[String],
                         buyerAddressLine3: Option[String],
                         buyerAddressLine4: Option[String],
                         buyerPostcode: Option[String],
                         buyerCountry: Option[String],
                         sellerName: Option[String],
                         sellerAddressInUK: Option[Boolean],
                         sellerAddressLine1: Option[String],
                         sellerAddressLine2: Option[String],
                         sellerAddressLine3: Option[String],
                         sellerAddressLine4: Option[String],
                         sellerPostcode: Option[String],
                         sellerCountry: Option[String],
                         connectedPersons: Option[Boolean],
                         applyingForRelief: Option[Boolean],
                         whatReliefAreYouApplyingFor: Option[String],
                         securitiesTarget: Option[String],
                         companyRegistrationNumber: Option[String],
                         chargingPoint: ParsedValue[LocalDate],
                         taxRate: Option[BigDecimal],
                         whatTypeOfSecurities: Option[String],
                         typeOfShares: Option[String],
                         securitiesQuantity: Option[BigDecimal],
                         amountPaidForSecurities: Option[String],
                         totalMarketValue: Option[String],
                         minSharePrice: Option[BigDecimal],
                         maxSharePrice: Option[BigDecimal],
                         sharePurchaseReason: Option[String],
                         purchaseForCancellation: Option[Boolean]
                       )

object ParsedStcRow {
  implicit val format: OFormat[ParsedStcRow] = Json.format[ParsedStcRow]
}