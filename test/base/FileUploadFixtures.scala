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

package base

import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, StcFileValidationResponse, StcRowValidationError, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}


trait FileUploadFixtures {

  val reference = "test-reference"

  val successfulValidationResponse: StcFileValidationResponse = StcFileValidationResponse(
    rows = Seq.empty
  )

  def readyFileUpload(reference: String = "ref123"): FileUpload =
    FileUpload(
      reference = reference,
      status = UpscanJourneyStatus.Ready,
      downloadUrl = Some("http://download"),
      uploadDetails = None,
      failureReason = None,
      message = None
    )

  val failedFileUpload: FileUpload =
    FileUpload(
      reference = reference,
      status = UpscanJourneyStatus.Failed
    )  

  def parsedStcRow(rowNumber: Int = 6): ParsedStcRow =
    ParsedStcRow(
      rowNumber = rowNumber,
      sellerName = Some("   "),
      sellerAddressInUk = Some(true),
      sellerAddressLine1 = Some("   "),
      sellerAddressLine2 = Some("   "),
      sellerAddressLine3 = None,
      sellerAddressLine4 = None,
      sellerPostcode = Some("   "),
      sellerCountry = None,
      connectedPersons = Some(false),
      applyingForRelief = Some(false),
      whatReliefAreYouApplyingFor = Some("   "),
      securitiesTarget = Some("   "),
      companyRegistrationNumber = Some("   "),
      chargingPoint = None,
      taxRate = None,
      whatTypeOfSecurities = Some("   "),
      typeOfShares = Some("   "),
      securitiesQuantity = Some(BigDecimal(1)),
      amountPaidForSecurities = Some(BigDecimal(1)),
      totalMarketValue = Some(BigDecimal(1)),
      minSharePrice = Some(BigDecimal(1)),
      maxSharePrice = Some(BigDecimal(1)),
      sharePurchaseReason = Some("   "),
      purchaseForCancellation = Some(false)
    )

  def validationResponseWithErrors(errors: Seq[StcRowValidationError]): StcFileValidationResponse =
    StcFileValidationResponse(
      rows = Seq(
        ValidatedStcRow(
          parsedRow = parsedStcRow(),
          validationErrors = errors
        )
      )
    )

  val blockingValidationErrors: Seq[StcRowValidationError] =
    Seq(
      StcRowValidationError(
        rowNumber = 6,
        fieldName = "sellerName",
        columnIndex = 7,
        message = "Enter the seller's full name",
        blocking = true
      ),
      StcRowValidationError(
        rowNumber = 6,
        fieldName = "sellerAddressLine1",
        columnIndex = 9,
        message = "Enter the first line of your address",
        blocking = true
      )
    )  

  def withBlockingErrors(count: Int): Seq[StcRowValidationError] =
    (1 to count).map { i =>
      StcRowValidationError(
        rowNumber = i + 2,
        fieldName = "sellerName",
        columnIndex = 7,
        message = s"Error $i",
        blocking = true
      )
    }
}