package codes.rorak.betterktor.internal

import codes.rorak.betterktor.BKConfig
import codes.rorak.betterktor.BKException
import codes.rorak.betterktor.BKHttpMethod
import codes.rorak.betterktor.annotations.*
import codes.rorak.betterktor.handlers.BKErrorHandler
import codes.rorak.betterktor.handlers.BKRoute
import codes.rorak.betterktor.handlers.BKWebsocket
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.jvm.javaMethod
import kotlin.reflect.typeOf

internal object BKProcessor {
	fun processRoutes(app: Application, config: BKConfig, routes: Set<Class<out BKRoute>>, endpointPackage: String) =
		app.routing {
			for (routeClass in routes) {
				val instance = routeClass.create();
				val path = pathOf(routeClass, config, endpointPackage);
				
				for (method in routeClass.kotlin.declaredFunctions) {
					if (method.hasAnnotation<BKIgnore>()) continue;
					checkMethod(method, listOf(typeOf<ApplicationCall>()), listOf(typeOf<ApplicationRequest>()));
					
					if (!method.isIn(BKRoute::class.declaredFunctions))
						processStandalone(method, path, config, instance);
					else registerMethod(path, method, isRegex(routeClass), instance);
				}
			}
		}
	
	fun processWebsockets(
		app: Application, config: BKConfig, routes: Set<Class<out BKWebsocket>>,
		endpointPackage: String
	) = app.routing {
		for (routeClass in routes) {
			if (isRegex(routeClass))
				throw IllegalArgumentException("Websocket class cannot be regex! (${routeClass.simpleName})");
			
			val instance = routeClass.create();
			val path = pathOf(routeClass, config, endpointPackage);
			
			for (method in routeClass.kotlin.declaredFunctions) {
				if (method.hasAnnotation<BKIgnore>()) continue;
				checkMethod(
					method,
					listOf(typeOf<DefaultWebSocketServerSession>()),
					listOf(typeOf<ApplicationCall>(), typeOf<ApplicationRequest>())
				);
				
				if (method.isIn(BKWebsocket::class.declaredFunctions)) registerWS(path, method, instance);
				else registerWS(path.addIfNeeds("/") + config.casing(method.name), method, instance);
			}
		}
	}
	
	fun processErrorHandlers(
		app: Application,
		config: BKConfig,
		routes: Set<Class<out BKErrorHandler>>,
		endpointPackage: String
	) = app.install(StatusPages) {
		val registered = mutableListOf<Triple<String, KFunction<*>, Any>>();
		
		for (routeClass in routes) {
			val path = pathOf(routeClass, config, endpointPackage, false);
			val instance = routeClass.create();
			
			for (method in routeClass.kotlin.declaredFunctions) {
				if (method.hasAnnotation<BKIgnore>()) continue;
				checkMethod(method, listOf(typeOf<ApplicationCall>(), typeOf<Throwable>()), listOf());
				
				registered +=
					if (method.isIn(BKErrorHandler::class.declaredFunctions))
						Triple(path.removeIfHasAndNotEmpty("/"), method, instance);
					else
						Triple(path.addIfNeeds("/") + config.casing(method.name), method, instance);
			}
		}
		
		for (triple in registered) {
			println("ERROR ${triple.first} -> ${triple.second.javaMethod?.declaringClass?.canonicalName}#${triple.second.name}")
		}
		
		exception { call: ApplicationCall, cause: Throwable ->
			var path = call.request.path().removeIfHasAndNotEmpty("/");
			do {
				path = path.ifEmpty { "/" };
				
				val handlers = registered
					.filter { path == it.first };
				
				if (handlers.isNotEmpty()) {
					handlers.forEach { it.second.callSuspend(it.third, call, cause); };
					return@exception;
				}
				
				path = path.substringBeforeLast("/");
			} while (path != "/");
			throw cause;
		};
	}
	
	private fun Routing.processStandalone(method: KFunction<*>, prefixPath: String, config: BKConfig, instance: Any) {
		val declaringClass = method.javaMethod?.declaringClass!!;
		val httpMethod = with(method) {
			if (hasAnnotation<BKGet>()) return@with BKHttpMethod.GET;
			if (hasAnnotation<BKPost>()) return@with BKHttpMethod.POST;
			
			findAnnotation<BKMethod>()?.method
				?: declaringClass.getAnnotation(BKDefaultMethod::class.java)?.method
				?: BKHttpMethod.POST;
		};
		val path = prefixPath.addIfNeeds("/") + config.casing(method.name);
		
		registerMethod(path, method, isRegex(declaringClass), instance, httpMethod.name);
	}
	
