package io.github.thibaultbee.krtmp.common.logger

interface IKrtmpLogger {
    /**
     * Logs an error.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun e(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a warning.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun w(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs an info.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun i(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a verbose message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun v(tag: String, message: String, tr: Throwable? = null)

    /**
     * Logs a debug message.
     *
     * @param tag calling object
     * @param message the message to log
     * @param tr exception to log (may be null).
     */
    fun d(tag: String, message: String, tr: Throwable? = null)
}