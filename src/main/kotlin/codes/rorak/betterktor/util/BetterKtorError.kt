package codes.rorak.betterktor.util

import codes.rorak.betterktor.internal.resolver.BetterKtorCache

class BetterKtorError internal constructor(msg: String = "", cache: BetterKtorCache):
	Error("There was an error during BetterKtor endpoint resolving: ${cache.errorMeta()}: $msg");