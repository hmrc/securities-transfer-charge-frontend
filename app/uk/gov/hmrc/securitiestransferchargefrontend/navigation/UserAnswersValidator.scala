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

package uk.gov.hmrc.securitiestransferchargefrontend.navigation

import play.api.mvc.{Call, Request}
import uk.gov.hmrc.securitiestransferchargefrontend.models.{NormalMode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{ErrorPage, JourneyRecoveryPage, Page}
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import cats.data.*
import cats.collections.*
import com.google.common.collect.BiMap
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import play.api.libs.json.Reads
/*
 * Used to validate that all the data required for a given page is present in the UserAnswers.
 * If data is missing, it will return the page used to collect the missing data.
 * If the data is present:
 * If the page is a CYA page, it will return true.
 * If the page is an error page, it will return false.
 */
abstract class UserAnswersValidator(navigator: Navigator)(implicit ec: ExecutionContext) {

  protected type GettablePage[A] = Page & Gettable[A]
  private val jrPage: GettablePage[?] = JourneyRecoveryPage

  final def callForMissingData[A: Reads](userAnswers: UserAnswers)(page: GettablePage[A])(implicit request: Request[A]): Future[Either[Call, Boolean]] = {
    if (isCyaPage(page))
      Future.successful(Right(true))
    else if (isErrorPage(page))
      Future.successful(Right(false))
    else if (!pageHasValidData(userAnswers, page))
      Future.successful(Left(callForPage(page)))
    else {
      navigator.nextPage(page, NormalMode, userAnswers).flatMap { nextCall =>
        val nextPage = callToPage(nextCall)
        callForMissingData(userAnswers)(nextPage)
      }
    }
  }

  protected val pageCallMap: BiMap[GettablePage[?], Call]
  private def callToPage[A](call: Call): GettablePage[A] = pageCallMap.inverse().getOrDefault(call, jrPage).asInstanceOf[GettablePage[A]]
  private def callForPage[A](page: GettablePage[A]): Call = pageCallMap.getOrDefault(page, navigator.errorPage(page))
  
  protected def isCyaPage(page: GettablePage[?]): Boolean
  protected def isErrorPage(page: GettablePage[?]): Boolean = page.isInstanceOf[uk.gov.hmrc.securitiestransferchargefrontend.pages.ErrorPage]
  
  protected def pageHasValidData[A: Reads](userAnswers: UserAnswers, page: GettablePage[A]): Boolean =
    userAnswers.exists(page)
    
 

}
