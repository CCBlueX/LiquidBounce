const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

const success = (message) => console.log('\x1b[32m%s\x1b[0m', message); // Green color for success
const log = (message) => console.log('\x1b[36m%s\x1b[0m', message); // Cyan color for logs
const error = (message) => console.log('\x1b[31m%s\x1b[0m', message); // Red color for errors

let archiver;

try {
    archiver = require('archiver');
} catch (error) {
    error('Error: The "archiver" package is not installed. Please run the following command to install it:');
    error('-> npm install archiver\n');
    process.exit(1); // Exit with code 1 to indicate an error
}

function cleanUp() {
    log("=> Cleaning up\n====================\n")

    // Copy theme bundle to jar structure
    if (fs.existsSync("resources")) {

        fs.rmSync("resources", { recursive: true });
        log("Deleted resources folder");
    }

    log("\n====================\n");
}

function build() {
    log("\n=> Building theme\n====================\n");
    try {
        execSync("npm i && npm run build", { encoding: 'utf-8', stdio: 'inherit' });
    } catch (e) {
        error(e);
        process.exit(1); // Exit with code 1 to indicate an error
    }

    success("====================\nSuccessfully built theme\n");
    return true;
}

function createBundle() {
    log("\n=> Packing up theme\n====================\n");
    fs.mkdirSync("resources/assets/liquidbounce", { recursive: true });

    // Create a zip archive
    const output = fs.createWriteStream('resources/assets/liquidbounce/default_theme.zip');
    const archive = archiver('zip', { });

    output.on('close', () => {
        success("Successfully created theme bundle");
        log("\n=> Done\n====================\n");
    });

    archive.on('error', (e) => {
        error(e);
    });

    archive.pipe(output);

    log("=> Creating theme bundle\n====================\n")
    if (fs.existsSync("./public")) {
        // Add contents of the public folder to the zip archive
        archive.directory("./public", ".");
        log(`Added public folder to theme bundle`);
    } else {
        error(`Theme does not contain a public folder`);
    }

    archive.finalize();
}

cleanUp();
if (build()) {
    createBundle();
}
