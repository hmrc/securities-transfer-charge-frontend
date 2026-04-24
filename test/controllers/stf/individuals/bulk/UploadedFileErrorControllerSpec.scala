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

package controllers.stf.individuals.bulk

import base.SpecBase
import org.mockito.ArgumentMatchers.{any as anyArg, eq as eqTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatestplus.mockito.MockitoSugar
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.bulk.routes as individualRoutes
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.fileupload.{FileParseError, ParsedStcRow, ParsedValue, StcFileValidationResponse, StcRowValidationError, ValidatedStcRow}
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.UploadedFileError
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.upscan.{FileUpload, UpscanJourneyStatus}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.UpscanJourneyRepository
import uk.gov.hmrc.securitiestransferchargefrontend.services.fileupload.StcUpscanProcessingService
import uk.gov.hmrc.securitiestransferchargefrontend.viewmodels.stf.fileupload.UploadedFileErrorMapper
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.stf.individuals.bulk.UploadedFileErrorView

import java.time.LocalDate
import scala.concurrent.Future

class UploadedFileErrorControllerSpec
  extends SpecBase
    with MockitoSugar
    with BeforeAndAfterEach {

  private val mockUpscanJourneyRepository    = mock[UpscanJourneyRepository]
  private val mockStcUpscanProcessingService = mock[StcUpscanProcessingService]

  private val reference = "test-reference"

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockUpscanJourneyRepository, mockStcUpscanProcessingService)
  }

  private def application =
    applicationBuilder(userAnswers = Some(emptyUserAnswers))
      .overrides(
        bind[UpscanJourneyRepository].toInstance(mockUpscanJourneyRepository),
        bind[StcUpscanProcessingService].toInstance(mockStcUpscanProcessingService)
      )
      .build()

  private val readyFileUpload: FileUpload =
    FileUpload(
      reference = reference,
      status = UpscanJourneyStatus.Ready
    )

  private val failedFileUpload: FileUpload =
    FileUpload(
      reference = reference,
      status = UpscanJourneyStatus.Failed
    )

  private val parsedRow: ParsedStcRow =
    ParsedStcRow(
      rowNumber = 6,
      sellerName = ParsedValue.Valid("Seller Ltd"),
      sellerAddressInUk = ParsedValue.Valid(true),
      sellerAddressLine1 = ParsedValue.Valid("1 Test Street"),
      sellerAddressLine2 = ParsedValue.Missing,
      sellerAddressLine3 = ParsedValue.Missing,
      sellerAddressLine4 = ParsedValue.Missing,
      sellerPostcode = ParsedValue.Valid("AA1 1AA"),
      sellerCountry = ParsedValue.Missing,
      connectedPersons = ParsedValue.Valid(false),
      applyingForRelief = ParsedValue.Valid(false),
      whatReliefAreYouApplyingFor = ParsedValue.Missing,
      securitiesTarget = ParsedValue.Valid("Target Ltd"),
      companyRegistrationNumber = ParsedValue.Valid("12345678"),
      chargingPoint = ParsedValue.Valid(LocalDate.of(2025, 11, 20)),
      taxRate = ParsedValue.Valid(BigDecimal("0.5")),
      whatTypeOfSecurities = ParsedValue.Valid("Shares"),
      typeOfShares = ParsedValue.Valid("Ordinary"),
      securitiesQuantity = ParsedValue.Valid(BigDecimal(100)),
      amountPaidForSecurities = ParsedValue.Valid(BigDecimal(1000)),
      totalMarketValue = ParsedValue.Missing
    )

  private val blockingValidationErrors: Seq[StcRowValidationError] =
    Seq(
      StcRowValidationError(
        rowNumber = 6,
        fieldName = "sellerName",
        columnIndex = 7,
        message = "Enter the seller's full name",
        blocking = true
      ),
      StcRowValidationError(
        rowNumber = 6,
        fieldName = "sellerAddressLine1",
        columnIndex = 9,
        message = "Enter the first line of your address",
        blocking = true
      )
    )

  private val expectedUploadedFileErrors: Seq[UploadedFileError] =
    UploadedFileErrorMapper.from(blockingValidationErrors)

  private def validationResponseWithErrors(errors: Seq[StcRowValidationError]): StcFileValidationResponse =
    StcFileValidationResponse(
      rows = Seq(
        ValidatedStcRow(
          parsedRow = parsedRow,
          validationErrors = errors
        )
      )
    )

  private def blockingErrors(count: Int): Seq[StcRowValidationError] =
    (1 to count).map { i =>
      StcRowValidationError(
        rowNumber = i + 2,
        fieldName = "sellerName",
        columnIndex = 7,
        message = s"Error $i",
        blocking = true
      )
    }

  "UploadedFileErrorController" - {

    "must return OK and the correct view for a GET when blocking errors are present" in {

      val validationResponse = validationResponseWithErrors(blockingValidationErrors)

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(readyFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(readyFileUpload))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[UploadedFileErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedUploadedFileErrors)(request, messages(app)).toString
      }
    }

    "must return OK and the correct view when there are 25 blocking errors" in {

      val errors = blockingErrors(25)
      val validationResponse = validationResponseWithErrors(errors)
      val expectedErrors = UploadedFileErrorMapper.from(errors)

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(readyFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(readyFileUpload))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        val view = app.injector.instanceOf[UploadedFileErrorView]

        status(result) mustEqual OK
        contentAsString(result) mustEqual view(expectedErrors)(request, messages(app)).toString
      }
    }

    "must redirect to formatting error page when there are 26 or more blocking errors" in {

      val validationResponse = validationResponseWithErrors(blockingErrors(26))

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(readyFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(readyFileUpload))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.FormattingErrorController.onPageLoad().url
      }
    }

    "must redirect to journey recovery when the upload cannot be found" in {

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(None))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to file upload page when the upload status is not Ready" in {

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(failedFileUpload)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.FileUploadController.onPageLoad().url
      }
    }

    "must redirect to file uploaded page when there are no blocking errors" in {

      val validationResponse = validationResponseWithErrors(Seq.empty)

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(readyFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(readyFileUpload))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Right(validationResponse)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.FileUploadedController.onPageLoad(reference).url
      }
    }

    "must redirect to formatting error page when parsing fails" in {

      val parseError = FileParseError.InvalidXlsx("Test parse error")

      when(mockUpscanJourneyRepository.find(reference))
        .thenReturn(Future.successful(Some(readyFileUpload)))

      when(mockStcUpscanProcessingService.process(eqTo(readyFileUpload))(using anyArg[HeaderCarrier]))
        .thenReturn(Future.successful(Left(parseError)))

      val app = application

      running(app) {
        val request = FakeRequest(GET, individualRoutes.UploadedFileErrorController.onPageLoad(reference).url)

        val result = route(app, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual individualRoutes.FormattingErrorController.onPageLoad().url
      }
    }
  }
}