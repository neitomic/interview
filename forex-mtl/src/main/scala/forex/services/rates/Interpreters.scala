package forex.services.rates

import cats.Applicative
import cats.effect.Sync
import forex.config.{ CacheConfig, OneFrameConfig }
import forex.services.rates.interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F] = new OneFrameDummy[F]()
  def live[F[_]: Sync](
      client: Client[F],
      oneFrameConfig: OneFrameConfig
  ): Algebra[F] = new OneFrameLive[F](client, oneFrameConfig)
  def hotCached[F[_]: Sync](client: Client[F], oneFrameConfig: OneFrameConfig, cacheConfig: CacheConfig) =
    new OneFrameHotCached(live(client, oneFrameConfig), cacheConfig)
}
