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

package uk.gov.hmrc.securitiestransferchargefrontend.models

import play.api.mvc.{JavascriptLiteral, PathBindable, QueryStringBindable}

sealed trait Mode

case object CheckMode extends Mode
case object NormalMode extends Mode

object Mode {

  implicit val jsLiteral: JavascriptLiteral[Mode] = new JavascriptLiteral[Mode] {
    override def to(value: Mode): String = value match {
      case NormalMode => "NormalMode"
      case CheckMode => "CheckMode"
    }
  }

  implicit val pathBindable: PathBindable[Mode] = new PathBindable[Mode] {
    override def bind(key: String, value: String): Either[String, Mode] = value match {
      case "NormalMode" | "normal" => Right(NormalMode)
      case "CheckMode" | "check" => Right(CheckMode)
      case _ => Left(s"Invalid mode: $value")
    }

    override def unbind(key: String, value: Mode): String = value match {
      case NormalMode => "NormalMode"
      case CheckMode => "CheckMode"
    }
  }

  implicit val queryStringBindable: QueryStringBindable[Mode] = new QueryStringBindable[Mode] {
    override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Mode]] = {
      params.get(key).flatMap(_.headOption).map {
        case "NormalMode" | "normal" => Right(NormalMode)
        case "CheckMode" | "check" => Right(CheckMode)
        case value => Left(s"Invalid mode: $value")
      }
    }

    override def unbind(key: String, value: Mode): String = {
      val modeString = value match {
        case NormalMode => "NormalMode"
        case CheckMode => "CheckMode"
      }
      s"$key=$modeString"
    }
  }
}
