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

package controllers.stf.individuals.single

import base.SpecBase
import base.stubs.StubPersistentNavigator
import com.google.inject.name.Names
import play.api.inject.bind
import play.api.test.FakeRequest
import play.api.test.Helpers.*
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.stf.individuals.single.routes
import uk.gov.hmrc.securitiestransferchargefrontend.models.CheckMode
import uk.gov.hmrc.securitiestransferchargefrontend.models.stf.*
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.{Navigator, PersistentNavigator}
import uk.gov.hmrc.securitiestransferchargefrontend.pages.stf.single.*

import java.time.LocalDate

class CheckYourAnswersControllerSpec extends SpecBase {

  private def completeUserAnswers = {
    emptyUserAnswers
      .set(ConfirmAddressPage, ConfirmableAddress(List("123 Main Street", "London"), "SW1A 1AA", Some(Country("United Kingdom", "GB")))).success.value
      .set(StfBuyersAddressPage, AlfConfirmedAddress("audit-ref-123", Some("id-123"), AlfAddress(List("123 Main Street", "London"), "SW1A 1AA", Country("United Kingdom", "GB")))).success.value
      .set(NameOfSellerPage, "Jane Smith").success.value
      .set(StfSellerAddressPage, AlfConfirmedAddress("audit-ref-456", Some("id-456"), AlfAddress(List("456 High Street", "Manchester"), "M1 1AA", Country("United Kingdom", "GB")))).success.value
      .set(ConnectedPersonsPage, true).success.value
      .set(ApplyingForReliefPage, false).success.value
      .set(SecuritiesTargetPage, SecuritiesTarget("Company ABC Ltd", Some("12345678"))).success.value
      .set(ChargingPointPage, LocalDate.of(2026, 6, 15)).success.value
      .set(TaxRatePage, TaxRate.HalfPercent).success.value
      .set(PurchasingSharesPage, true).success.value
      .set(DetailsOfThisTransferPage, DetailsOfThisTransfer(
        numberOfShares = 100,
        typeOfShares = "ordinary shares",
        amountPaid = BigDecimal("8000.00"),
        marketValue = Some(BigDecimal("10000.50"))
      )).success.value
  }

  "CheckYourAnswersController" - {

    "must return OK and the correct view for a GET" in {
      val stubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your answers")
      }
    }

    "must redirect to Journey Recovery for a GET if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to next page for a POST" in {
      val stubNavigator = new StubPersistentNavigator(testNextPage, completeUserAnswers, "stf", "")

      val application = applicationBuilder(userAnswers = Some(completeUserAnswers))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator)
        )
        .build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual testNextPage.url
      }
    }

    "must redirect to Journey Recovery for a POST if no existing data is found" in {
      val application = applicationBuilder(userAnswers = None).build()

      running(application) {
        val request = FakeRequest(POST, routes.CheckYourAnswersController.onSubmit().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual uk.gov.hmrc.securitiestransferchargefrontend.controllers.routes.JourneyRecoveryController.onPageLoad().url
      }
    }

    "must redirect to ConfirmAddressPage when ConfirmAddressPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(ConfirmAddressPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ConfirmAddressController.onPageLoad().url
      }
    }

    "must redirect to NameOfSellerPage when NameOfSellerPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(NameOfSellerPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.NameOfSellerController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to StfSellerAddressPage when StfSellerAddressPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(StfSellerAddressPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.StfSellerAddressController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to ConnectedPersonsPage when ConnectedPersonsPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(ConnectedPersonsPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ConnectedPersonsController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to ApplyingForReliefPage when ApplyingForReliefPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(ApplyingForReliefPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ApplyingForReliefController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to SecuritiesTargetPage when SecuritiesTargetPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(SecuritiesTargetPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SecuritiesTargetController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to ChargingPointPage when ChargingPointPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(ChargingPointPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.ChargingPointController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to TaxRatePage when TaxRatePage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(TaxRatePage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.TaxRateController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to PurchasingSharesPage when PurchasingSharesPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(PurchasingSharesPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.PurchasingSharesController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to DetailsOfThisTransferPage when ConnectedPersons is true but marketValue is missing" in {
      val incompleteAnswers = completeUserAnswers
        .set(DetailsOfThisTransferPage, DetailsOfThisTransfer(
          numberOfShares = 100,
          typeOfShares = "ordinary shares",
          amountPaid = BigDecimal("8000.00"),
          marketValue = None
        )).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url
      }
    }

    "must redirect to SecuritiesTargetPage when SecuritiesTarget businessName is empty" in {
      val incompleteAnswers = completeUserAnswers
        .set(SecuritiesTargetPage, SecuritiesTarget("", Some("12345678"))).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.SecuritiesTargetController.onPageLoad(CheckMode).url
      }
    }

    "must return OK when SecuritiesTarget has businessName but CRN is None (CRN is optional)" in {
      val answersWithNoCRN = completeUserAnswers
        .set(ConnectedPersonsPage, false).success.value
        .set(SecuritiesTargetPage, SecuritiesTarget("Company ABC Ltd", None)).success.value
        .set(DetailsOfThisTransferPage, DetailsOfThisTransfer(
          numberOfShares = 100,
          typeOfShares = "ordinary shares",
          amountPaid = BigDecimal("8000.00"),
          marketValue = None
        )).success.value

      val stubNavigator = new StubPersistentNavigator(testNextPage, answersWithNoCRN, "stf", "")

      val application = applicationBuilder(userAnswers = Some(answersWithNoCRN))
        .overrides(
          bind[Navigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator),
          bind[PersistentNavigator].qualifiedWith(Names.named("individuals")).toInstance(stubNavigator)
        )
        .build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) must include("Check your answers")
      }
    }

    "must redirect to DetailsOfThisTransferPage when DetailsOfThisTransferPage data is missing" in {
      val incompleteAnswers = completeUserAnswers.remove(DetailsOfThisTransferPage).success.value

      val application = applicationBuilder(userAnswers = Some(incompleteAnswers)).build()

      running(application) {
        val request = FakeRequest(GET, routes.CheckYourAnswersController.onPageLoad().url)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result).value mustEqual routes.DetailsOfThisTransferController.onPageLoad(CheckMode).url
      }
    }
  }
}
