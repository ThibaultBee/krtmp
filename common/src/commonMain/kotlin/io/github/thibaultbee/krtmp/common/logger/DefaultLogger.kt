package io.github.thibaultbee.krtmp.common.logger

/**
 * Default logger implementation.
 */
class DefaultLogger: ILogger {
    override fun e(tag: String, message: String, tr: Throwable?) {
        println("E/$tag: $message")
        tr?.printStackTrace()
    }

    override fun w(tag: String, message: String, tr: Throwable?) {
        println("W/$tag: $message")
        tr?.printStackTrace()
    }

    override fun i(tag: String, message: String, tr: Throwable?) {
        println("I/$tag: $message")
        tr?.printStackTrace()
    }

    override fun v(tag: String, message: String, tr: Throwable?) {
        println("V/$tag: $message")
        tr?.printStackTrace()
    }

    override fun d(tag: String, message: String, tr: Throwable?) {
        println("D/$tag: $message")
        tr?.printStackTrace()
    }
}