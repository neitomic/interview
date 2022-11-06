package forex.services.rates.interpreters

import forex.domain.Currency
import io.circe.{ Decoder, HCursor }
import io.circe.generic.semiauto._
import io.circe.Decoder.decodeList

import java.time.OffsetDateTime
import scala.util.Try

package object oneframe {

  implicit val decodeInstant: Decoder[OffsetDateTime] = Decoder.decodeString.emapTry { str =>
    Try(OffsetDateTime.parse(str))
  }
  implicit val currencyDecoder: Decoder[Currency] = Decoder.decodeString.emapTry { str =>
    Try(Currency.fromString(str))
  }

  implicit val rateDecoder: Decoder[Rate] = (c: HCursor) =>
    for {
      from <- c.downField("from").as[Currency]
      to <- c.downField("to").as[Currency]
      bid <- c.downField("bid").as[Double]
      ask <- c.downField("ask").as[Double]
      price <- c.downField("price").as[Double]
      timestamp <- c.downField("time_stamp").as[OffsetDateTime]
    } yield {
      Rate(from, to, bid, ask, price, timestamp)
  }

  implicit val errorDecoder: Decoder[Error] = deriveDecoder[Error]
  implicit val rateResponseDecoder: Decoder[Either[Error, List[Rate]]] = {
    val right: Decoder[Either[Error, List[Rate]]] =
      decodeList(rateDecoder).map(value => Right[Error, List[Rate]](value))
    val left: Decoder[Either[Error, List[Rate]]] = errorDecoder.map(error => Left[Error, List[Rate]](error))
    right or left
  }
}
