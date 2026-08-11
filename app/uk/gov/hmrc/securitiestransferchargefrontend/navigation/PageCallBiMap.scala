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

import play.api.mvc.Call
import uk.gov.hmrc.securitiestransferchargefrontend.models.{CheckMode, Mode, NormalMode}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.queries.Gettable

import scala.collection.mutable

type GettablePage[A] = Page & Gettable[A]
type CallCreator = Mode => Call

trait PageCallBiMap:
  def getCallFor(page: GettablePage[?]): Option[Call]
  def getPageFor[A](call: Call): Option[GettablePage[?]]

final class PageCallBiMapImpl(pageCallMap: collection.Map[GettablePage[?], Call], callPageMap: collection.Map[Call, GettablePage[?]]) extends PageCallBiMap {
  override def getCallFor(page: GettablePage[?]): Option[Call] = pageCallMap.get(page)
  override def getPageFor[A](call: Call): Option[GettablePage[?]] = callPageMap.get(call)
}

final class PageCallBiMapBuilder {
  private val pageCallMap: mutable.Map[GettablePage[?], Call] = mutable.Map.empty
  private val callPageMap: mutable.Map[Call, GettablePage[?]] = mutable.Map.empty

  def addMapping(page: GettablePage[?], callCreator: CallCreator): PageCallBiMapBuilder = {
    pageCallMap += page -> callCreator(CheckMode)
    callPageMap += callCreator(NormalMode) -> page
    this
  }
  
  def addMappingNoCheck(page: GettablePage[?], callCreator: () => Call): PageCallBiMapBuilder = {
    callPageMap += callCreator() -> page
    this
  }

  def build: PageCallBiMap = new PageCallBiMapImpl(pageCallMap.toMap, callPageMap.toMap)
}

/*
 * Usage example:
 *
 * val pageCallBiMapping =
 *   PageCallBiMapBuilder()
 *     .addMapping(AmountPaidForSecuritiesPage, orgSingleRoutes.AmountPaidForSecuritiesController.onPageLoad)
 *     .addMapping(OtherSecuritiesTypePage, orgSingleRoutes.OtherSecuritiesTypeController.onPageLoad)
 *     .build
*/
