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
import org.scalatest.exceptions.TestFailedException

import scala.io.Source

class MessagesApostropheSpec extends SpecBase {

  private def checkApostrophes(lines: List[String], fileName: String): Unit = {
    val failures = lines.zipWithIndex.collect {
      case (line, index) if line.contains('\'') =>
        s"$fileName:${index + 1} -> $line"
    }

    val message =
      s"""
         |ASCII apostrophes (') found in $fileName.
         |${failures.mkString("\n")}
         |""".stripMargin

    assert(failures.isEmpty, message)
  }

  private def readFileLines(path: String): List[String] = {
    val source = Source.fromFile(path, "UTF-8")
    try source.getLines().toList
    finally source.close()
  }

  "Messages apostrophes check" - {
    "should check the messages.en file" in {
      val fileName = "messages.en"
      readFileLines("conf/messages.en")
      checkApostrophes(readFileLines("conf/messages.en"),fileName)
    }

    "should fail when ASCII apostrophes are present" in {
      val fileName = "messages.en"
      val thrown = intercept[TestFailedException] {
        checkApostrophes(readFileLines("test/resources/messages.en"), fileName)
      }
      thrown.getMessage must include("This message contains incorrect 'apostrophe'")
    }
  }
}