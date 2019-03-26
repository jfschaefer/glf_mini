package info.kwarc.glf

class GlfException(private val message: String = "",
                   private val cause: Throwable = None.orNull)
    extends Exception(message, cause)


final case class MmtTermMissing(private val message: String = "",
                                private val cause: Throwable = None.orNull)
    extends GlfException(message, cause)
