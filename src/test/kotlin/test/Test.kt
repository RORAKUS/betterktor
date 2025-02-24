package test

import codes.rorak.betterktor.BKPlugin
import codes.rorak.betterktor.util.BKTransform
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.testing.*
import io.ktor.websocket.*
import kotlin.test.Test
import kotlin.test.assertEquals

class Test {
	companion object {
		const val TOKEN = "token";
	}
	
	@Test
	fun `Basic Test`() = testApplication {
		application {
			install(BKPlugin) {
				configureWebsockets {}
			};
		};
		val client = createClient {
			install(WebSockets);
		};
		
		assertGet("/", "Index");
		assertGet("", "Index");
		
		assertPost("/just-post", "Just Post");
		assertPost("/just-post/", "Just Post");
		
		assertGet("/test", "Test");
		assertPost("/test", "Test Post");
		assertGet("/user/get", "User Get");
		assertGet("/user", "User Index");
		assertGet("/user/when/not/done", "User When Not Done");
		
		assertGet("/completely/different/path/", "Completely Different Path");
		assertGet("/user/relative/path", "User Relative Path");
		
		assertGet("/category/example", "Category Example");
		assertGet("/category/test123", "Category Test123");
		
		assertGet("/this-should-be-just-kebab-case", "This Should Be Just Kebab Case");
		assertGet("/number/select/158", "Number Select 158");
		assertError("/number/select/test");
		
		assertGet("/and/this-and-that", "And this-and-that");
		assertGet("/number/delete/54", "Number Delete 54");
		
		assertGet("/user/new-user", "New user");
		assertPost("/user/update-user", "Updated user");
		assertRequest("/user/delete-user", HttpMethod.Delete, "User deleted");
		
		assertPost("/completely/different/path/test", "Works!");
		
		assertPost("/user/when/not/done", "Error caught in user!");
		assertGet("/number/numb", "Error caught in number");
		assertPost("/completely/different/path/example", "Error caught in /!");
		assertRequest("/test", HttpMethod.Put, "Error caught in /test!");
		
		assertGet("/multi", "Multi GET");
		
		client.webSocket("/multi") {
			val initialMessage = incoming.receive() as Frame.Text;
			assertEquals("/multi WEBSOCKET", initialMessage.readText());
			
			send(Frame.Text("Hello"));
			val secondMessage = incoming.receive() as Frame.Text;
			assertEquals("/multi HELLO", secondMessage.readText());
		};
		
		client.webSocket("/multi/chat") {
			send(Frame.Text("Hello"));
			val first = incoming.receive() as Frame.Text;
			assertEquals("Hello", first.readText());
			
			send(Frame.Text("Hi"));
			val second = incoming.receive() as Frame.Text;
			assertEquals("Hi", second.readText());
			
			close();
		};
	};
	
	@Test
	fun `Different Package Test`() = testApplication {
		application {
			install(BKPlugin) {
				basePackage = "test.other.endpoints";
				endpointsPackage = "api";
				casing = BKTransform::snakeCase;
				rootPath = "/api";
				configureWebsockets { };
				configureAuthentication {
					basic("auth") {
						validate { (username, password) ->
							if (username == TOKEN && password == TOKEN)
								UserIdPrincipal("auth")
							else null;
						};
					};
				};
			};
		}
		
		assertGet("/api/test", "Api Test");
		assertGet("/api/this_should_be_snake_case", "This Should Be Snake Case");
		assertGet("/api/inner/test", "Api Inner Test");
		assertGet("/completely/different", "Completely Different");
		
		assertGet("/api/relative", "Api Relative");
		assertGet("/api", "Api");
		assertGet("/api/", "Api");
		
		assertGet("/regex/test154", "Regex test154");
		assertGet("/api/regex/mango154", "Regex mango154");
		assertError("/api/regex/apple154");
		
		assertGet("/api/multi/", "GET /multi");
		assertPost("/api/multi/test", "POST /multi/test");
		assertPost("/api/multi/err", "ERROR /multi/err");
		assertPost("/api/multi/generic_err", "ERROR /multi");
		assertError("/api/multi/chat", status = HttpStatusCode.BadRequest);
		assertError("/api/multi/on-error");
		
		val auth: HttpRequestBuilder.() -> Unit = {
			basicAuth(TOKEN, TOKEN);
		};
		
		assertGet("/api/auth", "auth", auth);
		assertError("/api/auth", status = HttpStatusCode.Unauthorized);
		assertGet("/api/auth/one", "auth", auth);
		assertGet("/api/auth/one", "null");
	};
	
	
	private suspend fun ApplicationTestBuilder.assertGet(
		path: String,
		text: String,
		builder: HttpRequestBuilder.() -> Unit = {}
	) =
		assertRequest(path, HttpMethod.Get, text, builder);
	
	private suspend fun ApplicationTestBuilder.assertPost(
		path: String,
		text: String,
		builder: HttpRequestBuilder.() -> Unit = {}
	) =
		assertRequest(path, HttpMethod.Post, text, builder);
	
	private suspend fun ApplicationTestBuilder.assertRequest(
		path: String,
		method: HttpMethod,
		text: String,
		builder: HttpRequestBuilder.() -> Unit = {}
	) {
		println("Doing: $method $path -> should be '$text'");
		val response = client.request(path) { this.method = method; builder(); };
		assertEquals(HttpStatusCode.OK, response.status);
		assertEquals(text, response.bodyAsText());
	}
	
	private suspend fun ApplicationTestBuilder.assertError(
		path: String,
		method: HttpMethod = HttpMethod.Get,
		status: HttpStatusCode = HttpStatusCode.NotFound
	) {
		val response = client.request(path) { this.method = method; };
		assertEquals(status, response.status);
	}
}