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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.shared.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRow, StcRowValidationError}

import javax.inject.Inject


class StcBasicRowValidator @Inject()(
                                      support: StcValidationSupport,
                                      messagesApi: MessagesApi,
                                      nameOfSellerFormProvider: NameOfSellerFormProvider,
                                      securitiesTargetFormProvider: SecuritiesTargetFormProvider
                                    ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  private val amountMaximum = BigDecimal(999999999)

  def validate(
                row: ParsedStcRow,
                template: StcTemplate
              )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    template match {

      case StcTemplate.STF =>
        validateSTF(row)

      case StcTemplate.SH03 =>
        validateSH03(row)
    }
  }

  def validateSTF(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateNameOfSeller(row) ++
      validateSellerAddressInUk(row) ++
      validateConnectedPersons(row) ++
      validateApplyingForRelief(row) ++
      validateSecuritiesTarget(row) ++
      validateChargingPoint(row) ++
      validateTaxRate(row) ++
      validateWhatTypeOfSecurities(row) ++
      validateSecuritiesQuantity(row) ++
      validateAmountPaidForSecurities(row)


  def validateSH03(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    validateWhatTypeOfSecurities(row) ++
      validateSecuritiesQuantity(row) ++
      validateAmountPaidForSecurities(row) ++
      validateChargingPoint(row) ++
      validateMaxSharePrice(row) ++
      validateMinSharePrice(row) ++
      validateSharePurchaseReason(row) ++
      validatePurchasedForCancellation(row)


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
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.chargingPoint match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "chargingPoint",
            messages("chargingPoint.error.required.all")
          )
        )

      case Some(_) =>
        Seq.empty
    }
  }

  private def validateWhatTypeOfSecurities(row: ParsedStcRow)(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] =
    row.whatTypeOfSecurities.map(_.trim) match {

      case None | Some("") =>
        Seq(
          support.error(
            row.rowNumber,
            "whatTypeOfSecurities",
            messages("fileUpload.error.whatTypeOfSecurities.required")
          )
        )

      case _ => Seq.empty
    }

  private def validateSecuritiesQuantity(
                                          row: ParsedStcRow
                                        )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.securitiesQuantity match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.required")
          )
        )

      case Some(value) if value < support.securitiesQuantityMin =>
        Seq(
          support.error(
            row.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.minimum")
          )
        )

      case Some(value) if value >= support.securitiesQuantityMax =>
        Seq(
          support.error(
            row.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.maximum")
          )
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateAmountPaidForSecurities(
                                               row: ParsedStcRow
                                             )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.amountPaidForSecurities match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "amountPaidForSecurities",
            messages("amountPaidForSecurities.error.required")
          )
        )

      case Some(v) if v > amountMaximum =>
        Seq(
          support.error(
            row.rowNumber,
            "amountPaidForSecurities",
            messages("fileUpload.error.amountPaidForSecurities.maximum")
          )
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateSellerAddressInUk(
                                         row: ParsedStcRow
                                       )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.sellerAddressInUk match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "sellerAddressInUk",
            messages("fileUpload.error.sellerAddressInUk.invalid")
          )
        )
    }
  }

  private def validateConnectedPersons(
                                        row: ParsedStcRow
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.connectedPersons match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "connectedPersons",
            messages("fileUpload.error.connectedPersons.invalid")
          )
        )
    }
  }

  private def validateApplyingForRelief(
                                         row: ParsedStcRow
                                       )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.applyingForRelief match {

      case Some(_) =>
        Seq.empty

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "applyingForRelief",
            messages("fileUpload.error.applyingForRelief.invalid")
          )
        )
    }
  }

  private def validateSecuritiesTarget(
                                        row: ParsedStcRow
                                      )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    val boundForm = securitiesTargetFormProvider().bind(
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

      val message =
        formError.message match {

          case "securitiesTarget.error.businessName.required" =>
            messages("securitiesTarget.error.businessName.required")

          case "securitiesTarget.error.businessName.length" =>
            messages("securitiesTarget.error.businessName.length")

          case "securitiesTarget.error.crn.length" =>
            messages("securitiesTarget.error.crn.length")

          case _ if fieldName == "companyRegistrationNumber" =>
            messages("securitiesTarget.error.crn.length")

          case _ =>
            messages("securitiesTarget.error.businessName.required")
        }

      support.error(row.rowNumber, fieldName, message)
    }
  }

  private def validateMaxSharePrice(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.maxSharePrice match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "maxSharePrice",
            messages("maxSharePrice.error.required")
          )
        )

      case Some(v) if v > amountMaximum =>
        Seq(
          support.error(
            row.rowNumber,
            "maxSharePrice",
            messages("maxSharePrice.error.maximum")
          )
        )

      case _ =>
        Seq.empty
    }
  }

  private def validateMinSharePrice(
                                     row: ParsedStcRow
                                   )(implicit cols: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    row.maxSharePrice match {

      case None =>
        Seq(
          support.error(
            row.rowNumber,
            "minSharePrice",
            messages("minSharePrice.error.required")
          )
        )

      case Some(v) if v > amountMaximum =>
        Seq(
          support.error(
            row.rowNumber,
            "minSharePrice",
            messages("minSharePrice.error.maximum")
          )
        )

      case _ =>
        Seq.empty
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
}