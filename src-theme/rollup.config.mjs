import svelte from "rollup-plugin-svelte";
import commonjs from "@rollup/plugin-commonjs";
import resolve from "@rollup/plugin-node-resolve";
import livereload from "rollup-plugin-livereload";
import terser from "@rollup/plugin-terser";
import css from "rollup-plugin-css-only";
import preprocess from "svelte-preprocess";
import serve from "serve-handler"
import http from "http"

const production = !process.env.ROLLUP_WATCH;

// Live reload plugin is broken with multiple rollup configs, so we need to close the server manually
if (global.PLUGIN_LIVERELOAD?.server) {
    console.log("Closing old live reload server");
    global.PLUGIN_LIVERELOAD.server.close();
    global.PLUGIN_LIVERELOAD.server = null;
} 

const port = 10004;
let server = null;
let retryInterval = null; 
if (!production) {
    server = http.createServer(serve);
    server.on("error", (err) => {
        // If port is in use
        if (err.code === "EADDRINUSE") {
            console.log(`Port ${port} is in use, retrying in 1 second`);
            if (!retryInterval) retryInterval = setInterval(() => {
                server.listen(port);
            }, 1000);
        }
    });
    server.on("listening", () => {
        if (retryInterval) {
            clearInterval(retryInterval);
            retryInterval = null;
        }
    });
    server.listen(port);
    console.log(`Serving files on http://localhost:${port}`);
}

function closeWatcher() {
    if (retryInterval) {
        clearInterval(retryInterval);
        retryInterval = null;
    }
    if (server) {
        console.log("Closing live reload server");
        return new Promise((resolve) => {
            server.close(resolve);
            server = null;
        });
    }
} 

function constructConfig(partname) {
    return {
        input: `${partname}/src/main.js`,
        output: {
            sourcemap: true,
            format: "iife",
            name: "app",
            file: `${partname}/public/build/bundle.js`
        },
        plugins: [
            svelte({
                // Disable svelte accessibility warnings
                onwarn: (warning, handler) => {
                    if (warning.code.startsWith('a11y-')) {
                        return;
                    }
                    handler(warning);
                },
                compilerOptions: {
                    // enable run-time checks when not in production
                    dev: !production,
                },
                preprocess: preprocess()
            }),
            css({ output: "bundle.css" }),
            resolve({
                browser: true,
                dedupe: ["svelte"]
            }),
            commonjs(),

            // Serve files via http, used in `.client ultralight show <url>`
            // TODO: properly integrate gui dev reloading instead of using a command
            // !production && serve({
            //     contentBase: `${partname}/public`,
            //     port: port++,
            // }),

            // Watch the `public` directory and refresh the
            // browser on changes when not in production
            !production && livereload(`${partname}/public`),
            // Close live reload server when done
            { closeWatcher },

            // If we"re building for production (npm run build
            // instead of npm run dev), minify
            production && terser()
        ],
        watch: {
            clearScreen: false
        }
    }
}

// All GUI Components that use svelte should be listed here
export default [
    constructConfig("hud"),
    constructConfig("clickgui"),
    constructConfig("title"),
    constructConfig("customize")
]
