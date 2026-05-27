package services.fileupload.processing

import base.SpecBase
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{Request, RequestHeader, Result, Results, Session}
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.SessionFileProcessingRefreshCounter

class SessionFileProcessingRefreshCounterSpec extends SpecBase with MockitoSugar {
  private val retryCountKey = "retryCount"

  def setup(retryCount: Int): SessionFileProcessingRefreshCounter = {
    val mockConfig = mock[FrontendAppConfig]
    when(mockConfig.spinnerPageRefreshTimeout).thenReturn(30)
    when(mockConfig.spinnerPageRefreshInterval).thenReturn(2)

    val mockRequest = mock[Request[?]]
    when(mockRequest.session).thenReturn(Session(Map(retryCountKey -> retryCount.toString)))

    new SessionFileProcessingRefreshCounter(mockConfig, mockRequest)
  }

  "SessionFileProcessingRefreshCounter" - {

    "should return the current count from the session" in {
      val counter = setup(5)
      counter.currentCount mustBe 5
    }

    "should return false for isTimedOut if the counter is below the threshold" in {
      val counter = setup(1)
      counter.isTimedOut mustBe false
    }

    "should return true for isTimedOut if the counter is at the threshold" in {
      val counter = setup(15)
      counter.isTimedOut mustBe true
    }

    "should return true for isTimedOut if the counter is above the threshold" in {
      val counter = setup(16)
      counter.isTimedOut mustBe true
    }

    "should increment the counter in the session when withIncrementedCounter is called" in {
      val counter = setup(5)
      val noContentResult: Result = Results.NoContent
      implicit val mockHeader: RequestHeader = mock[RequestHeader]
      val result = counter.withIncrementedCounter(noContentResult)
      result.session.get(retryCountKey) mustBe Some("6")
    }
  }
}
