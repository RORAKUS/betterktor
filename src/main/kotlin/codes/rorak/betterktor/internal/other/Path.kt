package codes.rorak.betterktor.internal.other

import codes.rorak.betterktor.annotations.RelativeOption
import codes.rorak.betterktor.internal.resolver.BetterKtorCache
import codes.rorak.betterktor.util.BetterKtorError
import codes.rorak.betterktor.util.CasingMethod

internal class Path(val cache: BetterKtorCache) {
	init {
		withRoot(cache.config.rootPath);
	}
	
	private val p = object {
		var root: String? = null;
		var pack: String? = null;
		var file: String? = null;
		var outerClass: String? = null;
		var innerClass: String? = null;
		var function: String? = null;
	};
	private val parts: List<String>
		get() {
			// file and class cannot be set together
			check(!(p.file != null && p.outerClass != null));
			return listOfNotNull(p.root, p.pack, p.file, p.outerClass, p.innerClass, p.function);
		}
	
	fun render(): Regex {
		// join all the parts
		var path = parts.joinToString("/");
		
		// trim the / characters
		path = path.trim('/');
		// replace multiple / in row
		path = path.replace("/{2,}".toRegex(), "/");
		// add a / at the beginning if not present
		path = "/$path";
		// add an optional / at the end
		path += "/?";
		
		return Regex(path, RegexOption.IGNORE_CASE);
	}
	
	fun withRoot(path: String): Path {
		// set the root path
		p.root = escapeAndCase(path);
		
		return this;
	}
	
	fun withPackage(name: String): Path {
		// convert package name to a valid url path
		val path = name.replace(".", "/");
		
		// set the package path
		p.pack = escapeAndCase(path);
		
		return this;
	}
	
	fun withFile(name: String): Path {
		// convert the file name and remove the suffix
		val path = name.substringBeforeLast("Kt");
		
		// set the file path
		p.file = escapeAndCase(path);
		
		return this;
	}
	
	fun withClass(path: String, relativeTo: RelativeOption?, regex: Boolean, casing: CasingMethod): Path {
		// if the outer class is set, it is an inner class
		val type = if (p.outerClass != null) ENType.INNER_CLASS else ENType.CLASS;
		with(type, path, relativeTo, regex, casing);
		return this;
	}
	
	fun withFunction(
		path: String,
		fileClass: String?,
		relativeTo: RelativeOption?,
		regex: Boolean,
		casing: CasingMethod
	): Path {
		// select the type according to the parameters
		val type = if (fileClass != null) ENType.FUNCTION else ENType.METHOD;
		
		// set the file path, it will be removed later if not used
		fileClass?.let { withFile(it); };
		
		with(type, path, relativeTo, regex, casing);
		return this;
	}
	
	private fun with(type: ENType, path: String, relative: RelativeOption?, regex: Boolean, casing: CasingMethod) {
		when (relative) {
			// when the path annotation nor the relativeTo annotation is set
			null -> {
				p.file = null;
				setTypePath(type, escapeAndCase(path, casing));
			};
			// when the default relative option is used
			RelativeOption.DEFAULT -> processDefault(type, path, regex, casing);
			// when the relative option is set by an annotation or a parameter
			else -> processRelative(relative, path, type, regex, casing);
		}
	}
	
	private fun processRelative(
		relative: RelativeOption,
		path: String,
		type: ENType,
		regex: Boolean,
		casing: CasingMethod
	) = when (relative) {
		// if it is relative to the class, just set the path
		RelativeOption.CLASS -> setTypePath(type, escapeAndCase(path, casing, regex));
		// if it is relative to the package, delete all file and class packages
		RelativeOption.PACKAGE -> {
			p.innerClass = null;
			p.outerClass = null;
			p.file = null;
			setTypePath(type, escapeAndCase(path, casing, regex));
		};
		// if it is relative to the root path, delete everything execept it
		RelativeOption.ROOT_PATH -> {
			p.innerClass = null;
			p.outerClass = null;
			p.file = null;
			p.pack = null;
			setTypePath(type, escapeAndCase(path, casing, regex));
		};
		// if it is absolute, delete all
		RelativeOption.NONE -> {
			p.innerClass = null;
			p.outerClass = null;
			p.file = null;
			p.pack = null;
			p.root = null;
			setTypePath(type, escapeAndCase(path, casing, regex));
		};
		else -> throw IllegalArgumentException();
	}
	
	private fun processDefault(type: ENType, path: String, regex: Boolean, casing: CasingMethod) {
		// the first character of the string, determines the path relativity
		val firstCharacter = path.first();
		// the edited path without the first character
		val edditedPath = path.drop(1);
		
		// depending on the first char
		when (firstCharacter) {
			// absolute path
			'/' -> processRelative(RelativeOption.NONE, edditedPath, type, regex, casing);
			// special path
			'?' -> {
				// get the relative option according to the type
				// look at RelativeOption docs
				val relativeOption = when (type) {
					ENType.METHOD -> RelativeOption.PACKAGE;
					ENType.FUNCTION -> RelativeOption.CLASS;
					else -> throw BetterKtorError(
						"You cannot use the '?' wildcart when setting a path for a class endpoint!", cache
					);
				};
				
				processRelative(relativeOption, edditedPath, type, regex, casing);
			};
			// root path
			'$' -> processRelative(RelativeOption.ROOT_PATH, edditedPath, type, regex, casing);
			// normal path
			else -> {
				p.file = null;
				setTypePath(type, escapeAndCase(path, casing, regex));
			};
		}
	}
	
	// sets the correct path element according to the type
	private fun setTypePath(type: ENType, path: String) = when (type) {
		ENType.CLASS -> p.outerClass = path;
		ENType.INNER_CLASS -> p.innerClass = path;
		ENType.FUNCTION, ENType.METHOD -> p.function = path;
	};
	
	// end node type
	private enum class ENType {
		CLASS, INNER_CLASS, FUNCTION, METHOD;
	}
	
	private fun escapeAndCase(
		path: String,
		casing: CasingMethod = cache.config.defaultCasing,
		isRegex: Boolean = false
	) =
		if (!isRegex) Regex.escape(casing(path)) else casing(path);
}