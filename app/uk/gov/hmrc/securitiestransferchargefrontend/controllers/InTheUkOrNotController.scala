package uk.gov.hmrc.securitiestransferchargefrontend.controllers

import uk.gov.hmrc.securitiestransferchargefrontend.controllers.actions._
import uk.gov.hmrc.securitiestransferchargefrontend.forms.InTheUkOrNotFormProvider
import javax.inject.Inject
import uk.gov.hmrc.securitiestransferchargefrontend.models.Mode
import uk.gov.hmrc.securitiestransferchargefrontend.navigation.Navigator
import uk.gov.hmrc.securitiestransferchargefrontend.pages.InTheUkOrNotPage
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.securitiestransferchargefrontend.repositories.SessionRepository
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController
import uk.gov.hmrc.securitiestransferchargefrontend.views.html.InTheUkOrNotView

import scala.concurrent.{ExecutionContext, Future}

class InTheUkOrNotController @Inject()(
                                        override val messagesApi: MessagesApi,
                                        sessionRepository: SessionRepository,
                                        navigator: Navigator,
                                        identify: IdentifierAction,
                                        getData: DataRetrievalAction,
                                        requireData: DataRequiredAction,
                                        formProvider: InTheUkOrNotFormProvider,
                                        val controllerComponents: MessagesControllerComponents,
                                        view: InTheUkOrNotView
                                 )(implicit ec: ExecutionContext) extends FrontendBaseController with I18nSupport {

  val form = formProvider()

  def onPageLoad(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData) {
    implicit request =>

      val preparedForm = request.userAnswers.get(InTheUkOrNotPage) match {
        case None => form
        case Some(value) => form.fill(value)
      }

      Ok(view(preparedForm, mode))
  }

  def onSubmit(mode: Mode): Action[AnyContent] = (identify andThen getData andThen requireData).async {
    implicit request =>

      form.bindFromRequest().fold(
        formWithErrors =>
          Future.successful(BadRequest(view(formWithErrors, mode))),

        value =>
          for {
            updatedAnswers <- Future.fromTry(request.userAnswers.set(InTheUkOrNotPage, value))
            _              <- sessionRepository.set(updatedAnswers)
          } yield Redirect(navigator.nextPage(InTheUkOrNotPage, mode, updatedAnswers))
      )
  }
}
