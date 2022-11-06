package forex.http.rates

import forex.domain.Currency
import org.http4s.{ ParseFailure, QueryParamDecoder }
import org.http4s.dsl.impl.OptionalValidatingQueryParamDecoderMatcher

import scala.util.Try

object QueryParams {

  private[http] implicit val currencyQueryParam: QueryParamDecoder[Currency] =
    QueryParamDecoder[String].emap { value =>
      Try(Currency.fromString(value)).toEither.left.map(t => ParseFailure(t.getMessage, t.getMessage))
    }

  object FromQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("from")
  object ToQueryParam extends OptionalValidatingQueryParamDecoderMatcher[Currency]("to")

}
