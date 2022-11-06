package forex.http.errors

import cats.data.{ Kleisli, OptionT }
import cats.effect.Sync
import cats.implicits._
import org.http4s.{ HttpRoutes, Request, Response }

object RoutesHttpErrorHandler {
  def apply[F[_]: Sync](
      routes: HttpRoutes[F]
  )(handler: Throwable => F[Response[F]]): HttpRoutes[F] =
    Kleisli { req: Request[F] =>
      OptionT {
        routes.run(req).value.handleErrorWith { e =>
          handler(e).map(Option(_))
        }
      }
    }
}
