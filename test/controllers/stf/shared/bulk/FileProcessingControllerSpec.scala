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

package controllers.stf.shared.bulk

import base.SpecBase
import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.Request
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.config.FrontendAppConfig
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.shared.bulk.routes
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.processing.{FileProcessingRefreshCounter, FileProcessingRefreshCounterFactory}
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.shared.bulk.FileProcessingView
import play.api.inject.bind
import org.mockito.Mockito.when

class MockFileProcessingRefreshCounterFactory(counter: FileProcessingRefreshCounter) extends FileProcessingRefreshCounterFactory {
  override def apply(request: Request[?]): FileProcessingRefreshCounter = counter
}

class FileProcessingControllerSpec extends SpecBase with MockitoSugar {

  "FileProcessingController" - {

    "must return OK and the correct view for a GET" in {

      val application = applicationBuilder(userAnswers = Some(emptyUserAnswers)).build()

      running(application) {

        val request = FakeRequest(GET, routes.FileProcessingController.onPageLoad().url)
        val result = route(application, request).value
        val view = application.injector.instanceOf[FileProcessingView]
        val appConfig = application.injector.instanceOf[FrontendAppConfig]
        val refreshInterval = appConfig.spinnerPageRefreshInterval

        status(result) mustEqual OK

        contentAsString(result) mustEqual view(refreshInterval)(request, messages(application)).toString
      }
    }
  }

  "must return the timed out view if the number of retries is exceeded" in {

    val mockCounter = mock[FileProcessingRefreshCounter]
    when(mockCounter.isTimedOut).thenReturn(true)
    val factory = new MockFileProcessingRefreshCounterFactory(mockCounter)
    val application =
      applicationBuilder(userAnswers = Some(emptyUserAnswers))
        .overrides(bind[FileProcessingRefreshCounterFactory].toInstance(factory))
        .build()

    running(application) {

      val request = FakeRequest(GET, routes.FileProcessingController.onPageLoad().url)
      val result = route(application, request).value
      status(result) mustEqual SEE_OTHER
      redirectLocation(result).value mustEqual routes.FileProcessingController.onTimeout().url
    }
  }
}

