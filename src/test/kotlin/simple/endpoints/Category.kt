package simple.endpoints

import codes.rorak.betterktor.BKPath
import codes.rorak.betterktor.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@BKPath("category/{name}")
class Category: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Category ${call.parameters["name"]?.replaceFirstChar { it.uppercase() }}");
	}
}