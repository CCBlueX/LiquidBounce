import fs from "fs";
import archiver from "archiver";
import {defineConfig} from "vite";
import react from "@vitejs/plugin-react-swc";
import svgr from "vite-plugin-svgr";

// https://vitejs.dev/config/

/**
 * Bundles the theme into a zip file
 */
function createBundle() {
    return {
        name: "create-bundle",
        closeBundle() {
            const output = fs.createWriteStream("resources/assets/liquidbounce/default_theme.zip");
            const archive = archiver("zip", {
                zlib: {level: 9},
            });

            output.on("close", () => {
                console.log(`${archive.pointer()} total bytes`);
                console.log("archiver has been finalized and the output file descriptor has closed.");
            });

            archive.on("error", (err: any) => {
                throw err;
            });

            archive.pipe(output);
            archive.directory("dist/", false);
            archive.finalize();
        },
    };
}

export default defineConfig({
    resolve: {
        alias: {
            "~": "/src",
        },
    },
    base: "./",
    build: {
        target: "es2015"
    },
    css: {
        modules: {
            localsConvention: "camelCaseOnly",
        },
    },

    plugins: [react(), svgr(), createBundle()],
});
