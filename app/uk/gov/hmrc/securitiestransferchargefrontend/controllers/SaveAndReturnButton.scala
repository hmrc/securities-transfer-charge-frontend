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

package uk.gov.hmrc.securitiestransferchargefrontend.controllers
import play.api.mvc.AnyContent
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.{StcDataRequest, StcOptionalDataRequest}

object SaveAndReturnButton:
  val saveAndReturnButtonName = "save-and-return-button"

  private val checkReturn: Map[String, Seq[String]] => Boolean = _.contains(saveAndReturnButtonName)

  def isReturn(request: StcDataRequest[AnyContent]): Boolean =
    request.body.asFormUrlEncoded.exists(checkReturn)

  def isReturn(request: StcOptionalDataRequest[AnyContent]): Boolean =
    request.body.asFormUrlEncoded.exists(checkReturn)
