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

package base.stubs

import base.Fixtures.*
import play.api.mvc.*
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.{StcAuthEnrolledAction, StcAuthorisedRequest}
import play.api.Configuration

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StubStcAuthEnrolledAction @Inject()(playBodyParsers: PlayBodyParsers, configuration: Configuration) extends StcAuthEnrolledAction {

  def this(playBodyParsers: PlayBodyParsers) =
    this(playBodyParsers, Configuration.empty)

  override def parser: BodyParser[AnyContent] = playBodyParsers.defaultBodyParser

  override protected def executionContext: ExecutionContext = ExecutionContext.global

  private val affinityGroup: AffinityGroup =
    configuration.getOptional[String]("test.affinityGroup") match {
      case Some("Organisation") => AffinityGroup.Organisation
      case _                    => AffinityGroup.Individual
    }
  override def invokeBlock[A](request: Request[A], block: StcAuthorisedRequest[A] => Future[Result]): Future[Result] =
    block(StcAuthorisedRequest(request, testInternalId, affinityGroup, testSubscriptionId))
}