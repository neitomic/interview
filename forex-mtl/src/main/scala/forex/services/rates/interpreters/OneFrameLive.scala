package forex.services.rates.interpreters

import cats.data.EitherT
import cats.effect.Sync
import cats.implicits._
import forex.config.OneFrameConfig
import forex.domain
import forex.services.rates.interpreters.oneframe.Converters._
import forex.services.rates.interpreters.oneframe._
import forex.services.rates.{Algebra, errors}
import org.http4s.circe.CirceEntityDecoder._
import org.http4s.client.Client
import org.http4s._

class OneFrameLive[F[_]: Sync](
    client: Client[F],
    oneFrameConfig: OneFrameConfig
) extends Algebra[F] {
  private final val baseUri = Uri.unsafeFromString(oneFrameConfig.url)
  override def get(pair: domain.Rate.Pair): F[errors.Error Either domain.Rate] =
    for {
      rates <- mget(List(pair))
    } yield
      rates.flatMap {
        case head :: Nil => Right(head)
        case list =>
          Left(errors.Error.OneFrameLookupFailed(s"OneFrame lookup returns ${list.size} items. Expecting 1."))
      }

  override def mget(pairs: List[domain.Rate.Pair]): F[errors.Error Either List[domain.Rate]] = {
    val queryUri = baseUri / "rates" +? ("pair", pairs.map(_.pairString))
    val request = Request[F](
      method = Method.GET,
      uri = queryUri,
      headers = Headers.of(Header("token", oneFrameConfig.token))
    )

    EitherT(client.expect[Either[oneframe.Error, List[Rate]]](request))
      .map(_.map(rate => rate.asDomain))
      .leftMap[errors.Error](error => errors.Error.OneFrameLookupFailed(error.error))
      .value
  }
}
