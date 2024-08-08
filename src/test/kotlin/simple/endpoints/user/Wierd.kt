@file:Suppress("UNUSED_PARAMETER")

package simple.endpoints.user

import codes.rorak.betterktor.annotations.BKPath
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*

@Suppress("RedundantSuspendModifier")
@BKPath("/completely/different/path")
class Wierd: BKRoute {
	override suspend fun get(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Completely Different Path");
	}
	
	suspend fun test(call: ApplicationCall, request: ApplicationRequest) {
		call.respondText("Works!");
	}
	
	suspend fun amogus(call: ApplicationCall) {
		throw Error();
	}
}