const fs = require("fs");
const archiver = require("archiver");

const success = (message) => console.log("\x1b[32m%s\x1b[0m", message); // Green color for success
const log = (message) => console.log("\x1b[36m%s\x1b[0m", message); // Cyan color for logs
const error = (message) => console.log("\x1b[31m%s\x1b[0m", message); // Red color for errors

if (fs.existsSync("resources")) {
    fs.rmSync("resources", { recursive: true });
    log("Deleted resources folder");
}

fs.mkdirSync("resources/resources/liquidbounce/themes", { recursive: true });

// Create a zip archive
const output = fs.createWriteStream("resources/resources/liquidbounce/themes/liquidbounce.zip");
const archive = archiver("zip", {});

output.on("close", () => {
    success("Successfully created theme bundle");
    log("\n=> Done\n====================\n");
});

archive.on("error", (e) => {
    error(e);
});

archive.pipe(output);

log("=> Creating theme bundle\n====================\n");
if (fs.existsSync("./dist")) {
    // Add contents of the public folder to the zip archive
    archive.directory("./dist", ".");
    log(`Added public folder to theme bundle`);
} else {
    error(`Theme does not contain a public folder`);
}

archive.finalize();
