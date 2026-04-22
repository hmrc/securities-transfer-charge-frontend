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
import uk.gov.hmrc.securitiestransferchargefrontend.forms.stf.individuals._
import uk.gov.hmrc.securitiestransferchargefrontend.models.fileupload.{ParsedRow, ParsedStcRow, ParsedValue, StcRowValidationError}

import javax.inject.{Inject, Singleton}

@Singleton
class StcBasicRowValidator @Inject()(
                                      support: StcValidationSupport,
                                      messagesApi: MessagesApi,   
                                      applyingForReliefFormProvider: ApplyingForReliefFormProvider,
                                      chargingPointFormProvider: ChargingPointFormProvider,
                                      connectedPersonsFormProvider: ConnectedPersonsFormProvider,
                                      nameOfSellerFormProvider: NameOfSellerFormProvider,
                                      securitiesTargetFormProvider: SecuritiesTargetFormProvider
                                    ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

  private val amountMaximum = BigDecimal(999999999)

  def validate(rawRow: ParsedRow, parsedRow: ParsedStcRow): Seq[StcRowValidationError] =
    validateNameOfSeller(rawRow) ++
      validateSellerAddressInUk(rawRow) ++
      validateConnectedPersons(rawRow) ++
      validateApplyingForRelief(rawRow) ++
      validateSecuritiesTarget(rawRow) ++
      validateChargingPoint(rawRow) ++
      validateTaxRate(rawRow) ++
      validateWhatTypeOfSecurities(rawRow) ++
      validateSecuritiesQuantity(parsedRow) ++
      validateAmountPaidForSecurities(rawRow, parsedRow)

  private def validateNameOfSeller(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val errors = support.bindSingleValue(
      nameOfSellerFormProvider(),
      rawRow.valueAt(StcUploadColumn.sellerName).getOrElse("")
    )

    errors.map { formError =>
      val message =
        formError.message match {
          case "nameOfSeller.error.required" => messages("nameOfSeller.error.required")
          case "nameOfSeller.error.length"   => messages("nameOfSeller.error.length")
          case _                             => messages("nameOfSeller.error.required")
        }

      support.error(rawRow.rowNumber, "sellerName", message)
    }
  }

  private def validateSellerAddressInUk(rawRow: ParsedRow): Seq[StcRowValidationError] =
    support.validateBooleanField(
      rawRow,
      StcUploadColumn.sellerAddressInUK,
      "sellerAddressInUk",
      connectedPersonsFormProvider(),
      requiredMessage = messages("fileUpload.error.sellerAddressInUk.invalid"),
      invalidMessage = messages("fileUpload.error.sellerAddressInUk.invalid")
    )

  private def validateConnectedPersons(rawRow: ParsedRow): Seq[StcRowValidationError] =
    support.validateBooleanField(
      rawRow,
      StcUploadColumn.connectedPersons,
      "connectedPersons",
      connectedPersonsFormProvider(),
      requiredMessage = messages("fileUpload.error.connectedPersons.invalid"),
      invalidMessage = messages("fileUpload.error.connectedPersons.invalid")
    )

  private def validateApplyingForRelief(rawRow: ParsedRow): Seq[StcRowValidationError] =
    support.validateBooleanField(
      rawRow,
      StcUploadColumn.applyingForRelief,
      "applyingForRelief",
      applyingForReliefFormProvider(),
      requiredMessage = messages("fileUpload.error.applyingForRelief.invalid"),
      invalidMessage = messages("fileUpload.error.applyingForRelief.invalid")
    )

  private def validateSecuritiesTarget(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val boundForm = securitiesTargetFormProvider().bind(
      Map(
        "businessName" -> rawRow.valueAt(StcUploadColumn.securitiesTarget).getOrElse(""),
        "crn"          -> rawRow.valueAt(StcUploadColumn.whatIsCRN).getOrElse("")
      )
    )

    boundForm.errors.map { formError =>
      val fieldName =
        if (formError.key.contains("crn")) "companyRegistrationNumber"
        else "securitiesTarget"

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

      support.error(rawRow.rowNumber, fieldName, message)
    }
  }

  private def validateChargingPoint(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val raw = rawRow.valueAt(StcUploadColumn.chargingPoint).getOrElse("").trim

    val containsForbiddenChars =
      raw.nonEmpty && !raw.matches("""^[A-Za-z0-9/\-\s]+$""")

    if (containsForbiddenChars) {
      Seq(
        support.error(
          rawRow.rowNumber,
          "chargingPoint",
          messages("fileUpload.error.chargingPoint.invalidCharacters")
        )
      )
    } else {
      val parts = raw.split("""[/\-\s]""").toList.filter(_.nonEmpty)

      val dateMap =
        parts match {
          case year :: month :: day :: Nil if year.length == 4 =>
            Map(
              "value.day"   -> day,
              "value.month" -> month,
              "value.year"  -> year
            )

          case day :: month :: year :: Nil =>
            Map(
              "value.day"   -> day,
              "value.month" -> month,
              "value.year"  -> year
            )

          case _ =>
            Map(
              "value.day"   -> "",
              "value.month" -> "",
              "value.year"  -> ""
            )
        }

      val boundForm = chargingPointFormProvider().bind(dateMap)

      boundForm.errors.map { formError =>
        val message =
          formError.message match {
            case "chargingPoint.error.required.all" =>
              messages("chargingPoint.error.required.all")

            case "chargingPoint.error.futureDate" =>
              messages("chargingPoint.error.futureDate")

            case "chargingPoint.error.invalid" =>
              messages("chargingPoint.error.invalid")

            case "chargingPoint.error.required" =>
              messages("chargingPoint.error.required", formError.args: _*)

            case "chargingPoint.error.required.two" =>
              messages("chargingPoint.error.required.two", formError.args: _*)

            case _ =>
              messages("chargingPoint.error.invalid")
          }

        support.error(rawRow.rowNumber, "chargingPoint", message)
      }
    }
  }

  private def validateTaxRate(rawRow: ParsedRow): Seq[StcRowValidationError] =
    rawRow.valueAt(StcUploadColumn.taxRate).map(_.trim) match {
      case None | Some("") =>
        Seq(support.error(rawRow.rowNumber, "taxRate", messages("fileUpload.error.taxRate.invalid")))

      case Some(rawValue) if StcTaxRateParser.parse(rawValue).isEmpty =>
        Seq(support.error(rawRow.rowNumber, "taxRate", messages("fileUpload.error.taxRate.invalid")))

      case _ =>
        Seq.empty
    }

  private def validateWhatTypeOfSecurities(rawRow: ParsedRow): Seq[StcRowValidationError] =
    rawRow.valueAt(StcUploadColumn.whatTypeOfSecurities).map(_.trim) match {
      case None | Some("") =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "whatTypeOfSecurities",
            messages("fileUpload.error.whatTypeOfSecurities.required")
          )
        )

      case _ =>
        Seq.empty
    }

  private def validateSecuritiesQuantity(parsedRow: ParsedStcRow): Seq[StcRowValidationError] =
    parsedRow.securitiesQuantity match {
      case ParsedValue.Missing =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.required")
          )
        )

      case ParsedValue.Invalid(_, _) =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.nonNumeric")
          )
        )

      case ParsedValue.Valid(value) if value < support.securitiesQuantityMin =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.minimum")
          )
        )

      case ParsedValue.Valid(value) if value >= support.securitiesQuantityMax =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            messages("fileUpload.error.securitiesQuantity.maximum")
          )
        )

      case _ =>
        Seq.empty
    }

  private def validateAmountPaidForSecurities(
                                               rawRow: ParsedRow,
                                               parsedRow: ParsedStcRow
                                             ): Seq[StcRowValidationError] =
    parsedRow.amountPaidForSecurities match {
      case ParsedValue.Missing =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "amountPaidForSecurities",
            messages("amountPaidForSecurities.error.required")
          )
        )

      case ParsedValue.Invalid(_, _) =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "amountPaidForSecurities",
            messages("fileUpload.error.amountPaidForSecurities.nonNumeric")
          )
        )

      case ParsedValue.Valid(value) if value > amountMaximum =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "amountPaidForSecurities",
            messages("fileUpload.error.amountPaidForSecurities.maximum")
          )
        )

      case _ =>
        Seq.empty
    }
}