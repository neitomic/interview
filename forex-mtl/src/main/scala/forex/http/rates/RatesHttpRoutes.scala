package forex.http
package rates

import cats.data.ValidatedNel
import cats.effect.Sync
import cats.implicits.toFunctorOps
import cats.syntax.flatMap._
import forex.domain.Currency
import forex.http.errors.HttpErrorHandler
import forex.http.errors.Protocol.RequestParseError
import forex.programs.RatesProgram
import forex.programs.rates.{ Protocol => RatesProgramProtocol }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import org.http4s.{ HttpRoutes, ParseFailure }

class RatesHttpRoutes[F[_]: Sync](rates: RatesProgram[F])(implicit H: HttpErrorHandler[F]) extends Http4sDsl[F] {

  import Converters._
  import Protocol._
  import QueryParams._

  private[http] val prefixPath = "/rates"

  private def validateCurrencyParam(
      param: Option[ValidatedNel[ParseFailure, Currency]]
  ): F[Currency] =
    param.fold(Sync[F].raiseError[Currency](ParseFailure("", ""))) { value =>
      value.toEither match {
        case Right(currency) => Sync[F].pure(currency)
        case Left(errors)    => Sync[F].raiseError[Currency](RequestParseError(errors))
      }
    }

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root :? FromQueryParam(maybeFrom) +& ToQueryParam(maybeTo) =>
      for {
        from <- validateCurrencyParam(maybeFrom)
        to <- validateCurrencyParam(maybeTo)
        rateResponse <- rates.get(RatesProgramProtocol.GetRatesRequest(from, to))
        rate <- Sync[F].fromEither(rateResponse)
        resp <- Ok(rate.asGetApiResponse)
      } yield resp
  }

  val routes: HttpRoutes[F] = H.handle(
    Router(
      prefixPath -> httpRoutes
    )
  )

}
