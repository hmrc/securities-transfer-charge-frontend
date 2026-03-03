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
import base.stubs.{OrganisationStubStcAuthEnrolledAction, StubSessionRepository, StubStcAuthEnrolledAction, StubStcDataRequiredAction, StubStcDataRetrievalAction}
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
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.requests.DataRequest
import uk.gov.hmrc.securitiestransferchargefrontend.models.{Mode, UserAnswers}
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.Page
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.auth.core.AffinityGroup

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
  val userAnswersId: String = "id"
  val sessionId = "sessionId1234"
  val submissionId: SubmissionId = SubmissionId("STC-123456789")
  val userId = "internalId"
  val affinityGroupKeyInd = "individual"
  val affinityGroupKeyOrg = "orgs"

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

  def emptyUserAnswers: UserAnswers = UserAnswers.empty(userAnswersId)(submissionId)

  val testBackLinkRoute: Call = Call("GET", "/back-link")
  val testNextPage = Call("GET", "/next-page")
  val testErrorPage = Call("GET", "/error-page")

  def getNavigator: Navigator = new Navigator {
    override def nextPage(page: Page, mode: Mode, userAnswers: UserAnswers)(implicit request: Request[_]): Future[Call] = Future.successful(testNextPage)
    override def previousPage(page: Page, mode: Mode, userAnswers: UserAnswers): Call = testBackLinkRoute
    override def errorPage(forPage: Page): Call = testErrorPage
  }
  
  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("sessionId" -> sessionId)

  def fakeDataRequest(userAnswers: UserAnswers): DataRequest[AnyContent] =
    DataRequest[AnyContent](FakeRequest(), "userId", userAnswers)

  def messages(app: Application): Messages = app.injector.instanceOf[MessagesApi].preferred(FakeRequest())

  protected def applicationBuilder(userAnswers: Option[UserAnswers] = None,
                                   affinityGroup: AffinityGroup = AffinityGroup.Individual,
                                   saveAndReturnClient: SaveAndReturnClient = FakeSaveAndReturnClient(),
                                   sessionRepository: SessionRepository = StubSessionRepository()
                                   ): GuiceApplicationBuilder =
    new GuiceApplicationBuilder()
      .overrides(
        bind[IdentifierAction].to[FakeIdentifierAction],
        bind[StcAuthEnrolledAction].to(
          if (affinityGroup == AffinityGroup.Organisation)
            classOf[OrganisationStubStcAuthEnrolledAction]
          else
            classOf[StubStcAuthEnrolledAction]
        ),
        bind[StcDataRetrievalAction].to[StubStcDataRetrievalAction],
        bind[DataRequiredAction].to[DataRequiredActionImpl],
        bind[StcDataRequiredAction].toInstance(StubStcDataRequiredAction(userAnswers)),
        bind[DataRetrievalAction].toInstance(new FakeDataRetrievalAction(userAnswers)),
        bind[SaveAndReturnClient].toInstance(saveAndReturnClient),
        bind[AlfAddressConnector].to[FakeAlfConnector],
        bind[SessionRepository].toInstance(sessionRepository)
      )
}
