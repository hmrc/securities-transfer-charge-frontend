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

import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.submission.RowToFailureMap

import java.time.LocalDate

type ChargeReference = String

trait SubmissionCreateResponse:
  def fold[A](
   onSuccess: SubmissionCreateResponseSuccess => A,
   onPartialFailure: SubmissionCreateResponsePartialFailure => A,
   onFailure: => A
  ): A = this match {
    case s: SubmissionCreateResponseSuccess         => onSuccess(s)
    case pf: SubmissionCreateResponsePartialFailure => onPartialFailure(pf)
    case _                                          => onFailure
  }

  def isSuccessful: Boolean = this match {
    case _: SubmissionCreateResponseSuccess => true
    case _                                  => false
  }

  def hasAcceptedTransfers: Boolean = this match {
    case _: SubmissionCreateResponseSuccess        => true
    case _: SubmissionCreateResponsePartialFailure => true
    case _                                         => false
  }

/* TODO: For bulk the utrn is just the first returned, the others are not collected.
 *  This is fine for now since bulk does not have a confirmation page, but if we
 *  change that we will have to do something else here.
 */

final case class SubmissionCreateResponseSuccess(
  submissionId: SubmissionId,
  utrn: String,
  chargeReferences: Seq[ChargeReference],
  taxDue: BigDecimal,
  paymentDueBy: LocalDate,
  agentReference: Option[String]) extends SubmissionCreateResponse

final case class SubmissionCreateResponsePartialFailure(
  submissionId: SubmissionId,
  successfulChargeReferences: Seq[ChargeReference],
  failedRecords: RowToFailureMap,
  taxDue: BigDecimal,
  paymentDueBy: LocalDate,
  agentReference: Option[String]) extends SubmissionCreateResponse

case object SubmissionCreateResponseFailure extends SubmissionCreateResponse
