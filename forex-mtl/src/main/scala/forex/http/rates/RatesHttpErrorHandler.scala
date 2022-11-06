package forex.http.rates

import cats.effect.Sync
import cats.syntax.apply._
import forex.http.errors.Protocol.{ GenericError, RequestParseError }
import forex.http.errors.{ HttpErrorHandler, RoutesHttpErrorHandler }
import org.http4s.dsl.Http4sDsl
import org.http4s.{ HttpRoutes, Response }
import org.typelevel.log4cats.slf4j.Slf4jLogger

class RatesHttpErrorHandler[F[_]: Sync]() extends HttpErrorHandler[F] with Http4sDsl[F] {
  implicit val logger = Slf4jLogger.getLogger[F]

  private val handler: Throwable => F[Response[F]] = {
    case parseFailure: RequestParseError =>
      BadRequest(parseFailure.asApiResponse)
    case genericError: GenericError =>
      logger.error(genericError.error)("Something went wrong.") *> InternalServerError(genericError.asApiResponse)
    case throwable: Throwable =>
      logger.error(throwable)("Something went wrong.") *> InternalServerError(GenericError(throwable).asApiResponse)
  }

  override def handle(routes: HttpRoutes[F]): HttpRoutes[F] =
    RoutesHttpErrorHandler(routes)(handler)
}
