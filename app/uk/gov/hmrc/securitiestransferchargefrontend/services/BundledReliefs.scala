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

package uk.gov.hmrc.securitiestransferchargefrontend.services

import play.api.Logging
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.models.Relief
import uk.gov.hmrc.securitiestransferchargefrontend.utils.ResourceLoader

import javax.inject.{Inject, Singleton}
import scala.util.control.NonFatal

/** The reliefs bundled with the service, in `conf/reliefs.txt` (one "name, percentage" per line).
  *
  * These are only a last resort, used by [[ReliefsService]] when nothing is cached and the submissions service cannot
  * supply the reliefs, for example before ETMP publish their reliefs API. They are not cached, so the submissions
  * service is tried again on the next request. Keep them in step with the reliefs ETMP holds.
  */
trait BundledReliefs:
  def reliefs: Seq[Relief]

@Singleton
class BundledReliefsImpl @Inject()(
  resourceLoader: ResourceLoader,
  appConfig: FrontendAppConfig
) extends BundledReliefs with Logging:

  // Read lazily, once. A broken file is logged and gives no reliefs, rather than stopping the service from starting.
  override lazy val reliefs: Seq[Relief] =
    try BundledReliefs.parse(resourceLoader.loadString(appConfig.bundledReliefsFile))
    catch
      case NonFatal(e) =>
        logger.warn(s"Could not read the bundled reliefs from ${appConfig.bundledReliefsFile}", e)
        Seq.empty

object BundledReliefs extends Logging:

  /** Each non-blank line is "name, percentage". Lines that cannot be read are skipped with a WARN. */
  def parse(content: String): Seq[Relief] =
    content.linesIterator.map(_.trim).filter(_.nonEmpty).flatMap { line =>
      line.split(",", 2) match
        case Array(name, rate) if name.trim.nonEmpty && rate.trim.toIntOption.isDefined =>
          Some(Relief(name.trim, rate.trim.toInt))
        case _ =>
          logger.warn(s"Skipping bundled relief line that is not 'name, percentage': $line")
          None
    }.toSeq
