package test.endpoints

import codes.rorak.betterktor.annotations.BKMulti
import codes.rorak.betterktor.annotations.BKMultiFor
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import codes.rorak.betterktor.util.BKRouteType
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.isActive

@BKMulti
class Multi: BKRoute, BKWebsocket {
	override suspend fun get(call: ApplicationCall) {
		call.respondText("Multi GET");
	}
	
	override suspend fun handle(session: DefaultWebSocketServerSession) {
		session.send(Frame.Text("/multi WEBSOCKET"));
		
		val name = session.incoming.receive() as Frame.Text;
		session.send(Frame.Text("/multi ${name.readText().uppercase()}"));
		
		session.close();
	}
	
	@BKMultiFor(BKRouteType.WEBSOCKET)
	suspend fun chat(session: DefaultWebSocketServerSession) {
		while (session.isActive) {
			val message = session.incoming.receive() as Frame.Text;
			session.send(message);
		}
	}
}