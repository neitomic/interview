package forex.http.errors

import cats.data.NonEmptyList
import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder
import org.http4s.ParseFailure

object Protocol {

  implicit val configuration: Configuration = Configuration.default.withSnakeCaseMemberNames

  case class ApiErrorResponse(errors: NonEmptyList[String])

  sealed trait HandledHttpError extends Exception {
    def asApiResponse: ApiErrorResponse
  }

  case class RequestParseError(errors: NonEmptyList[ParseFailure]) extends HandledHttpError {
    override def asApiResponse: ApiErrorResponse =
      ApiErrorResponse(errors.map(_.sanitized))
  }
  case class GenericError(error: Throwable) extends HandledHttpError {
    override def asApiResponse: ApiErrorResponse =
      ApiErrorResponse(NonEmptyList("Something went wrong, we are investigating!", Nil))
  }

  implicit val errorResponseEncoder: Encoder[ApiErrorResponse] =
    deriveConfiguredEncoder[ApiErrorResponse]

}
