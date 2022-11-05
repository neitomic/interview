package forex.services.rates

object errors {

  sealed trait Error {
    def msg: String
  }
  object Error {
    final case class OneFrameLookupFailed(msg: String) extends Error
    final case class BatchError(errors: List[Error]) extends Error {
      override def msg: String = errors.map(_.msg).mkString("; ")
    }
  }

}
