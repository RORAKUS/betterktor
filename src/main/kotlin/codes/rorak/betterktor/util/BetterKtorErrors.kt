package codes.rorak.betterktor.util

import codes.rorak.betterktor.internal.resolver.BetterKtorCache

class BetterKtorError internal constructor(msg: String = "", cache: BetterKtorCache):
	Error("There was an error during BetterKtor endpoint resolving: ${cache.errorMeta()}: $msg");

class BetterKtorRuntimeError internal constructor(msg: String):
	RuntimeException("There was a runtime BetterKtor error: $msg");