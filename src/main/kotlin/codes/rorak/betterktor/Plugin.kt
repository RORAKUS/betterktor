package codes.rorak.betterktor

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*
import org.reflections.Reflections
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.declaredFunctions

/**
 * The main plugin class. Use this in the `install()` method
 */
val BKPlugin = createApplicationPlugin("BetterKtor", ::BKConfig) {
	val config = pluginConfig;
	
	val _package = "${config.basePackage ?: getBasePackage()}.${config.packageName}"
		.replaceFirstChar { if (it == '.') "" else "$it" };
	
	val reflections = Reflections(_package);
	val classes = reflections.getSubTypesOf(BKRoute::class.java);
	
	application.routing {
		classes.forEach { clazz ->
			val paths = fetchPaths(clazz, config, _package);
			
			val instance = clazz.create();
			
			clazz.kotlin.declaredFunctions.forEach methods@{ method ->
				if (method.name !in BKRoute::class.declaredFunctions.map { it.name }) return@methods;
				
				paths.first.forEach { path ->
					if (paths.second)
						route(Regex(path), HttpMethod.parse(method.name.uppercase())) {
							handle { method.callSuspend(instance, call, call.request); }
						};
					else
						route(path, HttpMethod.parse(method.name.uppercase())) {
							handle { method.callSuspend(instance, call, call.request); }
						};
				};
			};
		};
	};
};

private fun getBasePackage(): String {
	Thread.currentThread().stackTrace.forEach {
		if (it.className.contains("^(${{}::class.java.`package`.name}|java\\.lang|io\\.ktor)".toRegex())) return@forEach;
		
		return it.className.substringBeforeLast(".", "");
	};
	return "";
}

private fun fetchPaths(clazz: Class<out BKRoute>, config: BKConfig, basePackage: String): Pair<Array<String>, Boolean> {
	val prefixPath = config.rootPath + config.casing(
		clazz.`package`.name.substringAfter(basePackage).replace(".", "/")
	) + "/";
	val annotation = clazz.getAnnotation(BKPath::class.java);
	
	if (annotation != null) {
		val isRegex = annotation.regex.isNotEmpty();
		
		val annotationPath =
			if (isRegex)
				(annotation.path.let { if (it.isNotEmpty()) "$it/" else it } + annotation.regex)
					.replace(Regex("///?"), "/");
			else annotation.path;
		
		return (if (annotation.path.startsWith("/")) annotationPath else prefixPath + annotationPath).alsoEndWithSlash() to isRegex;
	}
	
	return (prefixPath + config.casing(clazz.simpleName)).let {
		if (clazz.simpleName.lowercase() == "index") it.substringBeforeLast(
			"/index"
		).ifBlank { "/" } else it
	}.alsoEndWithSlash() to false;
}

private fun String.alsoEndWithSlash(): Array<String> {
	return arrayOf(
		this,
		if (this.endsWith("/")) this.substringBeforeLast("/") else "$this/"
	);
}

private fun Class<*>.create(): Any {
	constructors.forEach {
		if (it.parameterCount != 0) return@forEach;
		return it.newInstance();
	};
	throw BKException("Cannot create an instance of type ${this.name}. No constructor found!");
}