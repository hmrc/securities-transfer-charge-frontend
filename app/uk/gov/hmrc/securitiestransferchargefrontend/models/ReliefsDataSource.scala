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

import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.utils.ResourceLoader

import javax.inject.Inject

class ReliefsDataSource @Inject()(
                                   resourceLoader: ResourceLoader,
                                   appConfig: FrontendAppConfig
                                 ) {

  private lazy val reliefsFileContent: String = resourceLoader.loadString(appConfig.reliefsFileLocation)

  lazy val reliefs: Seq[Relief] =
    reliefsFileContent
      .linesIterator
      .map(_.trim)
      .filter(_.nonEmpty)
      .map { line =>
        line.split(",", 2) match {
          case Array(name, rateStr) =>
            Relief(name.trim, rateStr.trim.toInt)
          case _ =>
            throw new IllegalArgumentException(s"Invalid relief line: $line")
        }
      }
      .toSeq


  def getRate(reliefName: String): Int =
    reliefs
      .find(_.name == reliefName)
      .map(_.rate)
      .getOrElse(throw new NoSuchElementException(s"No rate found for relief: $reliefName"))
}