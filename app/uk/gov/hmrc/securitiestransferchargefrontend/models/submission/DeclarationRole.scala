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

package uk.gov.hmrc.securitiestransferchargefrontend.models.submission

import play.api.libs.json.*

enum DeclarationRole(val code: String):
  case Director extends DeclarationRole("1")
  case Secretary extends DeclarationRole("2")
  case PersonAuthorised extends DeclarationRole("3")
  case Administrator extends DeclarationRole("4")
  case Receiver extends DeclarationRole("5")
  case ReceiverManager extends DeclarationRole("6")
  case CicManager extends DeclarationRole("7")
  case UkSocietas extends DeclarationRole("8")

object DeclarationRole:
  
  val fromString: String => Option[DeclarationRole] = s => DeclarationRole.values.find(_.toString.equalsIgnoreCase(s))
  
  given Format[DeclarationRole] = Format(
    Reads {
      case JsString(s) =>
        fromString(s)
          .map(JsSuccess(_))
          .getOrElse(JsError(s"Invalid DeclarationRole value [$s]. Expected one of: 1..8"))
      case other => JsError(s"Expected a JSON string for DeclarationRole, got: $other")
    },
    Writes(role => JsString(role.code))
  )
