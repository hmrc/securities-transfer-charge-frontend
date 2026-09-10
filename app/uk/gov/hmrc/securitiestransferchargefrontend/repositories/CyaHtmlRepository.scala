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

package uk.gov.hmrc.securitiestransferchargefrontend.repositories

import play.twirl.api.HtmlFormat
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId

import javax.inject.Inject
import scala.concurrent.Future

trait CyaHtmlRepository:
  def store(key: SubmissionId, value: HtmlFormat.Appendable): Future[Unit]
  def retrieve(key: SubmissionId): Future[HtmlFormat.Appendable]

// TODO: This needs to be implemented as part of the first NRS ticket.  
final class CyaHtmlRepositoryImpl @Inject() extends CyaHtmlRepository:

  def store(key: SubmissionId, value: HtmlFormat.Appendable): Future[Unit] = 
    Future.successful(())

  def retrieve(key: SubmissionId): Future[HtmlFormat.Appendable] =
    Future.successful(
      HtmlFormat.empty
    )
  