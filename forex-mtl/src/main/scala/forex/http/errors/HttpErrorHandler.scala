package forex.http.errors

import org.http4s.HttpRoutes

trait HttpErrorHandler[F[_]] {
  def handle(routes: HttpRoutes[F]): HttpRoutes[F]
}

object HttpErrorHandler {
  def apply[F[_]](implicit ev: HttpErrorHandler[F]) = ev
}
