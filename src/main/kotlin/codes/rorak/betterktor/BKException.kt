package codes.rorak.betterktor

/**
 * Just an exception...
 */
class BKException: RuntimeException {
	constructor(msg: String, cause: Throwable): super(msg, cause);
	constructor(msg: String): super(msg);
}