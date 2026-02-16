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

package conf

import base.SpecBase
import play.api.libs.json.*

import scala.io.Source

class ALFJsonApostropheSpec extends SpecBase {

  "ALF json file" - {

    "should not contain ASCII apostrophes (')" in {
      assertNoAsciiApostrophes("/alf.json")
    }

  }

  "ALF seller json file" - {

    "should not contain ASCII apostrophes (')" in {
      assertNoAsciiApostrophes("/alf-seller.json")
    }

  }

  private def assertNoAsciiApostrophes(resourcePath: String): Unit = {

    val stream = getClass.getResourceAsStream(resourcePath)
    require(stream != null, s"Resource not found: $resourcePath")

    val jsonString =
      try Source.fromInputStream(stream, "UTF-8").mkString
      finally stream.close()

    val json: JsValue = Json.parse(jsonString)

    def collectStrings(js: JsValue): Seq[String] = js match {
      case JsString(s) => Seq(s)
      case JsArray(values) => values.toSeq.flatMap(collectStrings)
      case JsObject(fields) => fields.values.toSeq.flatMap(collectStrings)
      case _ => Seq.empty
    }

    val failures = collectStrings(json).filter(_.contains('\''))

    val msg =
      s"""
         |ASCII apostrophes (') found in $resourcePath.
         |${failures.mkString("\n")}
         |""".stripMargin

    assert(failures.isEmpty, msg)
  }

}
