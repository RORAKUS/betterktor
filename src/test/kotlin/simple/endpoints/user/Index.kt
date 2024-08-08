package simple.endpoints.user

import codes.rorak.betterktor.BKHttpMethod
import codes.rorak.betterktor.annotations.BKDefaultMethod
import codes.rorak.betterktor.annotations.BKGet
import codes.rorak.betterktor.annotations.BKMethod
import codes.rorak.betterktor.handlers.BKRoute
import io.ktor.server.application.*
import io.ktor.server.response.*

@BKDefaultMethod(BKHttpMethod.DELETE)
class Index: BKRoute {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("User Index")
	}
	
	@BKGet
	suspend fun newUser(call: ApplicationCall) {
		call.respondText("New user")
	}
	
	@BKMethod(BKHttpMethod.POST)
	suspend fun updateUser(call: ApplicationCall) {
		call.respondText("Updated user")
	}
	
	suspend fun deleteUser(call: ApplicationCall) {
		call.respondText("User deleted")
	}
}