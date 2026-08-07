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

package base

import base.Fixtures.FakeAlfConnector
import base.stubs.{AgentStubStcAuthEnrolledAction, OrganisationStubStcAuthEnrolledAction, StubSessionRepository, StubStcAuthEnrolledAction, StubStcDataRequiredAction, StubStcDataRetrievalAction}
import clients.FakeSaveAndReturnClient
import controllers.actions.*
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{OptionValues, TryValues}
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Call, Request}
import play.api.test.FakeRequest
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.clients.SaveAndReturnClient
import uk.gov.hmrc.securitiestransferchargefrontend.clients.registration.Subscription
import uk.gov.hmrc.securitiestransferchargefrontend.connectors.AlfAddressConnector
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.domain.{GroupIdentifier, SubmissionId, UserId}
import uk.gov.hmrc.securitiestransferchargefrontend.models.requests.DataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, UserAnswersValidator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*

import java.time.LocalDate
import scala.concurrent.{ExecutionContext, Future}

trait SpecBase
  extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with IntegrationPatience {

  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val hc: HeaderCarrier = HeaderCarrier()
  val testUserId: UserId = UserId("id")
  val testGroupIdentifier: GroupIdentifier = GroupIdentifier("group-123")
  val sessionId = "sessionId1234"
  val submissionId: SubmissionId = SubmissionId("STC-123456789")
  val userId = "internalId"
  val affinityGroupKeyInd = "individual"
  val affinityGroupKeyOrg = "org"
  val affinityGroupKeyAgent = "agent"
  val individualAffinity: AffinityGroup = AffinityGroup.Individual
  val orgAffinity: AffinityGroup = AffinityGroup.Organisation
  val agentAffinity: AffinityGroup = AffinityGroup.Agent

  val subscription: Subscription = Subscription(
    subsValidTo = LocalDate.now().plusDays(5),
    contactName = "John Doe",
    addressLine1 = "1 high street",
    addressLine2 = Some("Town"),
    addressLine3 = None,
    postcode = "ZZ1 1ZZ",
    countryCode = "GB",
    telephoneNumber = "07777777777",
    emailAddress = "some@email.com"
  )

  def emptyUserAnswers: UserAnswers = UserAnswers.empty(testUserId)(testGroupIdentifier)(submissionId)

  val testBackLinkRoute: Call = Call("GET", "/back-link")
  val testNextPage = Call("GET", "/next-page")
  val testErrorPage = Call("GET", "/error-page")

  def getNavigator: Navigator = new Navigator {
    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers, isReturn: Boolean = false)(implicit request: Request[_]): Future[Call] = Future.successful(testNextPage)
    override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = testBackLinkRoute
    override def previousPage(page: Page, mode: Mode, userAnswers: Option[UserAnswers]): Call = testBackLinkRoute
    override def errorPage(forPage: Page): Call = testErrorPage
    override val userAnswersValidator: UserAnswersValidator = new base.stubs.StubUserAnswersValidator(this)
  }
  
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("sessionId" -> sessionId)

  def fakeDataRequest(userAnswers: UserAnswers): DataRequest[AnyContent] =
    DataRequest[AnyContent](FakeRequest(), "userId", userAnswers)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  private def stcAuthEnrolledActionClass(
                                          affinityGroup: AffinityGroup
                                        ): Class[_ <: StcAuthEnrolledAction] =
    affinityGroup match {
      case AffinityGroup.Agent => classOf[AgentStubStcAuthEnrolledAction]
      case AffinityGroup.Individual => classOf[StubStcAuthEnrolledAction]
      case AffinityGroup.Organisation => classOf[OrganisationStubStcAuthEnrolledAction]
    }

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None,
                                   affinityGroup: AffinityGroup = AffinityGroup.Individual,
                                   saveAndReturnClient: SaveAndReturnClient = FakeSaveAndReturnClient(),
                                   sessionRepository: SessionRepository = StubSessionRepository()
                                   ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[StcAuthEnrolledAction].to(stcAuthEnrolledActionClass(affinityGroup)),
        bind[StcDataRetrievalAction].toInstance(StubStcDataRetrievalAction(userAnswers)),
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[StcDataRequiredAction].toInstance(StubStcDataRequiredAction(userAnswers)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[SaveAndReturnClient].toInstance(saveAndReturnClient),
        bind[AlfAddressConnector].to[FakeAlfConnector],
        bind[SessionRepository].toInstance(sessionRepository)
      )

  // Helper methods for creating common UserAnswers scenarios for STF
  def buildStfUserAnswers(
    buyerAddress: Option[ConfirmableAddress] = None,
    stfBuyerAddress: Option[AlfConfirmedAddress] = None,
    sellerName: Option[String] = None,
    sellerAddress: Option[AlfConfirmedAddress] = None,
    connectedPersons: Option[Boolean] = None,
    applyingForRelief: Option[Boolean] = None,
    reliefName: Option[String] = None,
    securitiesTarget: Option[SecuritiesTarget] = None,
    chargingPoint: Option[LocalDate] = None,
    taxRate: Option[TaxRate] = None,
    purchasingShares: Option[Boolean] = None,
    detailsOfTransfer: Option[DetailsOfThisTransfer] = None,
    otherSecuritiesType: Option[String] = None,
    amountPaidForSecurities: Option[BigDecimal] = None,
    totalMarketValue: Option[BigDecimal] = None
  ): UserAnswers = {
    import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single._

    var answers = emptyUserAnswers

    buyerAddress.foreach { addr =>
      answers = answers.set(ConfirmAddressPage, addr).success.value
    }

    stfBuyerAddress.foreach { addr =>
      answers = answers.set(StfBuyersAddressPage, addr).success.value
    }

    sellerName.foreach { name =>
      answers = answers.set(NameOfSellerPage, name).success.value
    }

    sellerAddress.foreach { addr =>
      answers = answers.set(StfSellerAddressPage, addr).success.value
    }

    connectedPersons.foreach { value =>
      answers = answers.set(ConnectedPersonsPage, value).success.value
    }

    applyingForRelief.foreach { value =>
      answers = answers.set(ApplyingForReliefPage, value).success.value
    }

    reliefName.foreach { name =>
      answers = answers.set(WhatReliefAreYouApplyingForPage, name).success.value
    }

    securitiesTarget.foreach { target =>
      answers = answers.set(SecuritiesTargetPage, target).success.value
    }

    chargingPoint.foreach { date =>
      answers = answers.set(ChargingPointPage, date).success.value
    }

    taxRate.foreach { rate =>
      answers = answers.set(TaxRatePage, rate).success.value
    }

    purchasingShares.foreach { value =>
      answers = answers.set(PurchasingSharesPage, value).success.value
    }

    detailsOfTransfer.foreach { details =>
      answers = answers.set(DetailsOfThisTransferPage, details).success.value
    }

    otherSecuritiesType.foreach { secType =>
      answers = answers.set(OtherSecuritiesTypePage, secType).success.value
    }

    amountPaidForSecurities.foreach { amount =>
      answers = answers.set(AmountPaidForSecuritiesPage, amount).success.value
    }

    totalMarketValue.foreach { value =>
      answers = answers.set(TotalMarketValuePage, value).success.value
    }

    answers
  }
}
