package forex

import cats.effect._
import cats.effect.implicits._
import forex.config._
import fs2.Stream
import org.http4s.client.blaze.BlazeClientBuilder
import org.http4s.server.blaze.BlazeServerBuilder

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] =
    new Application[IO].stream(executionContext).compile.drain.as(ExitCode.Success)

}

class Application[F[_]: ConcurrentEffect: Timer] {

  def stream(ec: ExecutionContext): Stream[F, Unit] =
    for {
      config <- Config.stream("app")
      client <- Stream.resource(BlazeClientBuilder[F](ec).resource)
      module = new Module[F](config, client)
      _ <- Stream.eval(module.cacheRefresher.scheduler().start)
      _ <- BlazeServerBuilder[F](ec)
            .bindHttp(config.http.port, config.http.host)
            .withHttpApp(module.httpApp)
            .serve
    } yield ()

}
