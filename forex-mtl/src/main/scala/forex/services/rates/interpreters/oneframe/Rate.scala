package forex.services.rates.interpreters.oneframe

import forex.domain.Currency

import java.time.OffsetDateTime

case class Rate(
    from: Currency,
    to: Currency,
    bid: Double,
    ask: Double,
    price: Double,
    timestamp: OffsetDateTime
)
