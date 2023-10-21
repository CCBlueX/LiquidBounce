import {defineConfig} from "vite";
import react from "@vitejs/plugin-react-swc";
import svgr from "vite-plugin-svgr";

// https://vitejs.dev/config/
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

    plugins: [react(), svgr()],
});
