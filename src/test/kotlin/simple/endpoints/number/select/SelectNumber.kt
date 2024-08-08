package simple.endpoints.number.select

import codes.rorak.betterktor.annotations.BKRegexPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKRegexPath("(?<num>\\d+)")
class SelectNumber: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Number Select ${call.parameters["num"]}");
	}
}