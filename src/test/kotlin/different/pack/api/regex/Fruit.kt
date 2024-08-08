package different.pack.api.regex

import codes.rorak.betterktor.annotations.BKRegexPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKRegexPath("(?<str>(mango|test)\\d+)")
class Fruit: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Regex ${call.parameters["str"]}");
	}
}