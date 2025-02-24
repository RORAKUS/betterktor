package codes.rorak.betterktor.handlers

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.websocket.*

/**
 * An interface for all websockets. You must implement this when making a websocket class.
 * Contains a single handle function in 3 overloads, depending on your parameter needs.
 * Don't worry, you can get `call` by `session.call` and request by `session.call.request`.
 * You have more options only because it's more comfortable.
 */
interface BKWebsocket {
	suspend fun handle(session: DefaultWebSocketServerSession, call: ApplicationCall, request: ApplicationRequest) {};
	suspend fun handle(session: DefaultWebSocketServerSession) {};
	suspend fun handle(session: DefaultWebSocketServerSession, call: ApplicationCall) {};
}