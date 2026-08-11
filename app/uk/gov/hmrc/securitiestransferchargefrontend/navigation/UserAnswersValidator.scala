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
import uk.gov.hmrc.securitiestransferchargefrontend.pages.{CyaPage, ErrorPage, JourneyRecoveryPage, Page}
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable

import scala.concurrent.{ExecutionContext, Future}

abstract class UserAnswersValidator(navigator: Navigator)(implicit ec: ExecutionContext) {

  protected type GettablePage[A] = Page & Gettable[A]
  private val recoveryPage: GettablePage[?] = JourneyRecoveryPage

  /*
   * Uses the navigator to walk the page graph and check if all the data is present in the UserAnswers.
   * Returns:
    * - Left(Call) if a page is missing data, where the Call is the page that should be displayed to the user to fill in the missing data.
    * - Right(true) if all data is present and the user can continue to the next page.
    * - Right(false) if an error page is found.
   */
  final def validate[A](userAnswers: UserAnswers)(implicit request: Request[?]): Future[Either[Call, Boolean]] = {
    validateRec(userAnswers, startPage)
  }

  private def validateRec(userAnswers: UserAnswers, page: GettablePage[?])(implicit request: Request[?]): Future[Either[Call, Boolean]] = {
    if (isCyaPage(page))
      Future.successful(Right(true))
    else if (isErrorPage(page))
      Future.successful(Right(false))
    else if (!pageHasValidDataAtPath(userAnswers, page))
      Future.successful(Left(callForPage(page)))
    else {
      navigator.nextPage(page, NormalMode, userAnswers).flatMap { nextCall =>
        val nextPage: GettablePage[?] = callToPage(nextCall)
        validateRec(userAnswers, nextPage)
      }
    }
  }

  protected val startPage: GettablePage[?]
  protected val pageCallMap: PageCallBiMap

  // Default implementation of pageHasValidDataAtPath, can be overridden in subclasses
  // where additional validation logic is required for specific pages.
  protected def pageHasValidDataAtPath(userAnswers: UserAnswers, page: GettablePage[?]): Boolean =
    pageHasDataAtPath(userAnswers, page)

  private def callToPage(call: Call): GettablePage[?] =
    pageCallMap.getPageFor(call).getOrElse(recoveryPage)

  private def callForPage(page: GettablePage[?]): Call =
    pageCallMap.getCallFor(page).getOrElse(navigator.errorPage(page))

  private def isCyaPage(page: GettablePage[?]): Boolean =
    page.isInstanceOf[CyaPage]

  private def isErrorPage(page: GettablePage[?]): Boolean =
    page.isInstanceOf[ErrorPage]

  private def pageHasDataAtPath(userAnswers: UserAnswers, page: GettablePage[?]): Boolean = {
    (userAnswers.data \ page.toString).toOption.isDefined
  }

}
