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
                                      amountPaidForSecuritiesFormProvider: AmountPaidForSecuritiesFormProvider,
                                      applyingForReliefFormProvider: ApplyingForReliefFormProvider,
                                      chargingPointFormProvider: ChargingPointFormProvider,
                                      connectedPersonsFormProvider: ConnectedPersonsFormProvider,
                                      nameOfSellerFormProvider: NameOfSellerFormProvider,
                                      securitiesTargetFormProvider: SecuritiesTargetFormProvider,
                                      whatTypeOfSecuritiesFormProvider: WhatTypeOfSecuritiesFormProvider
                                    ) {

  private implicit val messages: Messages =
    messagesApi.preferred(Seq(Lang("en")))

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
      validateAmountPaidForSecurities(rawRow)

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
      requiredMessage = "Enter ‘yes’ if the seller lives in the UK",
      invalidMessage = "Enter ‘yes’ if the seller lives in the UK"
    )

  private def validateConnectedPersons(rawRow: ParsedRow): Seq[StcRowValidationError] =
    support.validateBooleanField(
      rawRow,
      StcUploadColumn.connectedPersons,
      "connectedPersons",
      connectedPersonsFormProvider(),
      requiredMessage = "Enter ‘yes’ if you and the buyer are connected persons",
      invalidMessage = "Enter ‘yes’ if you and the buyer are connected persons"
    )

  private def validateApplyingForRelief(rawRow: ParsedRow): Seq[StcRowValidationError] =
    support.validateBooleanField(
      rawRow,
      StcUploadColumn.applyingForRelief,
      "applyingForRelief",
      applyingForReliefFormProvider(),
      requiredMessage = messages("applyingForRelief.error.required"),
      invalidMessage = "Enter ‘yes’ if you are applying for a relief"
    )

  private def validateChargingPoint(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val raw = rawRow.valueAt(StcUploadColumn.chargingPoint).getOrElse("").trim

    val containsForbiddenChars =
      raw.nonEmpty && !raw.matches("""^[A-Za-z0-9/\-\s]+$""")

    if (containsForbiddenChars) {
      Seq(
        support.error(
          rawRow.rowNumber,
          "chargingPoint",
          "The date you bought the securities can only contain numbers and letters"
        )
      )
    } else {
      val parts = raw.split("""[/\-\s]""").toList.filter(_.nonEmpty)

      val dateMap =
        parts match {
          case year :: month :: day :: Nil if year.length == 4 =>
            Map(
              "value.day" -> day,
              "value.month" -> month,
              "value.year" -> year
            )

          case day :: month :: year :: Nil =>
            Map(
              "value.day" -> day,
              "value.month" -> month,
              "value.year" -> year
            )

          case _ =>
            Map(
              "value.day" -> "",
              "value.month" -> "",
              "value.year" -> ""
            )
        }

      val boundForm = chargingPointFormProvider().bind(dateMap)

      boundForm.errors.map { formError =>
        val message =
          formError.message match {
            case "chargingPoint.error.required" =>
              formError.args.headOption match {
                case Some("day") => "The date you bought the securities must include a day"
                case Some("month") => "The date you bought the securities must include a month"
                case Some("year") => "The date you bought the securities must include a year"
                case _ => messages("chargingPoint.error.required")
              }

            case "chargingPoint.error.required.all" =>
              messages("chargingPoint.error.required.all")

            case "chargingPoint.error.required.two" =>
              val args = formError.args.map(_.toString).toSet
              if (args.contains("day")) {
                "The date you bought the securities must include a day"
              } else if (args.contains("month")) {
                "The date you bought the securities must include a month"
              } else if (args.contains("year")) {
                "The date you bought the securities must include a year"
              } else {
                messages("chargingPoint.error.required.two", formError.args: _*)
              }

            case "chargingPoint.error.futureDate" =>
              messages("chargingPoint.error.futureDate")

            case "chargingPoint.error.invalid" =>
              messages("chargingPoint.error.invalid")

            case _ =>
              "The charging point date is not in the correct format. Enter eg 20 November 2025 as 20/11/2025"
          }

        support.error(rawRow.rowNumber, "chargingPoint", message)
      }
    }
  }

  private def validateTaxRate(rawRow: ParsedRow): Seq[StcRowValidationError] =
    rawRow.valueAt(StcUploadColumn.taxRate).map(_.trim) match {
      case None | Some("") =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "taxRate",
            messages("taxRate.error.required")
          )
        )

      case Some(rawValue) if StcTaxRateParser.parse(rawValue).isEmpty =>
        Seq(
          support.error(
            rawRow.rowNumber,
            "taxRate",
            messages("taxRate.error.required")
          )
        )

      case _ =>
        Seq.empty
    }

  private def validateWhatTypeOfSecurities(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val errors = support.bindSingleValue(
      whatTypeOfSecuritiesFormProvider(),
      rawRow.valueAt(StcUploadColumn.whatTypeOfSecurities).getOrElse("")
    )

    errors.map { formError =>
      val message =
        formError.message match {
          case "individual.whatTypeOfSecurities.error.required" =>
            messages("individual.whatTypeOfSecurities.error.required")
          case "error.invalid" =>
            "Enter a valid type of security"
          case _ =>
            "Enter a valid type of security"
        }

      support.error(rawRow.rowNumber, "whatTypeOfSecurities", message)
    }
  }

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
            "Company Reference Number must be 8 characters or fewer"

          case _ if fieldName == "companyRegistrationNumber" =>
            "Company Reference Number must be 8 characters or fewer"

          case _ =>
            messages("securitiesTarget.error.businessName.required")
        }

      support.error(rawRow.rowNumber, fieldName, message)
    }
  }

  private def validateSecuritiesQuantity(parsedRow: ParsedStcRow): Seq[StcRowValidationError] =
    parsedRow.securitiesQuantity match {
      case ParsedValue.Missing =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            messages("detailsOfThisTransfer.error.numberOfShares.required")
          )
        )

      case ParsedValue.Invalid(_, _) =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            "The amount of shares you are buying must be a number"
          )
        )

      case ParsedValue.Valid(value) if value < support.securitiesQuantityMin =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            "The number of shares must be at least 1"
          )
        )

      case ParsedValue.Valid(value) if value >= support.securitiesQuantityMax =>
        Seq(
          support.error(
            parsedRow.rowNumber,
            "securitiesQuantity",
            "The number of shares you are buying must be below 999,999,999"
          )
        )

      case _ =>
        Seq.empty
    }

  private def validateAmountPaidForSecurities(rawRow: ParsedRow): Seq[StcRowValidationError] = {
    val errors = support.bindSingleValue(
      amountPaidForSecuritiesFormProvider(),
      rawRow.valueAt(StcUploadColumn.amountPaidForSecurities).getOrElse("")
    )

    errors.map { formError =>
      val message =
        formError.message match {
          case "amountPaidForSecurities.error.required" =>
            messages("amountPaidForSecurities.error.required")

          case "amountPaidForSecurities.error.invalidNumeric" =>
            messages("amountPaidForSecurities.error.nonNumeric")

          case "amountPaidForSecurities.error.nonNumeric" =>
            messages("amountPaidForSecurities.error.nonNumeric")

          case "amountPaidForSecurities.error.belowMinimum" =>
            messages("amountPaidForSecurities.error.nonNumeric")

          case "amountPaidForSecurities.error.aboveMaximum" =>
            "The amount you paid for the securities must be £999,999,999 or below"

          case _ =>
            messages("amountPaidForSecurities.error.required")
        }

      support.error(rawRow.rowNumber, "amountPaidForSecurities", message)
    }
  }
}