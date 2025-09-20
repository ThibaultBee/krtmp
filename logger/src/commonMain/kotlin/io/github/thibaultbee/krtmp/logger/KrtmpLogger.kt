package io.github.thibaultbee.krtmp.logger

object KrtmpLogger {
    /**
     * The logger implementation.
     * Customize it by setting a new [IKrtmpLogger] implementation.
     */
    var logger: IKrtmpLogger = DefaultKrtmpLogger()

    /**
     * Logs an error.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun e(tag: String, message: String, tr: Throwable? = null) = logger.e(tag, message, tr)

    /**
     * Logs a warning.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun w(tag: String, message: String, tr: Throwable? = null) = logger.w(tag, message, tr)

    /**
     * Logs an info.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun i(tag: String, message: String, tr: Throwable? = null) = logger.i(tag, message, tr)

    /**
     * Logs a verbose message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun v(tag: String, message: String, tr: Throwable? = null) = logger.v(tag, message, tr)

    /**
     * Logs a debug message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun d(tag: String, message: String, tr: Throwable? = null) = logger.d(tag, message, tr)
}