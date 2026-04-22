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

package uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload

import java.time.LocalDate

final case class UploadedTransfer(
                                   rowNumber: Int,
                                   sellerName: String,
                                   sellerAddress: UploadedSellerAddress,
                                   connectedPersons: Boolean,
                                   relief: Option[String],
                                   securitiesTarget: UploadedSecuritiesTarget,
                                   chargingPoint: LocalDate,
                                   taxRate: BigDecimal,
                                   securityDetails: UploadedSecurityDetails,
                                   securitiesQuantity: BigDecimal,
                                   amountPaidForSecurities: BigDecimal,
                                   totalMarketValue: Option[BigDecimal]
                                 )

sealed trait UploadedSellerAddress

object UploadedSellerAddress {

  final case class UkAddress(
                              line1: String,
                              line2: Option[String],
                              line3: Option[String],
                              line4: Option[String],
                              postcode: String
                            ) extends UploadedSellerAddress

  final case class NonUkAddress(
                                 country: String
                               ) extends UploadedSellerAddress
}

final case class UploadedSecuritiesTarget(
                                           businessName: String,
                                           companyRegistrationNumber: Option[String]
                                         )

sealed trait UploadedSecurityDetails

object UploadedSecurityDetails {

  final case class Shares(
                           typeOfShares: String
                         ) extends UploadedSecurityDetails

  final case class Other(
                          description: String
                        ) extends UploadedSecurityDetails
}