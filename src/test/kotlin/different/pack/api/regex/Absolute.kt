package different.pack.api.regex

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("/regex/", "(?<str>(mango|test)\\d+)")
class Absolute: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Regex ${call.parameters["str"]}");
	}
}