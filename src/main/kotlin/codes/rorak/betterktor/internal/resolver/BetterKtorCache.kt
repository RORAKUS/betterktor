package codes.rorak.betterktor.internal.resolver

import codes.rorak.betterktor.BetterKtorConfig
import codes.rorak.betterktor.internal.endpoints.BaseEndpoint
import codes.rorak.betterktor.internal.other.declaringClass
import codes.rorak.betterktor.internal.other.isTopLevel
import io.ktor.server.application.*
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.reflect.jvm.javaMethod

internal class BetterKtorCache(val config: BetterKtorConfig, val application: Application) {
	val naming get() = config.naming;
	val strict get() = config.strict;
	
	val endpoints = mutableListOf<BaseEndpoint>();
	
	val cwInstances = mutableMapOf<KClass<*>, List<Any>>();
	
	// returns the error message information with currently edited class/method
	fun errorMeta(): String {
		var message = "In ";
		
		if (currentClass != null)
			message += currentClass!!.qualifiedName;
		if (currentFile != null)
			message += currentFile;
		if (currentFunction != null)
			message += " in ${currentFunction!!.name}";
		if (currentProperty != null)
			message += " resolving ${currentProperty!!.name}";
		
		return message;
	}
	
	// sets the class that is being resolved
	fun current(clazz: KClass<*>) {
		currentClass = clazz;
		currentFile = null;
		currentFunction = null;
		currentProperty = null;
	}
	
	// sets the method that is being resolved
	fun current(function: KFunction<*>) {
		currentFunction = function;
		currentProperty = null;
		if (function.isTopLevel) {
			currentFile = function.javaMethod!!.declaringClass.canonicalName.substringBeforeLast("Kt") + ".kt";
			currentClass = null;
		} else {
			currentClass = function.declaringClass;
			currentFile = null;
		}
	}
	
	// sets the property that is being resolved
	fun current(prop: KProperty<*>) {
		currentProperty = prop;
		currentFunction = null;
	}
	
	private var currentClass: KClass<*>? = null;
	private var currentFunction: KFunction<*>? = null;
	private var currentProperty: KProperty<*>? = null;
	private var currentFile: String? = null;
}