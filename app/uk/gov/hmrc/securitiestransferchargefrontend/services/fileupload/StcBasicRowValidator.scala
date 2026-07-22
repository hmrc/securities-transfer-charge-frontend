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

import play.api.i18n.{Lang, Messages, MessagesApi}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.fileUpload.{SecuritiesTargetFormProvider,AmountPaidForSecuritiesFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.{NameOfBuyerFormProvider, NameOfSellerFormProvider}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, ParsedValue, StcRowValidationError}

import scala.util.{Failure, Success, Try}
import javax.inject.Inject


class StcBasicRowValidator @Inject()(
                                      support: StcValidationSupport,
                                      messagesApi: MessagesApi,
                                      nameOfSellerFormProvider: NameOfSellerFormProvider,
                                      securitiesTargetFormProvider: SecuritiesTargetFormProvider,
                                      nameOfBuyerFormProvider: NameOfBuyerFormProvider,
                                      amountPaidForSecuritiesFormProvider: AmountPaidForSecuritiesFormProvider
                                    ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  def validate(
                row: ParsedStcRow,
                template: StcTemplate,
                affinityKey:String
              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    template match {

      case StcTemplate.STF =>
        validateSTF(row, affinityKey)

      case StcTemplate.SH03 =>
        validateSH03(row, affinityKey)

      case StcTemplate.STFAgent =>
        validateAgentSTF(row, affinityKey)
    }
  }

  def validateSTF(row: ParsedStcRow,affinityKey:String)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateNameOfSeller(row) ++
      validateSellerAddressInUk(row) ++
      validateConnectedPersons(row,affinityKey) ++
      validateApplyingForRelief(row,affinityKey) ++
      validateSecuritiesTarget(row,affinityKey) ++
      validateChargingPoint(row,affinityKey) ++
      validateTaxRate(row) ++
      validateWhatTypeOfSecurities(row,affinityKey) ++
      validateSecuritiesQuantity(row,affinityKey) ++
      validateAmountPaidForSecurities(row,affinityKey)


  def validateSH03(row: ParsedStcRow,affinityKey:String)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateWhatTypeOfSecurities(row,affinityKey) ++
      validateSecuritiesQuantity(row,affinityKey) ++
      validateAmountPaidForSecurities(row,affinityKey) ++
      validateChargingPoint(row,affinityKey) ++
      validateMaxSharePrice(row) ++
      validateMinSharePrice(row) ++
      validateSharePurchaseReason(row) ++
      validatePurchasedForCancellation(row) ++
      validateConnectedPersons(row, affinityKey) ++
      validateApplyingForRelief(row, affinityKey)

  def validateAgentSTF(row: ParsedStcRow, affinityKey: String)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateNameOfBuyer(row) ++
      validateBuyerAddressInUk(row) ++
      validateNameOfSeller(row) ++
      validateSellerAddressInUk(row) ++
      validateConnectedPersons(row, affinityKey) ++
      validateApplyingForRelief(row, affinityKey) ++
      validateSecuritiesTarget(row, affinityKey) ++
      validateChargingPoint(row, affinityKey) ++
      validateTaxRate(row) ++
      validateTypeOfShares(row, affinityKey) ++
      validateSecuritiesQuantity(row, affinityKey) ++
      validateAmountPaidForSecurities(row, affinityKey)


  private def validateNameOfSeller(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    val errors = support.bindSingleValue(
      nameOfSellerFormProvider(),
      row.sellerName.getOrElse("")
    )

    errors.map { e =>
      support.error(row.rowNumber, "sellerName", messages(e.message))
    }
  }

  private def validateTaxRate(
                               row: ParsedStcRow
                             )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.taxRate match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "taxRate",
            messages("fileUpload.error.taxRate.invalid")
          )
        )

      case Some(_) =>
        Seq.empty
    }
  }

  private def validateChargingPoint(
                                     row: ParsedStcRow,
                                     affinityKey:String
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.chargingPoint match {

      case ParsedValue.Missing =>
        Seq(
          support.error(
            row.rowNumber,
            "chargingPoint",
            messages(s"fileUpload.$affinityKey.chargingPoint.error.required.all")
          )
        )

      case ParsedValue.Invalid(_, _) =>
        Seq(
          support.error(
            row.rowNumber,
            "chargingPoint",
            messages(s"fileUpload.$affinityKey.chargingPoint.error.invalid")
          )
        )

      case ParsedValue.Valid(date) if date.isAfter(support.dateToday) =>
        Seq(
          support.error(
            row.rowNumber,
            "chargingPoint",
            messages(s"fileUpload.$affinityKey.chargingPoint.error.futureDate")
          )
        )

      case ParsedValue.Valid(_) =>
        Seq.empty
    }
  }

  private def validateWhatTypeOfSecurities(row: ParsedStcRow, affinityKey: String)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    row.whatTypeOfSecurities.map(_.trim) match {

      case None | Some("") =>
        Seq(
          support.error(
            row.rowNumber,
            "whatTypeOfSecurities",
            messages(s"$affinityKey.fileUpload.error.whatTypeOfSecurities.required")
          )
        )

      case _ => Seq.empty
    }

  private def validateSecuritiesQuantity(
                                          row: ParsedStcRow,
                                          affinityKey: String
                                        )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.securitiesQuantity match {

      case None | Some("") =>
        Seq(
          support.error(
            row.rowNumber,
            "securitiesQuantity",
            messages(s"$affinityKey.fileUpload.error.securitiesQuantity.required")
          )
        )

      case Some(stringValue) =>
        Try(BigDecimal(stringValue.replace(",", ""))) match {

          case Failure(_) =>
            Seq(
              support.error(
                row.rowNumber,
                "securitiesQuantity",
                messages(s"$affinityKey.detailsOfThisTransfer.error.numberOfShares.nonNumeric")
              )
            )

          case Success(value) if !value.isWhole =>
            Seq(
              support.error(
                row.rowNumber,
                "securitiesQuantity",
                messages("detailsOfThisTransfer.error.numberOfShares.wholeNumber")
              )
            )

          case Success(value) if value > support.securitiesQuantityMax =>
            Seq(
              support.error(
                row.rowNumber,
                "securitiesQuantity",
                messages(s"$affinityKey.fileUpload.error.securitiesQuantity.maximum")
              )
            )

          case Success(value) if value < support.securitiesQuantityMin =>
            Seq(
              support.error(
                row.rowNumber,
                "securitiesQuantity",
                messages(s"$affinityKey.fileUpload.error.securitiesQuantity.minimum")
              )
            )

          case Success(_) =>
            Seq.empty
        }
    }
  }

  private def validateAmountPaidForSecurities(
                                               row: ParsedStcRow,
                                               affinityKey: String
                                             )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    val form = amountPaidForSecuritiesFormProvider(affinityKey).bind(
      Map("value" -> row.amountPaidForSecurities.getOrElse(""))
    )

    form.errors.map { e =>
      support.error(row.rowNumber, "amountPaidForSecurities", messages(e.message))
    }
  }

  private def validateSellerAddressInUk(
                                         row: ParsedStcRow
                                       )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sellerAddressInUK match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerAddressInUK",
            messages("fileUpload.error.sellerAddressInUK.invalid")
          )
        )
    }
  }

  private def validateConnectedPersons(
                                        row: ParsedStcRow,
                                        affinityKey: String
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.connectedPersons match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "connectedPersons",
            messages(s"$affinityKey.fileUpload.error.connectedPersons.invalid")
          )
        )
    }
  }

  private def validateApplyingForRelief(
                                         row: ParsedStcRow,
                                         affinityKey: String
                                       )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.applyingForRelief match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "applyingForRelief",
            messages(s"$affinityKey.fileUpload.error.applyingForRelief.invalid")
          )
        )
    }
  }

  private def validateSecuritiesTarget(
                                        row: ParsedStcRow,
                                        affinityKey: String
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    val boundForm = securitiesTargetFormProvider(affinityKey = affinityKey).bind(
      Map(
        "businessName" -> row.securitiesTarget.getOrElse(""),
        "crn" -> row.companyRegistrationNumber.getOrElse("")
      )
    )

    boundForm.errors.map { formError =>
      val fieldName =
        if (formError.key.contains("crn"))
          "companyRegistrationNumber"
        else
          "securitiesTarget"

      support.error(row.rowNumber, fieldName, messages(formError.message))
    }
  }

  private def validateMaxSharePrice(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.maxSharePrice match {

      case None | Some("") =>
        Seq.empty

      case Some(stringValue) =>
        Try(BigDecimal(stringValue.replace(",", ""))) match {

          case Failure(_) =>
            Seq(
              support.error(
                row.rowNumber,
                "maxSharePrice",
                messages("maxSharePrice.error.invalid")
              )
            )

          case Success(v) if v.scale > 2 =>
            Seq(
              support.error(
                row.rowNumber,
                "maxSharePrice",
                messages("maxSharePrice.error.invalid")
              )
            )

          case Success(v) if v > support.maxCurrency =>
            Seq(
              support.error(
                row.rowNumber,
                "maxSharePrice",
                messages("maxSharePrice.error.maximum")
              )
            )

          case Success(v) if v < support.minCurrency =>
            Seq(
              support.error(
                row.rowNumber,
                "maxSharePrice",
                messages("maxSharePrice.error.minimum")
              )
            )

          case Success(_) =>
            Seq.empty
        }
    }
  }

  private def validateMinSharePrice(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.minSharePrice match {

      case None | Some("") =>
        Seq.empty

      case Some(stringValue) =>
        Try(BigDecimal(stringValue.replace(",", ""))) match {

          case Failure(_) =>
            Seq(
              support.error(
                row.rowNumber,
                "minSharePrice",
                messages("minSharePrice.error.invalid")
              )
            )

          case Success(v) if v.scale > 2 =>
            Seq(
              support.error(
                row.rowNumber,
                "minSharePrice",
                messages("minSharePrice.error.invalid")
              )
            )

          case Success(v) if v > support.maxCurrency =>
            Seq(
              support.error(
                row.rowNumber,
                "minSharePrice",
                messages("minSharePrice.error.maximum")
              )
            )

          case Success(v) if v < support.minCurrency =>
            Seq(
              support.error(
                row.rowNumber,
                "minSharePrice",
                messages("minSharePrice.error.minimum")
              )
            )

          case Success(_) =>
            Seq.empty
        }
    }
  }

  private def validateSharePurchaseReason(
                                           row: ParsedStcRow
                                         )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sharePurchaseReason match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sharePurchaseReason",
            messages("sharePurchaseReason.required")
          )
        )
      case Some(value) => value.trim.toLowerCase match {
        case "cancellation" | "treasury" =>
          Seq.empty
        case _ =>
          Seq(
            support.error(
              row.rowNumber,
              "sharePurchaseReason",
              messages("sharePurchaseReason.invalid")
            )
          )
      }
    }
  }

  private def validatePurchasedForCancellation(
                                                row: ParsedStcRow
                                              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.purchaseForCancellation match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "purchasedForCancellation",
            messages("purchasedForCancellation.invalid")
          )
        )
    }
  }

  private def validateNameOfBuyer(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    val errors = support.bindSingleValue(
      nameOfBuyerFormProvider(),
      row.buyerName.getOrElse("")
    )

    errors.map { e =>
      support.error(row.rowNumber, "buyerName", messages(e.message))
    }
  }


  private def validateBuyerAddressInUk(
                                        row: ParsedStcRow
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.buyerAddressInUK match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(row.rowNumber, "buyerAddressInUK", messages("fileUpload.error.buyerAddressInUK.invalid"))
        )
    }
  }

  private def validateTypeOfShares(row: ParsedStcRow, affinityKey: String)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    row.typeOfShares match {

      case None  =>
        Seq(
          support.error(
            row.rowNumber,
            "typeOfShares",
            messages(s"$affinityKey.fileUpload.error.typeOfSecurities.required")
          )
        )

      case _ => Seq.empty
    }
}