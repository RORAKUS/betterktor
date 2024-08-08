package simple.endpoints.number.select

import codes.rorak.betterktor.annotations.BKRegexPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKRegexPath("/and/(?<str>[a-z]+-and-[a-z]+)")
class AbsoluteSelect: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("And ${call.parameters["str"]}");
	}
}