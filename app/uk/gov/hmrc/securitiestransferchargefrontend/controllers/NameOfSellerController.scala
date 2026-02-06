package uk.gov.hmrc.securitiestransferchargefrontend.controllers

import play.api.data.Form
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions.*
import uk.gov.hmrc.securitiestransferchargefrontend.forms.NameOfSellerFormProvider
import uk.gov.hmrc.securitiestransferchargefrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.NameOfSellerPage
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.NameOfSellerView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NameOfSellerController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        stcAuthEnrolled: StcAuthEnrolledAction,
                                        getData: StcDataRetrievalAction,
                                        requireData: StcDataRequiredAction,
                                        formProvider: NameOfSellerFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: NameOfSellerView
                                    )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form: Form[String] = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async{
    implicit request =>

      val preparedForm = request.userAnswers.get(NameOfSellerPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Future.successful(Ok(view(preparedForm, mode)))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (stcAuthEnrolled andThen getData andThen requireData).async{
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(NameOfSellerPage, value))
            _              <- sessionRepository.set(updatedAnswers)
            nextPage       <- navigator.nextPage(NameOfSellerPage, mode, updatedAnswers)
          } yield Redirect(nextPage)
      )
  }
}
