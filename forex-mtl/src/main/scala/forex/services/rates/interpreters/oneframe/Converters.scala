package forex.services.rates.interpreters.oneframe

object Converters {

  implicit class OneFrameRateExtensions(val rate: Rate) extends AnyVal {
    def asDomain: forex.domain.Rate =
      forex.domain.Rate(
        forex.domain.Rate.Pair(rate.from, rate.to),
        forex.domain.Price(BigDecimal(rate.price)),
        forex.domain.Timestamp(rate.timestamp)
      )
  }
}
