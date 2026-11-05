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

import play.api.libs.json.Json
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.requests.StcDataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType
import uk.gov.hmrc.securitiestransferchargefrontend.domain.TransferType.{Other, SH03, STF}
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{ParsedStcRowsDocument, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.BulkConfirmationDetails
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class BulkCheckYourAnswersSubmissionService @Inject()(
                                                       sessionRepository: SessionRepository,
                                                       transactionSubmissionService: TransactionSubmissionService
                                                     )(implicit ec: ExecutionContext) {

  /** Saves the confirmation details and starts the bulk submission.
   * Returns the updated answers so the controller can navigate. */
  def submit(
              doc: ParsedStcRowsDocument,
              transferType: TransferType,
              paymentDueBy: LocalDate,
              taxDue: BigDecimal,
              agentReference: Option[String]
            )(using request: StcDataRequest[?]): Future[UserAnswers] = {
    
    val rows = doc.rows.map(row => ValidatedStcRow(row, Seq.empty))
    // using Seq.empty above as we could only get to this point if there are no validation errors

    val answers = request.userAnswers
    val details = BulkConfirmationDetails(answers.submissionId, paymentDueBy, taxDue, agentReference)
    val updatedAnswers = answers.copy(
      data = answers.data + (BulkConfirmationDetails.key -> Json.toJson(details))
    )

    for {
      _ <- sessionRepository.set(updatedAnswers)
      _  = submitFor(transferType, rows) // currently fire and forget
    } yield updatedAnswers
  }

  private def submitFor(transferType: TransferType, rows: Seq[ValidatedStcRow])(using StcDataRequest[?]): Future[Boolean] =
    transferType match {
      case STF   => transactionSubmissionService.submitBulkStf(rows)
      case SH03  => transactionSubmissionService.submitBulkSh03(rows)
      case Other => throw new NotImplementedError("Transfer type Other is not supported.")
    }
}