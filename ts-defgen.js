/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

"use strict";
Object.defineProperty(exports, "__esModule", { value: true });
const URLClassLoader_1 = require("@ccbluex/liquidbounce-script-api/java/net/URLClassLoader");
const File_1 = require("@ccbluex/liquidbounce-script-api/java/io/File");
const Thread_1 = require("@ccbluex/liquidbounce-script-api/java/lang/Thread");
const Paths_1 = require("@ccbluex/liquidbounce-script-api/java/nio/file/Paths");
// @ts-expect-error
const HashMap_1 = require("@ccbluex/liquidbounce-script-api/java/util/HashMap");
// @ts-expect-error
const ArrayList_1 = require("@ccbluex/liquidbounce-script-api/java/util/ArrayList");
const JvmClassMappingKt_1 = require("@ccbluex/liquidbounce-script-api/kotlin/jvm/JvmClassMappingKt");
const Class_1 = require("@ccbluex/liquidbounce-script-api/java/lang/Class");
const ScriptModule_1 = require("@ccbluex/liquidbounce-script-api/net/ccbluex/liquidbounce/script/bindings/features/ScriptModule");
const ClassPath_1 = require("@ccbluex/liquidbounce-script-api/com/google/common/reflect/ClassPath");
const ScriptManager_1 = require("@ccbluex/liquidbounce-script-api/net/ccbluex/liquidbounce/script/ScriptManager");
const LiquidBounce_1 = require("@ccbluex/liquidbounce-script-api/net/ccbluex/liquidbounce/LiquidBounce");
const LocalDate_1 = require("@ccbluex/liquidbounce-script-api/java/time/LocalDate");
const DateTimeFormatter_1 = require("@ccbluex/liquidbounce-script-api/java/time/format/DateTimeFormatter");
const inDev = LiquidBounce_1.LiquidBounce.IN_DEVELOPMENT;
// @ts-expect-error
const globalEntries = Object.entries(globalThis);
// Function to create a URLClassLoader from a JAR path
function createClassLoaderFromJar(jarPath) {
    try {
        // Create File object for the JAR
        const jarFile = new File_1.File(jarPath);
        // Convert File to URL
        const jarUrl = jarFile.toURI().toURL();
        // Create URLClassLoader with the system class loader as parent
        return new URLClassLoader_1.URLClassLoader([jarUrl], Thread_1.Thread.currentThread().getContextClassLoader());
    }
    catch (e) {
        console.error("Error creating ClassLoader:", e);
        throw e;
    }
}
// Function to load a class from a given ClassLoader
function loadClassFromJar(classLoader, className) {
    try {
        return classLoader.loadClass(className);
    }
    catch (e) {
        console.error(`Error loading class ${className}:`, e);
        throw e;
    }
}
// @ts-expect-error
function findAllClassInfos() {
    // @ts-expect-error
    return Java.from(ClassPath_1.ClassPath.from(Thread_1.Thread.currentThread()
        .getContextClassLoader())
        .getTopLevelClasses()
        // @ts-expect-error
        .asList());
}
function getName(javaClass) {
    const fullName = javaClass.name;
    return fullName.substring(fullName.lastIndexOf(".") + 1);
}
const script = registerScript.apply({
    name: "ts-defgen",
    version: "1.0.0",
    authors: ["commandblock2", "CCBlueX"],
});
function generate(path, packageName) {
    try {
        const loader = createClassLoaderFromJar(path + "/ts-generator.jar");
        const NPMGen = loadClassFromJar(loader, "me.commandblock2.tsGenerator.NPMPackageGenerator");
        const TsGen = loadClassFromJar(loader, "me.ntrrgc.tsGenerator.TypeScriptGenerator");
        const VoidType = loadClassFromJar(loader, "me.ntrrgc.tsGenerator.VoidType");
        const NULL = VoidType.getEnumConstants()[0];
        const javaClasses = globalEntries
            .filter((entry) => entry[1] != undefined)
            .map((entry) => (entry[1] instanceof Class_1.Class ? entry[1] : entry[1].class))
            .filter((entry) => entry != undefined);
        const eventEntries = ReflectionUtil.getDeclaredField(ScriptModule_1.ScriptModule, "LOWERCASE_NAME_EVENT_MAP").entrySet().toArray();
        Client.displayChatMessage("looking for all jvm classes");
        const allClassInfos = findAllClassInfos();
        Client.displayChatMessage(`found ${allClassInfos.length} classes, converting to kotlin classes`);
        const classNames = ["java.net.URLClassLoader",
            "java.nio.file.Paths",
            "java.util.HashMap",
            "java.util.ArrayList",
            "java.util.jar.JarInputStream",
            "java.util.Map",
            "com.google.common.reflect.ClassPath",
            "kotlin.jvm.JvmClassMappingKt"
        ]
            .concat(allClassInfos.map((entry) => {
                try {
                    return entry.getName();
                }
                catch (e) {
                    return null;
                }
            }));
        const jvmClasses = classNames
            .map((entry) => {
                try {
                    return ReflectionUtil.classByName(entry);
                }
                catch (e) {
                    return null;
                }
            })
            .filter((entry) => entry != undefined);
        const jvmClassesInKotlin = jvmClasses
            .map((entry) => {
                try {
                    return JvmClassMappingKt_1.JvmClassMappingKt.getKotlinClass(entry);
                }
                catch (e) {
                    return null;
                }
            })
            .filter((entry) => entry != null);
        Client.displayChatMessage(`converted to ${jvmClassesInKotlin.length} kotlin classes`);
        const kotlinClasses = javaClasses
            .concat([
                // Using the imported class from @embedded
                ReflectionUtil.classByName("net.ccbluex.liquidbounce.script.bindings.features.ScriptModule")
            ])
            .concat(eventEntries.map((entry) => entry[1]))
            .map(entry => {
                try {
                    return JvmClassMappingKt_1.JvmClassMappingKt.getKotlinClass(entry);
                }
                catch (e) {
                    return null;
                }
            })
            .filter((entry) => entry != undefined)
            .concat(jvmClassesInKotlin);
        const classes = new ArrayList_1.ArrayList(kotlinClasses);
        Client.displayChatMessage(`generating types for ${classes.length} classes`);
        Client.displayChatMessage("this may take a while, please wait...");
        // @ts-expect-error
        const generated = new TsGen(classes, new HashMap_1.HashMap(), new ArrayList_1.ArrayList(), new ArrayList_1.ArrayList(), "number", NULL);
        const today = LocalDate_1.LocalDate.now();
        const formatter = DateTimeFormatter_1.DateTimeFormatter.ofPattern('y.M.d');
        Client.displayChatMessage("writing types");
        // @ts-expect-error
        const npmPack = new NPMGen(generated, packageName, `${inDev ? today.format(formatter) : LiquidBounce_1.LiquidBounce.INSTANCE.clientVersion}+${LiquidBounce_1.LiquidBounce.INSTANCE.clientBranch}.${LiquidBounce_1.LiquidBounce.INSTANCE.clientCommit}`,
            // extraFiles - add the ambient and augmentations files
            `"augmentations/**/*.d.ts", "ambient/ambient.d.ts"`,
            // extraTypesVersion - add the augmentations and ambient paths
            `"./augmentations/*", "ambient/ambient.d.ts"`,
            // otherExtras - add the types field
            `"types": "ambient/ambient.d.ts"`, null);
        npmPack.writePackageTo(
            // @ts-expect-error
            Paths_1.Paths.get(path));
        Client.displayChatMessage("print embedded script types, see log for more info, those are for maintainace use");
        const embeddedDefinition = `
// ambient.ts
// imports
import "../augmentations/index.d.ts"
${javaClasses
            .map((clazz) => {
                return `import { ${getName(clazz)} as ${getName(clazz)}_ } from "../types/${clazz.name.replaceAll(".", "/")}";`;
            })
            .join("\n")}
declare global {


// exports
${globalEntries
            .filter((entry) => entry[1] != undefined)
            .filter((entry) => !(entry[1] instanceof Class_1.Class))
            .filter((entry) => entry[1].class != undefined)
            .map((entry) => `    export const ${entry[0]}: ${getName(entry[1].class)}_;`)
            .join("\n\n")}

${javaClasses
            .map((clazz) => {
                var _a, _b;
                // Check if this class is exported as a constructor (appears in globalEntries as Class)
                const isExportedAsClass = globalEntries.some(([name, value]) => value instanceof Class_1.Class && value === clazz);
                if (isExportedAsClass) {
                    const exportName = (_a = globalEntries.find(([name, value]) => value instanceof Class_1.Class && value === clazz)) === null || _a === void 0 ? void 0 : _a[0];
                    // Determine if it's a concrete class or interface
                    // You might need to adjust this logic based on how you distinguish them
                    const isInterface = ((_b = clazz.isInterface) === null || _b === void 0 ? void 0 : _b.call(clazz)) || false; // Adjust this condition as needed
                    if (isInterface) {
                        return `    export const ${exportName}: ${getName(clazz)}_;`;
                    }
                    else {
                        return `    export const ${exportName}: typeof ${getName(clazz)}_;`;
                    }
                }
                return null;
            })
            .filter((entry) => entry !== null)
            .join("\n\n")}

}
`;
        const importsForScriptEventPatch = `
// imports for
${eventEntries.map((entry) => entry[1]).map((kClassImpl) => `import type { ${kClassImpl.simpleName} } from '../types/${kClassImpl.qualifiedName.replaceAll(".", "/")}.d.ts'`).join("\n")}


`;
        const onEventsForScriptPatch = `
// on events
${eventEntries.map((entry) => `on(eventName: "${entry[0]}", handler: (${entry[0]}Event: ${entry[1].simpleName}) => void): Unit;`).join("\n")}


`;
        Client.displayChatMessage("Generated TypeScript definitions successfully!");
        Client.displayChatMessage(`Output path: ${path}`);
        // Output the generated content to console for debugging
        console.log(embeddedDefinition);
        // @ts-expect-error
        const Files = Java.type('java.nio.file.Files');
        // @ts-expect-error
        const filePath = Paths_1.Paths.get(`${path}/${packageName}/ambient/ambient.d.ts`);
        // @ts-expect-error
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, embeddedDefinition,
            // @ts-expect-error
            Java.type("java.nio.charset.StandardCharsets").UTF_8);
        // Write the ScriptModule augmentation file
        const augmentationContent = `// ScriptModule augmentation - adds event handler interfaces

// Event type imports
${importsForScriptEventPatch}
import type { Unit } from '../types/kotlin/Unit';

// Augment ScriptModule with specific event handler overloads
declare module '../types/net/ccbluex/liquidbounce/script/bindings/features/ScriptModule' {
    interface ScriptModule {
        on(eventName: "enable" | "disable", handler: () => void): Unit;

        // on events with specific event types
        ${onEventsForScriptPatch}
    }
}
`;
        // @ts-expect-error
        const augmentationFilePath = Paths_1.Paths.get(`${path}/${packageName}/augmentations/ScriptModule.augmentation.d.ts`);
        // @ts-expect-error
        Files.createDirectories(augmentationFilePath.getParent());
        Files.writeString(augmentationFilePath, augmentationContent,
            // @ts-expect-error
            Java.type("java.nio.charset.StandardCharsets").UTF_8);
        console.log(importsForScriptEventPatch);
        console.log(onEventsForScriptPatch);
    }
    catch (e) {
        console.error(e);
        Client.displayChatMessage(`Error generating TypeScript definitions: ${e.message}`);
        e.printStackTrace();
        throw e;
    }
}
const packageName = "@ccbluex/liquidbounce-script-api";
const path = ScriptManager_1.ScriptManager.INSTANCE.root.path;
// @ts-expect-error
if (Java.type("java.lang.System").getenv("SCRIPT_TYPEGEN_BUILD")) {
    generate(path, packageName);
    mc.close();
}
script.registerCommand({
    name: "ts-defgen",
    aliases: ["tsgen"],
    parameters: [],
    onExecute() {
        // @ts-expect-error
        UnsafeThread.run(() => generate(path, packageName));
    }
});
