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

import play.api.data.{Form, FormError}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedRow, StcRowValidationError}

import java.time.LocalDate
import javax.inject.Singleton
import scala.util.matching.Regex

@Singleton
class StcValidationSupport {

  val addressLineMaxLength = 100
  val optAddressLineMaxLength = 40
  val postcodeMaxLength    = 10
  val countryMaxLength     = 100

  val addressPattern: Regex =
    """^[A-Za-z0-9\s\.,\-’'…]+$""".r

  val countryPattern: Regex =
    """^[A-Za-z0-9\s\.,\-’'…]+$""".r

  val securitiesQuantityMin: BigDecimal = BigDecimal(1)
  val securitiesQuantityMax: BigDecimal = BigDecimal(999999999)

  private val ukPostcodePattern: Regex =
    """(?i)^[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}$""".r

  val typeOfShareMaxLength = 255
  val maxCurrency: BigDecimal = BigDecimal(999999999)
  val minCurrency: BigDecimal = BigDecimal(0.01)
  val dateToday: LocalDate = LocalDate.now()

  
  def bindSingleValue[A](form: Form[A], rawValue: String): Seq[FormError] =
    form.bind(Map("value" -> rawValue)).errors

  def error(rowNumber: Int, fieldName: String, message: String)(implicit columnIndexBuilder: ColumnIndexBuilder): StcRowValidationError =
    StcRowValidationError(
      rowNumber = rowNumber,
      fieldName = fieldName,
      columnIndex = columnIndexBuilder.find(fieldName).getOrElse(-1),
      message = message,
      blocking = true
    )

  def validateRequiredText(
                            value: Option[String],
                            rowNumber: Int,
                            fieldName: String,
                            requiredMessage: String,
                            maxLength: Option[Int] = None,
                            lengthMessage: Option[String] = None,
                            pattern: Option[Regex] = None,
                            invalidMessage: Option[String] = None
                          )(implicit columnIndexBuilder: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    value.map(_.trim) match {

      case None | Some("") =>
        Seq(error(rowNumber, fieldName, requiredMessage))

      case Some(v) =>
        validateTextValue(
          rowNumber = rowNumber,
          fieldName = fieldName,
          value = v,
          maxLength = maxLength,
          lengthMessage = lengthMessage,
          pattern = pattern,
          invalidMessage = invalidMessage
        )
    }
  }

  def validateOptionalText(
                            value: Option[String],
                            rowNumber: Int,
                            fieldName: String,
                            maxLength: Option[Int] = None,
                            lengthMessage: Option[String] = None,
                            pattern: Option[Regex] = None,
                            invalidMessage: Option[String] = None
                          )(implicit columnIndexBuilder: ColumnIndexBuilder): Seq[StcRowValidationError] = {

    value.map(_.trim) match {

      case None | Some("") =>
        Seq.empty

      case Some(v) =>
        validateTextValue(
          rowNumber,
          fieldName,
          v,
          maxLength,
          lengthMessage,
          pattern,
          invalidMessage
        )
    }
  }

  def validateBooleanField(
                            rawRow: ParsedRow,
                            columnIndex: Int,
                            fieldName: String,
                            form: Form[Boolean],
                            requiredMessage: String,
                            invalidMessage: String
                          )(implicit columnIndexBuilder: ColumnIndexBuilder): Seq[StcRowValidationError] = {
    val errors = bindSingleValue(form, rawRow.valueAt(columnIndex).getOrElse(""))

    errors.map { formError =>
      val message =
        formError.message match {
          case "connectedPersons.error.required" |
               "applyingForRelief.error.required" |
               "error.required" =>
            requiredMessage

          case _ =>
            invalidMessage
        }

      error(rawRow.rowNumber, fieldName, message)
    }
  }

  def looksLikeUkPostcode(value: String): Boolean =
    ukPostcodePattern.pattern.matcher(value.trim).matches()

  def looksLikeCountry(value: String): Boolean =
    countryPattern.pattern.matcher(value.trim).matches()  

  private def validateTextValue(
                                 rowNumber: Int,
                                 fieldName: String,
                                 value: String,
                                 maxLength: Option[Int],
                                 lengthMessage: Option[String],
                                 pattern: Option[Regex],
                                 invalidMessage: Option[String]
                               )(implicit columnIndexBuilder: ColumnIndexBuilder): Seq[StcRowValidationError] = {
    val lengthErrors =
      (maxLength, lengthMessage) match {
        case (Some(max), Some(message)) if value.length > max =>
          Seq(error(rowNumber, fieldName, message))
        case _ =>
          Seq.empty
      }

    val patternErrors =
      (pattern, invalidMessage) match {
        case (Some(regex), Some(message)) if regex.findFirstMatchIn(value).isEmpty =>
          Seq(error(rowNumber, fieldName, message))
        case _ =>
          Seq.empty
      }

    lengthErrors ++ patternErrors
  }
}
