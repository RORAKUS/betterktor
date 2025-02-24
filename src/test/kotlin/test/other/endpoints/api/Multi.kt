@file:Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")

package test.other.endpoints.api

import codes.rorak.betterktor.annotations.BKMulti
import codes.rorak.betterktor.annotations.BKMultiFor
import codes.rorak.betterktor.handlers.BKErrorHandler
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import codes.rorak.betterktor.util.BKRouteType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*

@BKMulti
class Multi: BKRoute, BKWebsocket, BKErrorHandler {
	override suspend fun get(call: ApplicationCall) {
		call.respond("GET /multi");
	}
	
	override suspend fun handle(session: DefaultWebSocketServerSession, call: ApplicationCall) {
		session.send("WS /multi");
	}
	
	override suspend fun onError(call: ApplicationCall, cause: Throwable) {
		call.respond("ERROR /multi");
	}
	
	suspend fun test(call: ApplicationCall) {
		call.respond("POST /multi/test");
	}
	
	suspend fun err(call: ApplicationCall) {
		throw Error();
	}
	
	suspend fun genericErr(call: ApplicationCall) {
		throw Error();
	}
	
	@BKMultiFor(BKRouteType.ERROR_HANDLER)
	suspend fun err(call: ApplicationCall, cause: Throwable) {
		call.respondText("ERROR /multi/err");
	}
	
	@BKMultiFor(BKRouteType.WEBSOCKET)
	suspend fun chat(session: DefaultWebSocketServerSession) {
		session.send("WS /multi/chat");
		session.close();
	}
}