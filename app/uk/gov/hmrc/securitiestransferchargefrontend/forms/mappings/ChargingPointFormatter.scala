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

package uk.gov.hmrc.securitiestransferchargefrontend.forms.mappings

import play.api.data.FormError
import play.api.i18n.Messages
import uk.gov.hmrc.securitiestransferchargefrontend.models.DateHelper

import java.time.LocalDate
import scala.util.{Failure, Success, Try}


private[mappings] class ChargingPointFormatter (
                                            invalidKey: String,
                                            allRequiredKey: String,
                                            twoRequiredKey: String,
                                            requiredKey: String,
                                            futureDateKey: String,
                                            args: Seq[String] = Seq.empty
                                          )(implicit messages: Messages) extends LocalDateFormatter (invalidKey, allRequiredKey, twoRequiredKey, requiredKey, args) {

  private def isDateInTheFuture(localDate: LocalDate): Boolean = {
    val today = DateHelper.today
    localDate.isAfter(today)
  }

  override protected def toDate(key: String, day: Int, month: Int, year: Int): Either[Seq[FormError], LocalDate] =
    Try(LocalDate.of(year, month, day)) match {
      case Success(date) =>
        if (isDateInTheFuture(date)) Left(Seq(FormError(key, futureDateKey)))
        else Right(date)
      case Failure(_) =>
        Left(Seq(FormError(key, invalidKey, getErrorArgs(day, month))))
    }

  protected def getErrorArgs(day: Int, month: Int): Seq[String] = {
    val isDayError   = day < 1 || day > 31
    val isMonthError = month < 1 || month > 12

    (isDayError, isMonthError) match {
      case (true, false) => Seq("day")
      case (false, true) => Seq("month")
      case (_, _)        => Seq("day", "month", "year")
    }
  }
}