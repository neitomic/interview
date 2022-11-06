package forex.services.rates.interpreters

import forex.services.rates.Algebra
import cats.Applicative
import cats.syntax.applicative._
import cats.syntax.either._
import forex.domain.{ Price, Rate, Timestamp }
import forex.services.rates.errors.Error.BatchError
import forex.services.rates.errors._

class OneFrameDummy[F[_]: Applicative] extends Algebra[F] {

  override def get(pair: Rate.Pair): F[Error Either Rate] =
    Rate(pair, Price(BigDecimal(100)), Timestamp.now).asRight[Error].pure[F]

  override def mget(pairs: List[Rate.Pair]): F[Either[Error, List[Rate]]] = {
    import cats.implicits._
    pairs
      .traverse(get)
      .map(eitherList => {
        eitherList.partition(_.isLeft) match {
          case (Nil, rates) => Right(for (Right(i) <- rates) yield i)
          case (errors, _)  => Left(BatchError(for (Left(s) <- errors) yield s))
        }
      })
  }
}
