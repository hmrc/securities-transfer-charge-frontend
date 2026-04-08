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

package uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.fileupload

object SpreadsheetColumnLetters {

  def fromZeroBasedIndex(index: Int): String = {
    require(index >= 0, s"Column index must be >= 0 but was $index")

    @annotation.tailrec
    def loop(n: Int, acc: String): String = {
      val remainder = n % 26
      val next = ('A' + remainder).toChar.toString + acc
      val quotient = n / 26 - 1

      if (quotient < 0) next
      else loop(quotient, next)
    }

    loop(index, "")
  }
}