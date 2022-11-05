package forex.services.rates.interpreters

import cats.Applicative
import cats.effect._
import cats.implicits._
import forex.cache.HotCached
import forex.domain.{ Currency, Rate }
import forex.services.rates.errors.Error
import forex.services.rates.errors.Error.BatchError
import forex.services.rates.{ errors, Algebra }
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.ConcurrentHashMap

class OneFrameHotCached[F[_]: Applicative: Sync](
    underlying: Algebra[F]
) extends Algebra[F]
    with HotCached[F] {

  implicit val logger = Slf4jLogger.getLogger[F]
  private val _cache  = new ConcurrentHashMap[Rate.Pair, Rate]()

  override def get(pair: Rate.Pair): F[Either[errors.Error, Rate]] =
    for {
      opt <- Option(_cache.get(pair)).pure[F]
    } yield
      opt match {
        case Some(rate) => Right(rate)
        case None       => Left(Error.OneFrameLookupFailed("Not found"))
      }

  override def mget(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] =
    pairs
      .traverse(get)
      .map(eitherList => {
        eitherList.partition(_.isLeft) match {
          case (Nil, rates) => Right(for (Right(i) <- rates) yield i)
          case (errors, _)  => Left(BatchError(for (Left(s) <- errors) yield s))
        }
      })

  private def allPairs(): F[List[Rate.Pair]] =
    Applicative[F].pure {
      val allPairs      = List.newBuilder[Rate.Pair]
      val allCurrencies = Currency.all
      for (i <- 0 until (allCurrencies.size - 1)) {
        for (j <- (i + 1) until allCurrencies.size) {
          allPairs += Rate.Pair(allCurrencies(i), allCurrencies(j))
          allPairs += Rate.Pair(allCurrencies(j), allCurrencies(i))
        }
      }
      allPairs.result()
    }

  private def updateCache(rates: List[Rate]): F[Unit] =
    Applicative[F].pure(rates.foreach(rate => _cache.put(rate.pair, rate)))

  override def reloadCache(): F[Unit] =
    for {
      pairs <- allPairs()
      _ <- Logger[F].info(s"Reloading cache with all pairs: ${pairs.map(_.pairString).mkString(",")}")
      rates <- underlying.mget(pairs) //todo: batching
      _ <- rates match {
            case Right(rates) => updateCache(rates)
            case Left(error) =>
              Logger[F].error(s"Error when reload cache: ${error.msg}")
          }
      _ <- Logger[F].info("Cache reloaded")
    } yield {}
}
