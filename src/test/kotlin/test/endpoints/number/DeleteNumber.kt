package test.endpoints.number

import codes.rorak.betterktor.annotations.BKRegexPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKRegexPath("delete/(?<num>\\d+)")
class DeleteNumber: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Number Delete ${call.parameters["num"]}");
	}
}