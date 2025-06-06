// jvm-require.js
/**
 * NOTE FOR DEBUGGERS:
 *
 * If you're debugging with GraalJS and expected to pause at the first line of YOUR code
 * but instead paused here - this is normal! This script runs first to set up Jvm integration.
 *
 * To get to your actual code:
 * - Press the "Step" (or "Step Over") button (F10) in your debugger 2+ times
 * - This will complete the execution of this setup script
 * - The debugger will then continue to the first line of your script
 *
 * This script creates a custom 'require' function that allows importing Java classes
 * using the syntax: require('jvm-types/java/lang/String').
 * As of now, this script is embedded in the context and is not optional.
 *
 */

(function setupJavaRequire() {
    // Store the original require
    const originalRequire = globalThis.require;

    // Replace require with our version
    globalThis.require = function jvmRequire(modulePath) {
        // Check if this path starts with jvm-types/
        const prefix = 'jvm-types/'
        if (modulePath.startsWith(prefix)) {
            // Extract the actual Java type path by removing the jvm-types/ prefix
            const javaTypePath = modulePath.substring(prefix.length).replaceAll("/", ".");

            try {
                return {
                    [modulePath.substring(modulePath.lastIndexOf("/") + 1)]: Java.type(javaTypePath)
                }
            } catch (e) {
                // Enhanced error reporting with more context
                // Enhanced error reporting with more context
                const errorMessage = `Failed to load Java type '${javaTypePath}' from module path '${modulePath}'`;
                const suggestions = [];

                if (javaTypePath.includes("..") || javaTypePath.startsWith(".") || javaTypePath.endsWith(".")) {
                    suggestions.push("Check for invalid dots in class path");
                }


                const originalPath = modulePath.substring(prefix.length);
                if (originalPath.toLowerCase() !== originalPath && originalPath.includes("/")) {
                    suggestions.push("Ensure proper package/class name casing");
                }

                if (!javaTypePath.includes(".")) {
                    suggestions.push("Missing package name? Try 'java.lang." + javaTypePath + "'");
                }


                const fullError = suggestions.length > 0
                    ? `${errorMessage}. Suggestions: ${suggestions.join("; ")}. Original error: ${e.message}`
                    : `${errorMessage}. Original error: ${e.message}`;

                Client.displayChatMessage(fullError);
                Client.displayChatMessage("Java exception details:", e);
                throw new Error(fullError);
            }
        }

        // For non-Java modules, use the original require
        return originalRequire(modulePath);
    };
})();