	private fun Routing.registerMethod(
		path: String,
		method: KFunction<*>,
		isRegex: Boolean,
		instance: Any,
		httpMethod: String = method.name
	) {
		val methodStr = HttpMethod.parse(httpMethod.uppercase());
		val exec: Route.() -> Unit = { handle { method.callSuspendIgnoreArgs(instance, call, call.request); }; }
		
		if (isRegex)
			route(
				Regex("$path/?"),
				methodStr,
				exec
			).also { println("${methodStr.value} $path/? -> ${method.javaMethod?.declaringClass?.canonicalName}#${method.name}") };
		else {
			route(
				path,
				methodStr,
				exec
			).also { println("${methodStr.value} $path -> ${method.javaMethod?.declaringClass?.canonicalName}#${method.name}") };
			route(
				path.withOrWithout("/"),
				methodStr,
				exec
			).also { println("${methodStr.value} ${path.withOrWithout("/")} -> ${method.javaMethod?.declaringClass?.canonicalName}#${method.name}") };
		}
	}
	
	private fun Routing.registerWS(
		path: String,
		method: KFunction<*>,
		instance: Any
	) {
		val exec: suspend DefaultWebSocketServerSession.() -> Unit =
			{ method.callSuspendIgnoreArgs(instance, this, call, call.request); }
		
		webSocket(path = path, handler = exec);
		webSocket(path = path.withOrWithout("/"), handler = exec);
	}
	
	fun String.plusNotEmpty(str: String) = if (isNotEmpty()) this + str else this;
	private fun String.addIfNeeds(str: String) = if (!endsWith(str)) this + str else this;
	private fun String.removeIfHasAndNotEmpty(str: String) =
		if (endsWith(str) && length != 1) substringBeforeLast(str) else this;
	
	private fun String.withOrWithout(str: String) = if (endsWith(str)) substringBeforeLast(str) else this + str;
	private fun Class<*>.create(): Any {
		constructors.forEach {
			if (it.parameterCount != 0) return@forEach;
			return it.newInstance();
		};
		throw BKException("Cannot create an instance of type ${this.name}. No constructor found!");
	}
	
	private fun pathOf(clazz: Class<*>, config: BKConfig, packageName: String, useClassName: Boolean = true): String {
		val packagePath = clazz.`package`.name.substringAfter(packageName).replace(".", "/");
		val classPath = config.rootPath + config.casing(packagePath) + "/";
		
		val annotationPath = clazz.getAnnotation(BKPath::class.java)?.path
			?: clazz.getAnnotation(BKRegexPath::class.java)?.path;
		
		if (annotationPath != null)
			return if (annotationPath.startsWith("/")) annotationPath else classPath + annotationPath;
		
		if (clazz.simpleName == "Index" || !useClassName) return classPath;
		
		return classPath + config.casing(clazz.simpleName);
	}
	
	private fun checkMethod(method: KFunction<*>, required: Collection<KType>, optional: Collection<KType>) {
		if (!method.isSuspend) throw IllegalStateException("Route method must be suspend! $method")
		if (method.returnType != typeOf<Unit>()) throw IllegalStateException("Route method must return Unit! $method")
		if (!method.parameters.map { it.type }
				.containsAll(required)) throw IllegalStateException("Route method is missing parameters! $method");
		if (!required.plus(optional)
				.containsAll(method.parameters.drop(1).map { it.type })
		) throw IllegalStateException("Route method has more parameters then it should!");
	}
	
	private fun isRegex(clazz: Class<*>): Boolean = clazz.isAnnotationPresent(BKRegexPath::class.java);
	
	private suspend fun KFunction<*>.callSuspendIgnoreArgs(vararg args: Any) =
		callSuspend(*args.copyOfRange(0, parameters.size));
	
	private fun KFunction<*>.isIn(col: Collection<KFunction<*>>) = col.any { fn ->
		name == fn.name && parameters.size == fn.parameters.size
				&& fn.parameters.map(KParameter::type).drop(1) == parameters.map(KParameter::type).drop(1);
	}
}