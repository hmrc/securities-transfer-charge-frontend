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
      buyerName =
        readString(row, columnIndex.find(StcColumns.buyerName)),

      buyerAddressInUK =
        readBoolean(row, columnIndex.find(StcColumns.buyerAddressInUK)),

      buyerAddressLine1 =
        readString(row, columnIndex.find(StcColumns.buyerAddressLine1)),

      buyerAddressLine2 =
        readString(row, columnIndex.find(StcColumns.buyerAddressLine2)),

      buyerAddressLine3 =
        readString(row, columnIndex.find(StcColumns.buyerAddressLine3)),

      buyerAddressLine4 =
        readString(row, columnIndex.find(StcColumns.buyerAddressLine4)),
      buyerPostcode =
        readString(row, columnIndex.find(StcColumns.buyerPostcode)),

      buyerCountry =
        readString(row, columnIndex.find(StcColumns.buyerCountry)),

      sellerName =
        readString(row, columnIndex.find(StcColumns.sellerName)),

      sellerAddressInUK =
        readBoolean(row, columnIndex.find(StcColumns.sellerAddressInUK)),

      sellerAddressLine1 =
        readString(row, columnIndex.find(StcColumns.sellerAddressLine1)),

      sellerAddressLine2 =
        readString(row, columnIndex.find(StcColumns.sellerAddressLine2)),

      sellerAddressLine3 =
        readString(row, columnIndex.find(StcColumns.sellerAddressLine3)),

      sellerAddressLine4 =
        readString(row, columnIndex.find(StcColumns.sellerAddressLine4)),

      sellerPostcode =
        readString(row, columnIndex.find(StcColumns.sellerPostcode)),

      sellerCountry =
        readString(row, columnIndex.find(StcColumns.sellerCountry)),

      connectedPersons =
        readBoolean(row, columnIndex.find(StcColumns.connectedPersons)),

      applyingForRelief =
        readBoolean(row, columnIndex.find(StcColumns.applyingForRelief)),

      whatReliefAreYouApplyingFor =
        readString(row, columnIndex.find(StcColumns.whatRelief)),

      securitiesTarget =
        readString(row, columnIndex.find(StcColumns.securitiesTarget)),

      companyRegistrationNumber =
        readString(row, columnIndex.find(StcColumns.companyRegistrationNumber)),

      chargingPoint =
        readParsedDate(row, columnIndex.find(StcColumns.chargingPoint)),

      taxRate =
        readTaxRate(row, columnIndex.find(StcColumns.taxRate)),

      whatTypeOfSecurities =
        readString(row, columnIndex.find(StcColumns.whatTypeOfSecurities)),

      typeOfShares =
        readString(row, columnIndex.find(StcColumns.typeOfShares)),

      securitiesQuantity =
        readString(row, columnIndex.find(StcColumns.securitiesQuantity)),

      amountPaidForSecurities =
        readString(row, columnIndex.find(StcColumns.amountPaidForSecurities)),

      totalMarketValue =
        readString(row, columnIndex.find(StcColumns.totalMarketValue)),

      minSharePrice = 
        readString(row, columnIndex.find(StcColumns.minSharePrice)),
      
      maxSharePrice = 
        readString(row, columnIndex.find(StcColumns.maxSharePrice)),

      sharePurchaseReason =
        readString(row, columnIndex.find(StcColumns.purchaseReason)),

      purchaseForCancellation =
        readBoolean(row, columnIndex.find(StcColumns.purchasedForCancellation))
    )
}