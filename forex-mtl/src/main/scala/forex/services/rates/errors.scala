package forex.services.rates

import forex.domain.Rate

object errors {

  sealed trait Error {
    def msg: String
  }
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class StaledCacheRate(rate: Rate) extends Error {
      override def msg: String = s"The cached rate for ${rate.pair.pairString} is staled"
    }
    final case class NoCachedRate(pair: Rate.Pair) extends Error {
      override def msg: String = s"The rate for ${pair.pairString} is not found in cache"
    }
    final case class BatchError(errors: List[Error]) extends Error {
      override def msg: String = errors.map(_.msg).mkString("; ")
    }
  }

}
