// jvm-require.js
/**
 * NOTE FOR WHO ARE USING DEBUGGER:
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
                const errorMsg = `Cannot load Java type with require(${modulePath}): ${javaTypePath}`;
                Client.displayChatMessage(errorMsg);
                Client.displayChatMessage(`${e}`)
                throw new Error(errorMsg);
            }
        }

        // For non-Java modules, use the original require 
        return originalRequire(modulePath);
    };
})();
