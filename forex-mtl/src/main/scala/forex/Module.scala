package forex

import cats.effect.{ Concurrent, Sync, Timer }
import forex.cache.HotCacheRefresher
import forex.config.ApplicationConfig
import forex.http.errors.HttpErrorHandler
import forex.http.rates.{ RatesHttpErrorHandler, RatesHttpRoutes }
import forex.services._
import forex.programs._
import forex.services.rates.interpreters.OneFrameHotCached
import org.http4s._
import org.http4s.client.Client
import org.http4s.implicits._
import org.http4s.server.middleware.{ AutoSlash, Timeout }

class Module[F[_]: Concurrent: Timer: Sync](
    config: ApplicationConfig,
    client: Client[F]
) {

  private val ratesService: OneFrameHotCached[F] = RatesServices.hotCached[F](client, config.oneFrame, config.cache)
  val cacheRefresher: HotCacheRefresher[F]       = HotCacheRefresher.of(ratesService, config.cache)

  private val ratesProgram: RatesProgram[F] = RatesProgram[F](ratesService)

  private implicit val ratesErrorHandler: HttpErrorHandler[F] = new RatesHttpErrorHandler[F]()
  private val ratesHttpRoutes: HttpRoutes[F]                  = new RatesHttpRoutes[F](ratesProgram).routes

  type PartialMiddleware = HttpRoutes[F] => HttpRoutes[F]
  type TotalMiddleware   = HttpApp[F] => HttpApp[F]

  private val routesMiddleware: PartialMiddleware = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    }
  }

  private val appMiddleware: TotalMiddleware = { http: HttpApp[F] =>
    Timeout(config.http.timeout)(http)
  }

  private val http: HttpRoutes[F] = ratesHttpRoutes

  val httpApp: HttpApp[F] = appMiddleware(routesMiddleware(http).orNotFound)

}
