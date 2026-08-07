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

package uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single

import play.api.libs.json.JsPath
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.pages.QuestionPage
import uk.gov.hmrc.securitiestransferchargefrontend.pages.sh03.WhatReliefAreYouApplyingForPage

import scala.util.{Success, Try}

case object ApplyingForReliefPage extends QuestionPage[Boolean]:

  override def path: JsPath = JsPath \ toString

  override def toString: String = "applyingForRelief"

  override def cleanup(applyingForRelief: Option[Boolean], userAnswers: UserAnswers): Try[UserAnswers] =
    if applyingForRelief.contains(false) then
      userAnswers.remove(WhatReliefAreYouApplyingForPage)
    else Success(userAnswers)

