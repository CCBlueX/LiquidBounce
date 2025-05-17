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
 * using the syntax: require('$jvm/java/lang/String').
 * As of now, this script is embedded in the context and is not optional.
 * 
 */

(function setupJavaRequire() {
    // Store the original require
    const originalRequire = globalThis.require;

    // Cache for loaded Java types
    const javaTypeCache = new Map();

    // Replace require with our version
    globalThis.require = function jvmRequire(modulePath) {
        // Check if this path starts with $jvm
        const prefix = 'jvm-types'
        if (modulePath.startsWith(prefix)) {
            // Extract the actual Java type path by removing the $jvm prefix
            const javaTypePath = modulePath.substring(prefix.length).replaceAll("/", "."); // Remove '$jvm/'

            try {

                // Use the cache if available
                const javaType = javaTypeCache.has(javaTypePath) ?
                    javaTypeCache.get(javaTypePath) :
                    function () {
                        const type = Java.type(javaTypePath);
                        javaTypeCache.set(javaTypePath, type);
                        return type;
                    }();

                return {
                    [modulePath.substring(modulePath.lastIndexOf("/") + 1)]: javaType
                }
            } catch (e) {
                console.error(`Failed to load Java type for: ${javaTypePath}`, e);
                throw new Error(`Cannot load Java type: ${javaTypePath} - ${e.message}`);
            }
        }

        // For non-Java modules, use the original require
        return originalRequire(modulePath);
    };
})();
