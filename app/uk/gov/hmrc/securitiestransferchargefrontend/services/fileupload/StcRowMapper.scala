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

import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedRow, ParsedStcRow}

import javax.inject.Singleton

@Singleton
class StcRowMapper {

  def map(row: ParsedRow): ParsedStcRow =
    ParsedStcRow(
      rowNumber = row.rowNumber,
      addressLine1 = ParsedRowReader.readString(row, StcUploadColumn.addressLine1),
      addressLine2 = ParsedRowReader.readString(row, StcUploadColumn.addressLine2),
      addressLine3 = ParsedRowReader.readString(row, StcUploadColumn.addressLine3),
      addressLine4 = ParsedRowReader.readString(row, StcUploadColumn.addressLine4),
      postcode = ParsedRowReader.readString(row, StcUploadColumn.postcode),
      country = ParsedRowReader.readString(row, StcUploadColumn.country),
      sellerName = ParsedRowReader.readString(row, StcUploadColumn.sellerName),
      sellerAddressInUk = ParsedRowReader.readBoolean(row, StcUploadColumn.sellerAddressInUK),
      sellerAddressLine1 = ParsedRowReader.readString(row, StcUploadColumn.sellerAddressLine1),
      sellerAddressLine2 = ParsedRowReader.readString(row, StcUploadColumn.sellerAddressLine2),
      sellerAddressLine3 = ParsedRowReader.readString(row, StcUploadColumn.sellerAddressLine3),
      sellerAddressLine4 = ParsedRowReader.readString(row, StcUploadColumn.sellerAddressLine4),
      sellerPostcode = ParsedRowReader.readString(row, StcUploadColumn.sellerPostcode),
      sellerCountry = ParsedRowReader.readString(row, StcUploadColumn.sellerCountry),
      connectedPersons = ParsedRowReader.readBoolean(row, StcUploadColumn.connectedPersons),
      applyingForRelief = ParsedRowReader.readBoolean(row, StcUploadColumn.applyingForRelief),
      whatReliefAreYouApplyingFor = ParsedRowReader.readString(row, StcUploadColumn.whatReliefAreYouApplyingFor),
      securitiesTarget = ParsedRowReader.readString(row, StcUploadColumn.securitiesTarget),
      companyRegistrationNumber = ParsedRowReader.readString(row, StcUploadColumn.whatIsCRN),
      chargingPoint = ParsedRowReader.readDate(row, StcUploadColumn.chargingPoint),
      taxRate = ParsedRowReader.readBigDecimal(row, StcUploadColumn.taxRate),
      whatTypeOfSecurities = ParsedRowReader.readString(row, StcUploadColumn.whatTypeOfSecurities),
      otherSecuritiesType = ParsedRowReader.readString(row, StcUploadColumn.otherSecuritiesType),
      securitiesQuantity = ParsedRowReader.readBigDecimal(row, StcUploadColumn.securitiesQuantity),
      amountPaidForSecurities = ParsedRowReader.readBigDecimal(row, StcUploadColumn.amountPaidForSecurities),
      totalMarketValue = ParsedRowReader.readBigDecimal(row, StcUploadColumn.totalMarketValue)
    )
}