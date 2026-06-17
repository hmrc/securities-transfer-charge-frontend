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

package uk.gov.hmrc.securitiestransferchargefrontend.utils

import play.api.Logger
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.ConnectedPersonsPage

import scala.concurrent.{ExecutionContext, Future}

object CommonHelpers {

  def logInfoAndFail[A, E <: Throwable](logger: Logger): E => Future[A] = e => {
    logger.info(e.getMessage)
    Future.failed(e)
  }

  implicit class FutureOptionOps[A](fo: Future[Option[A]]) {
    def getOrFail(ex: => Throwable)(implicit ec: ExecutionContext): Future[A] =
      fo.flatMap(_.fold[Future[A]](Future.failed(ex))(Future.successful))
  }

  def requireMarketValue(implicit request: StcDataRequest[_]): Boolean =
    request.userAnswers.get(ConnectedPersonsPage).contains(true)

  val linkToTemplateFor: TransferType => AffinityGroup => String =
    case TransferType.STF => {
      case AffinityGroup.Individual => "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1i.xlsx"
      case AffinityGroup.Organisation => "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1b.xlsx"
      case AffinityGroup.Agent => "/securities-transfer-charge/assets/Bulk_Securities_Transfer_Charge_template_v1a.xlsx"
    }
    case TransferType.SH03 => {
      case AffinityGroup.Individual => "TODO"
      case AffinityGroup.Organisation => "TODO"
      case AffinityGroup.Agent => "TODO"
    }
    case TransferType.Other => {
      case AffinityGroup.Individual => "TODO"
      case AffinityGroup.Organisation => "TODO"
      case AffinityGroup.Agent => "TODO"
    }

  val linkToTemplateForStf: AffinityGroup => String = linkToTemplateFor(TransferType.STF)

}
