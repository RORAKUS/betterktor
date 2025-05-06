package codes.rorak.betterktor.internal.other

import codes.rorak.betterktor.annotations.ComplexWebsocket
import codes.rorak.betterktor.annotations.Endpoint
import codes.rorak.betterktor.annotations.ErrorHandler
import codes.rorak.betterktor.annotations.SSE
import codes.rorak.betterktor.annotations.Websocket
import codes.rorak.betterktor.api.BetterKtorEndpoint
import io.github.classgraph.ClassGraph
import io.github.classgraph.ScanResult
import java.lang.reflect.Method
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.jvm.kotlinFunction

internal class ClassLoader(val loadedPackages: List<String>) {
	private val classes: MutableSet<KClass<*>> = mutableSetOf();
	private val functions: MutableSet<KFunction<*>> = mutableSetOf();
	
	fun load(strict: Boolean) {
		// scan the packages
		val scan = ClassGraph()
			.acceptPackages(*loadedPackages.toTypedArray())
			.enableAnnotationInfo()
			.enableClassInfo()
			.scan();
		
		// load the classes. If strict mode is on, load only annotated and implementing classes
		val loadedClasses = if (strict) strictClasses(scan) else scan.allClasses.loadClasses().map(Class<*>::kotlin);
		
		// for every class check and add it to a correct list
		loadedClasses.forEach { clazz ->
			// skip interfaces and annotations
			if (clazz.java.isInterface || clazz.java.isAnnotation) return@forEach;
			// if the class is normal, just add it
			if (!clazz.isFileClass) {
				classes += clazz;
				return@forEach;
			}
			
			// if it is a special class, add all its methods
			functions.addAll(clazz.java.declaredMethods.mapNotNull(Method::kotlinFunction));
		};
	}
	
	private fun strictClasses(scan: ScanResult): List<KClass<*>> {
		// all classes that implement the interface
		val interfaceClasses = scan.getClassesImplementing(BetterKtorEndpoint::class.java).loadClasses();
		
		// all annotated classes
		val annotatedClasses = scan.getClassesWithAnyAnnotation(
			Endpoint::class.java, Websocket::class.java, SSE::class.java,
			ErrorHandler::class.java, ComplexWebsocket::class.java
		).loadClasses();
		
		// all classes that can be file classes and filter file classes
		val fileClasses = scan.allClasses
			.filter { it.name.endsWith("Kt") }
			.loadClasses()
			.filter { it.kotlin.isFileClass };
		
		// return all these classes and map them
		return (interfaceClasses + annotatedClasses + fileClasses).map(Class<*>::kotlin);
	}
	
	// checks if the class is just a file class using the metadata annotation
	private val KClass<*>.isFileClass get() = findAnnotation<Metadata>()?.kind == 2;
	
	fun classes() = classes.toList();
	fun functions() = functions.toList();
}