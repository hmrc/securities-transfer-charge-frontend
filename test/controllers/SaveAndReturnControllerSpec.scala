package controllers

import base.SpecBase
import base.stubs.StubStcAuthEnrolledAction
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status.SEE_OTHER
import play.api.i18n.MessagesApi
import play.api.mvc.{AnyContentAsEmpty, Call, MessagesControllerComponents, PlayBodyParsers}
import play.api.test.FakeRequest
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.SaveAndReturnController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.StcAuthEnrolledAction
import uk.gov.hmrc.securitiestransferchargefrontend.domain.SubmissionId
import uk.gov.hmrc.securitiestransferchargefrontend.models.UserAnswers
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator

import scala.concurrent.*

class SaveAndReturnControllerSpec extends SpecBase with MockitoSugar with ScalaFutures {

  implicit val testRequest: FakeRequest[AnyContentAsEmpty.type] = fakeRequest
  val testSubmissionId = "STC900"
  val testUserAnswers: UserAnswers = UserAnswers.empty("User123")(SubmissionId("test"))
  val testCall: Call = Call("GET", "/test-page")

  val mockMessagesApi: MessagesApi = mock[MessagesApi]
  val mockControllerComponents: MessagesControllerComponents = mock[MessagesControllerComponents]
  val mockBodyParsers: PlayBodyParsers = mock[PlayBodyParsers]
  val stcAuthEnrolledAction: StcAuthEnrolledAction = StubStcAuthEnrolledAction(mockBodyParsers)

  val mockNavigator: Navigator = mock[Navigator]

  def testSetup(maybeUserAnswers: Option[UserAnswers]): SaveAndReturnController = {
    maybeUserAnswers.fold
      ( when(mockNavigator.restore(any(), any())(any())).thenReturn(Future.failed(new RuntimeException("No user answers"))) )
      ( ua => when(mockNavigator.restore(any(), any())(any())).thenReturn(Future.successful(ua)) )

    new SaveAndReturnController(mockMessagesApi, mockControllerComponents, stcAuthEnrolledAction, mockNavigator)(ec)
  }
  "SaveAndReturnController" - {

    "Fail if no user answers are available from the server" in {
      val controller = testSetup(None)
      val action = controller.restore(testSubmissionId)
      val result = action.apply(testRequest)
      whenReady(result.failed) { exception =>
        exception mustBe a[RuntimeException]
        exception.getMessage mustBe "No user answers"
      }
    }

    "Return the starting page if no next page is available in the user answers" in {
      val controller = testSetup(Some(testUserAnswers))
      val action = controller.restore(testSubmissionId)
      val result = action.apply(testRequest)
      whenReady(result) { res =>
        res.header.status mustBe SEE_OTHER
        res.header.headers("Location") mustEqual Navigator.startPage.url
      }
    }

    "Succeed with the supplied page if user answers are available and a next page is present" in {
      val userAnswersWithNextPage = testUserAnswers.setNextPage(testCall)
      val controller = testSetup(Some(userAnswersWithNextPage))
      val action = controller.restore(testSubmissionId)
      val result = action.apply(testRequest)
      whenReady(result) { res =>
        res.header.status mustBe SEE_OTHER
        res.header.headers("Location") mustEqual testCall.url
      }
    }
  }

}
