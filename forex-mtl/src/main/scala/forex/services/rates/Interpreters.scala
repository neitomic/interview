package forex.services.rates

import cats.Applicative
import interpreters._
import org.http4s.client.Client

object Interpreters {
  def dummy[F[_]: Applicative]: Algebra[F]                   = new OneFrameDummy[F]()
  def live[F[_]: Applicative](client: Client[F]): Algebra[F] = new OneFrameLive[F](client)
}
