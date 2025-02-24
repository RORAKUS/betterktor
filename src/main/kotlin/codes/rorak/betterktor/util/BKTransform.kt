package codes.rorak.betterktor.util

/**
 * An object containing all possible object transformations. You can use extension methods to extend it.
 */
@Suppress("unused")
object BKTransform {
	/**
	 * Method that makes converting easier. Automatically converts every case into the "intern case".
	 *
	 * The intern case looks like this: `this§is§the§intern§casing`
	 */
	fun String.toInternCase(): String = this
		.replace("-", "§")
		.replace("_", "§")
		.replace("([a-z])([A-Z])".toRegex(), "$1§$2")
		.lowercase();
	
	/**
	 * Converts a string into `camelCase`
	 */
	fun camelCase(s: String): String = "§([a-z])".toRegex().replace(s.toInternCase()) { it.groupValues[1].uppercase() };
	
	/**
	 * Converts a string into `snake_case`
	 */
	fun snakeCase(s: String): String = s.toInternCase().replace("§", "_");
	
	/**
	 * Converts a string into `kebab-case`
	 */
	fun kebabCase(s: String): String = s.toInternCase().replace("§", "-");
	
	/**
	 * Converts a string into `PascalCase`
	 */
	fun pascalCase(s: String): String = camelCase(s).replaceFirstChar { it.uppercase() };
	
	/**
	 * Converts a string into `Train-Case`
	 */
	fun trainCase(s: String): String = pascalCase(s).replace("([a-z])([A-Z])".toRegex(), "$1-$2");
}