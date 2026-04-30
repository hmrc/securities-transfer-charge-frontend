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

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedRow, ParsedStcRow}

import javax.inject.Singleton

@Singleton
class StcRowMapper(columnIndex: ColumnIndexBuilder) {

  import SafeRowReader.*

  def map(row: ParsedRow): ParsedStcRow =
    ParsedStcRow(
      rowNumber = row.rowNumber,

      sellerName =
        readString(row, columnIndex.get(StcColumns.sellerName)),

      sellerAddressInUk =
        readBoolean(row, columnIndex.get(StcColumns.sellerAddressInUk)),

      sellerAddressLine1 =
        readString(row, columnIndex.get(StcColumns.sellerAddressLine1)),

      sellerAddressLine2 =
        readString(row, columnIndex.get(StcColumns.sellerAddressLine2)),

      sellerAddressLine3 =
        readString(row, columnIndex.get(StcColumns.sellerAddressLine3)),

      sellerAddressLine4 =
        readString(row, columnIndex.get(StcColumns.sellerAddressLine4)),

      sellerPostcode =
        readString(row, columnIndex.get(StcColumns.sellerPostcode)),

      sellerCountry =
        readString(row, columnIndex.get(StcColumns.sellerCountry)),

      connectedPersons =
        readBoolean(row, columnIndex.get(StcColumns.connectedPersons)),

      applyingForRelief =
        readBoolean(row, columnIndex.get(StcColumns.applyingForRelief)),

      whatReliefAreYouApplyingFor =
        readString(row, columnIndex.get(StcColumns.whatRelief)),

      securitiesTarget =
        readString(row, columnIndex.get(StcColumns.securitiesTarget)),

      companyRegistrationNumber =
        readString(row, columnIndex.get(StcColumns.whatIsCRN)),

      chargingPoint =
        readDate(row, columnIndex.get(StcColumns.chargingPoint)),

      taxRate =
        readTaxRate(row, columnIndex.get(StcColumns.taxRate)),

      whatTypeOfSecurities =
        readString(row, columnIndex.get(StcColumns.whatTypeOfSecurities)),

      typeOfShares =
        readString(row, columnIndex.get(StcColumns.typeOfShares)),

      securitiesQuantity =
        readBigDecimal(row, columnIndex.get(StcColumns.securitiesQuantity)),

      amountPaidForSecurities =
        readBigDecimal(row, columnIndex.get(StcColumns.amountPaidForSecurities)),

      totalMarketValue =
        readBigDecimal(row, columnIndex.get(StcColumns.totalMarketValue)),

      minSharePrice =
        readBigDecimal(row, columnIndex.get(StcColumns.minSharePrice)),
      maxSharePrice = readBigDecimal(row, columnIndex.get(StcColumns.maxSharePrice)),

      sharePurchaseReason =
        readString(row, columnIndex.get(StcColumns.purchaseReason)),

      purchaseForCancellation =
        readBoolean(row, columnIndex.get(StcColumns.purchasedForCancellation))
    )
}