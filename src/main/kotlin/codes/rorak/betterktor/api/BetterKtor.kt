package codes.rorak.betterktor.api

import codes.rorak.betterktor.BetterKtor
import kotlinx.coroutines.sync.Mutex
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * The object holding important values from BetterKtor
 */
data object BetterKtor {
	/**
	 * Map of all available mutexes for usage
	 */
	val mutexMap = mutableMapOf<String, Mutex>("default" to Mutex());
	
	/**
	 * The default mutex
	 */
	val defaultMutex
		get() = mutexMap["default"]!!;
	
	val logger: Logger = LoggerFactory.getLogger(BetterKtor::class.java);
	
	internal lateinit var defaultPagesDirectory: String;
}