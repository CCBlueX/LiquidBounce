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
    authors: ["commandblock2"],
});
function work(path, packageName) {
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
        Paths_1.Paths.get(path + "/liquidbounce-script-api"));
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
        Client.displayChatMessage(`Output path: ${path}/liquidbounce-script-api`);
        // Output the generated content to console for debugging
        console.log(embeddedDefinition);
        // @ts-expect-error
        const Files = Java.type('java.nio.file.Files');
        // @ts-expect-error
        const filePath = Paths_1.Paths.get(`${path}/liquidbounce-script-api/${packageName}/ambient/ambient.d.ts`);
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
        const augmentationFilePath = Paths_1.Paths.get(`${path}/liquidbounce-script-api/${packageName}/augmentations/ScriptModule.augmentation.d.ts`);
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
const packageName = "liquidbounce-script-api";
const path = ScriptManager_1.ScriptManager.INSTANCE.root.path;
// @ts-expect-error
if (Java.type("java.lang.System").getenv("CI_BUILD")) {
    work(path, packageName);
    mc.close();
}
script.registerCommand({
    name: "ts-defgen",
    aliases: ["tsgen"],
    parameters: [],
    onExecute() {
        // @ts-expect-error
        UnsafeThread.run(() => work(path, packageName));
    }
});
//# sourceMappingURL=data:application/json;base64,eyJ2ZXJzaW9uIjozLCJmaWxlIjoidHMtZGVmZ2VuLmpzIiwic291cmNlUm9vdCI6IiIsInNvdXJjZXMiOlsiLi4vc3JjL3RzLWRlZmdlbi50cyJdLCJuYW1lcyI6W10sIm1hcHBpbmdzIjoiOztBQUFBLHNFQUFtRTtBQUNuRSxpREFBOEM7QUFFOUMsdURBQW9EO0FBQ3BELHlEQUFzRDtBQUN0RCxtQkFBbUI7QUFDbkIseURBQXNEO0FBQ3RELG1CQUFtQjtBQUNuQiw2REFBMEQ7QUFDMUQsOEVBQTJFO0FBQzNFLHFEQUFrRDtBQUNsRCwyR0FBd0c7QUFLeEcsNkVBQTBFO0FBQzFFLDJGQUF3RjtBQUl4RixrRkFBOEU7QUFDOUUsNkRBQTBEO0FBQzFELG9GQUFpRjtBQUVqRixNQUFNLEtBQUssR0FBRywyQkFBWSxDQUFDLGNBQWMsQ0FBQTtBQUV6QyxtQkFBbUI7QUFDbkIsTUFBTSxhQUFhLEdBQVUsTUFBTSxDQUFDLE9BQU8sQ0FBQyxVQUFVLENBQUMsQ0FBQztBQUV4RCxzREFBc0Q7QUFDdEQsU0FBUyx3QkFBd0IsQ0FBQyxPQUFlO0lBQzdDLElBQUksQ0FBQztRQUNELGlDQUFpQztRQUNqQyxNQUFNLE9BQU8sR0FBRyxJQUFJLFdBQUksQ0FBQyxPQUFPLENBQUMsQ0FBQztRQUVsQyxzQkFBc0I7UUFDdEIsTUFBTSxNQUFNLEdBQUcsT0FBTyxDQUFDLEtBQUssRUFBRSxDQUFDLEtBQUssRUFBRSxDQUFDO1FBRXZDLCtEQUErRDtRQUMvRCxPQUFPLElBQUksK0JBQWMsQ0FDckIsQ0FBQyxNQUFNLENBQUMsRUFDUixlQUFNLENBQUMsYUFBYSxFQUFFLENBQUMscUJBQXFCLEVBQUUsQ0FDakQsQ0FBQztJQUNOLENBQUM7SUFBQyxPQUFPLENBQUMsRUFBRSxDQUFDO1FBQ1QsT0FBTyxDQUFDLEtBQUssQ0FBQyw2QkFBNkIsRUFBRSxDQUFDLENBQUMsQ0FBQztRQUNoRCxNQUFNLENBQUMsQ0FBQztJQUNaLENBQUM7QUFDTCxDQUFDO0FBRUQsb0RBQW9EO0FBQ3BELFNBQVMsZ0JBQWdCLENBQUMsV0FBMkIs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bkIsSUFBSSxJQUFJLENBQUMsSUFBSSxDQUFDLGtCQUFrQixDQUFDLENBQUMsTUFBTSxDQUFDLFVBQVUsQ0FBQyxFQUFFLENBQUM7SUFDbkQsSUFBSSxDQUFDLElBQUksRUFBRSxXQUFXLENBQUMsQ0FBQTtJQUN2QixFQUFFLENBQUMsS0FBSyxFQUFFLENBQUM7QUFDZixDQUFDO0FBRUQsTUFBTSxDQUFDLGVBQWUsQ0FBQztJQUNuQixJQUFJLEVBQUUsV0FBVztJQUNqQixPQUFPLEVBQUUsQ0FBQyxPQUFPLENBQUM7SUFDbEIsVUFBVSxFQUFFLEVBQ1g7SUFDRCxTQUFTO1FBQ0wsbUJBQW1CO1FBQ25CLFlBQVksQ0FBQyxHQUFHLENBQUMsR0FBRyxFQUFFLENBQUMsSUFBSSxDQUFDLElBQUksRUFBRSxXQUFXLENBQUMsQ0FBQyxDQUFDO0lBQ3BELENBQUM7Q0FDSixDQUFDLENBQUMiLCJzb3VyY2VzQ29udGVudCI6WyJpbXBvcnQgeyBVUkxDbGFzc0xvYWRlciB9IGZyb20gXCJqdm0tdHlwZXMvamF2YS9uZXQvVVJMQ2xhc3NMb2FkZXJcIjtcbmltcG9ydCB7IEZpbGUgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvaW8vRmlsZVwiO1xuaW1wb3J0IHsgVVJMIH0gZnJvbSBcImp2bS10eXBlcy9qYXZhL25ldC9VUkxcIjtcbmltcG9ydCB7IFRocmVhZCB9IGZyb20gXCJqdm0tdHlwZXMvamF2YS9sYW5nL1RocmVhZFwiO1xuaW1wb3J0IHsgUGF0aHMgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvbmlvL2ZpbGUvUGF0aHNcIjtcbi8vIEB0cy1leHBlY3QtZXJyb3JcbmltcG9ydCB7IEhhc2hNYXAgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvdXRpbC9IYXNoTWFwXCI7XG4vLyBAdHMtZXhwZWN0LWVycm9yXG5pbXBvcnQgeyBBcnJheUxpc3QgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvdXRpbC9BcnJheUxpc3RcIjtcbmltcG9ydCB7IEp2bUNsYXNzTWFwcGluZ0t0IH0gZnJvbSBcImp2bS10eXBlcy9rb3RsaW4vanZtL0p2bUNsYXNzTWFwcGluZ0t0XCI7XG5pbXBvcnQgeyBDbGFzcyB9IGZyb20gXCJqdm0tdHlwZXMvamF2YS9sYW5nL0NsYXNzXCI7XG5pbXBvcnQgeyBTY3JpcHRNb2R1bGUgfSBmcm9tIFwianZtLXR5cGVzL25ldC9jY2JsdWV4L2xpcXVpZGJvdW5jZS9zY3JpcHQvYmluZGluZ3MvZmVhdHVyZXMvU2NyaXB0TW9kdWxlXCI7XG5pbXBvcnQgeyBPYmplY3QgYXMgSmF2YU9iamVjdCB9IGZyb20gXCJqdm0tdHlwZXMvamF2YS9sYW5nL09iamVjdFwiO1xuLy8gQHRzLWV4cGVjdC1lcnJvclxuaW1wb3J0IHsgTWFwIGFzIEphdmFNYXAgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvdXRpbC9NYXBcIjtcbmltcG9ydCB7IFRocm93YWJsZSB9IGZyb20gXCJqdm0tdHlwZXMvamF2YS9sYW5nL1Rocm93YWJsZVwiO1xuaW1wb3J0IHsgQ2xhc3NQYXRoIH0gZnJvbSBcImp2bS10eXBlcy9jb20vZ29vZ2xlL2NvbW1vbi9yZWZsZWN0L0NsYXNzUGF0aFwiO1xuaW1wb3J0IHsgU2NyaXB0TWFuYWdlciB9IGZyb20gXCJqdm0tdHlwZXMvbmV0L2NjYmx1ZXgvbGlxdWlkYm91bmNlL3NjcmlwdC9TY3JpcHRNYW5hZ2VyXCI7XG5pbXBvcnQgeyBFeGNlcHRpb24gfSBmcm9tIFwianZtLXR5cGVzL2phdmEvbGFuZy9FeGNlcHRpb25cIjtcbmltcG9ydCB7IEZpbGVzS3QgfSBmcm9tIFwianZtLXR5cGVzL2tvdGxpbi9pby9GaWxlc0t0XCI7XG5pbXBvcnQgeyBGaWxlIGFzIEphdmFGaWxlIH0gZnJvbSBcImp2bS10eXBlcy9qYXZhL2lvL0ZpbGVcIjtcbmltcG9ydCB7IExpcXVpZEJvdW5jZSB9IGZyb20gXCJqdm0tdHlwZXMvbmV0L2NjYmx1ZXgvbGlxdWlkYm91bmNlL0xpcXVpZEJvdW5jZVwiXG5pbXBvcnQgeyBMb2NhbERhdGUgfSBmcm9tIFwianZtLXR5cGVzL2phdmEvdGltZS9Mb2NhbERhdGVcIjtcbmltcG9ydCB7IERhdGVUaW1lRm9ybWF0dGVyIH0gZnJvbSBcImp2bS10eXBlcy9qYXZhL3RpbWUvZm9ybWF0L0RhdGVUaW1lRm9ybWF0dGVyXCI7XG5cbmNvbnN0IGluRGV2ID0gTGlxdWlkQm91bmNlLklOX0RFVkVMT1BNRU5UXG5cbi8vIEB0cy1leHBlY3QtZXJyb3JcbmNvbnN0IGdsb2JhbEVudHJpZXM6IGFueVtdID0gT2JqZWN0LmVudHJpZXMoZ2xvYmFsVGhpcyk7XG5cbi8vIEZ1bmN0aW9uIHRvIGNyZWF0ZSBhIFVSTENsYXNzTG9hZGVyIGZyb20gYSBKQVIgcGF0aFxuZnVuY3Rpb24gY3JlYXRlQ2xhc3NMb2FkZXJGcm9tSmFyKGphclBhdGg6IHN0cmluZyk6IFVSTENsYXNzTG9hZGVyIHtcbiAgICB0cnkge1xuICAgICAgICAvLyBDcmVhdGUgRmlsZSBvYmplY3QgZm9yIHRoZSBKQVJcbiAgICAgICAgY29uc3QgamFyRmlsZSA9IG5ldyBGaWxlKGphclBhdGgpO1xuXG4gICAgICAgIC8vIENvbnZlcnQgRmlsZSB0byBVUkxcbiAgICAgICAgY29uc3QgamFyVXJsID0gamFyRmlsZS50b1VSSSgpLnRvVVJMKCk7XG5cbiAgICAgICAgLy8gQ3JlYXRlIFVSTENsYXNzTG9hZGVyIHdpdGggdGhlIHN5c3RlbSBjbGFzcyBsb2FkZXIgYXMgcGFyZW50XG4gICAgICAgIHJldHVybiBuZXcgVVJMQ2xhc3NMb2FkZXIoXG4gICAgICAgICAgICBbamFyVXJsXSxcbiAgICAgICAgICAgIFRocmVhZC5jdXJyZW50VGhyZWFkKCkuZ2V0Q29udGV4dENsYXNzTG9hZGVyKClcbiAgICAgICAgKTtcbiAgICB9IGNhdGNoIChlKSB7XG4gICAgICAgIGNvbnNvbGUuZXJyb3IoXCJFcnJvciBjcmVhdGluZyBDbGFzc0xvYWRlcjpcIiwgZSk7XG4gICAgICAgIHRocm93IGU7XG4gICAgfVxufVxuXG4vLyBGdW5jdGlvbiB0byBsb2FkIGEgY2xhc3MgZnJvbSBhIGdpdmVuIENsYXNzTG9hZGVyXG5mdW5jdGlvbiBsb2FkQ2xhc3NGcm9tSmFyKGNsYXNzTG9hZGVyOiBVUkxDbGFzc0xvYWRlciwgY2xhc3NOYW1lOiBzdHJpbmcpOiBDbGFzczxhbnk+IHtcbiAgICB0cnkge1xuICAgICAgICByZXR1cm4gY2xhc3NMb2FkZXIubG9hZENsYXNzKGNsYXNzTmFtZSk7XG4gICAgfSBjYXRjaCAoZSkge1xuICAgICAgICBjb25zb2xlLmVycm9yKGBFcnJvciBsb2FkaW5nIGNsYXNzICR7Y2xhc3NOYW1lfTpgLCBlKTtcbiAgICAgICAgdGhyb3cgZTtcbiAgICB9XG59XG5cbi8vIEB0cy1leHBlY3QtZXJyb3JcbmZ1bmN0aW9uIGZpbmRBbGxDbGFzc0luZm9zKCk6IENsYXNzSW5mbzxhbnk+W10ge1xuICAgIC8vIEB0cy1leHBlY3QtZXJyb3JcbiAgICByZXR1cm4gSmF2YS5mcm9tKFxuICAgICAgICBDbGFzc1BhdGguZnJvbShcbiAgICAgICAgICAgIFRocmVhZC5jdXJyZW50VGhyZWFkKClcbiAgICAgICAgICAgICAgICAuZ2V0Q29udGV4dENsYXNzTG9hZGVyKClcbiAgICAgICAgKVxuICAgICAgICAgICAgLmdldFRvcExldmVsQ2xhc3NlcygpXG4gICAgICAgICAgICAvLyBAdHMtZXhwZWN0LWVycm9yXG4gICAgICAgICAgICAuYXNMaXN0KClcbiAgICApO1xufVxuXG5cbmZ1bmN0aW9uIGdldE5hbWUoamF2YUNsYXNzOiBDbGFzczxhbnk+KTogc3RyaW5nIHtcbiAgICBjb25zdCBmdWxsTmFtZSA9IGphdmFDbGFzcy5uYW1lO1xuICAgIHJldHVybiBmdWxsTmFtZS5zdWJzdHJpbmcoZnVsbE5hbWUubGFzdEluZGV4T2YoXCIuXCIpICsgMSk7XG59XG5cbmNvbnN0IHNjcmlwdCA9IHJlZ2lzdGVyU2NyaXB0LmFwcGx5KHtcbiAgICBuYW1lOiBcInRzLWRlZmdlblwiLFxuICAgIHZlcnNpb246IFwiMS4wLjBcIixcbiAgICBhdXRob3JzOiBbXCJjb21tYW5kYmxvY2syXCJdLFxufSk7XG5cbmZ1bmN0aW9uIHdvcmsocGF0aDogc3RyaW5nLCBwYWNrYWdlTmFtZTogc3RyaW5nKSB7XG4gICAgdHJ5IHtcbiAgICAgICAgY29uc3QgbG9hZGVyID0gY3JlYXRlQ2xhc3NMb2FkZXJGcm9tSmFyKFxuICAgICAgICAgICAgcGF0aCArIFwiL3RzLWdlbmVyYXRvci5qYXJcIlxuICAgICAgICApO1xuICAgICAgICBjb25zdCBOUE1HZW4gPSBsb2FkQ2xhc3NGcm9tSmFyKFxuICAgICAgICAgICAgbG9hZGVyLFxuICAgICAgICAgICAgXCJtZS5jb21tYW5kYmxvY2syLnRzR2VuZXJhdG9yLk5QTVBhY2thZ2VHZW5lcmF0b3JcIlxuICAgICAgICApO1xuICAgICAgICBjb25zdCBUc0dlbiA9IGxvYWRDbGFzc0Zyb21KYXIoXG4gICAgICAgICAgICBsb2FkZXIsXG4gICAgICAgICAgICBcIm1lLm50cnJnYy50c0dlbmVyYXRvci5UeXBlU2NyaXB0R2VuZXJhdG9yXCJcbiAgICAgICAgKTtcbiAgICAgICAgY29uc3QgVm9pZFR5cGUgPSBsb2FkQ2xhc3NGcm9tSmFyKFxuICAgICAgICAgICAgbG9hZGVyLFxuICAgICAgICAgICAgXCJtZS5udHJyZ2MudHNHZW5lcmF0b3IuVm9pZFR5cGVcIlxuICAgICAgICApO1xuXG4gICAgICAgIGNvbnN0IE5VTEwgPSBWb2lkVHlwZS5nZXRFbnVtQ29uc3RhbnRzKClbMF07XG5cbiAgICAgICAgY29uc3QgamF2YUNsYXNzZXMgPSBnbG9iYWxFbnRyaWVzXG4gICAgICAgICAgICAuZmlsdGVyKChlbnRyeSkgPT4gZW50cnlbMV0gIT0gdW5kZWZpbmVkKVxuICAgICAgICAgICAgLm1hcCgoZW50cnkpID0+IChlbnRyeVsxXSBpbnN0YW5jZW9mIENsYXNzID8gZW50cnlbMV0gOiBlbnRyeVsxXS5jbGFzcykpXG4gICAgICAgICAgICAuZmlsdGVyKChlbnRyeSkgPT4gZW50cnkgIT0gdW5kZWZpbmVkKTtcblxuICAgICAgICBjb25zdCBldmVudEVudHJpZXMgPSAoUmVmbGVjdGlvblV0aWwuZ2V0RGVjbGFyZWRGaWVsZChTY3JpcHRNb2R1bGUgYXMgdW5rbm93biBhcyBDbGFzczxKYXZhT2JqZWN0PiwgXCJMT1dFUkNBU0VfTkFNRV9FVkVOVF9NQVBcIikgYXMgSmF2YU1hcCkuZW50cnlTZXQoKS50b0FycmF5KCk7XG5cbiAgICAgICAgQ2xpZW50LmRpc3BsYXlDaGF0TWVzc2FnZShcImxvb2tpbmcgZm9yIGFsbCBqdm0gY2xhc3Nlc1wiKVxuICAgICAgICBjb25zdCBhbGxDbGFzc0luZm9zID0gZmluZEFsbENsYXNzSW5mb3MoKVxuXG4gICAgICAgIENsaWVudC5kaXNwbGF5Q2hhdE1lc3NhZ2UoYGZvdW5kICR7YWxsQ2xhc3NJbmZvcy5sZW5ndGh9IGNsYXNzZXMsIGNvbnZlcnRpbmcgdG8ga290bGluIGNsYXNzZXNgKVxuXG5cbiAgICAgICAgY29uc3QgY2xhc3NOYW1lcyA9IFtcImphdmEubmV0LlVSTENsYXNzTG9hZGVyXCIsXG4gICAgICAgICAgICBcImphdmEubmlvLmZpbGUuUGF0aHNcIixcbiAgICAgICAgICAgIFwiamF2YS51dGlsLkhhc2hNYXBcIixcbiAgICAgICAgICAgIFwiamF2YS51dGlsLkFycmF5TGlzdFwiLFxuICAgICAgICAgICAgXCJqYXZhLnV0aWwuamFyLkphcklucHV0U3RyZWFtXCIsXG4gICAgICAgICAgICBcImphdmEudXRpbC5NYXBcIixcbiAgICAgICAgICAgIFwiY29tLmdvb2dsZS5jb21tb24ucmVmbGVjdC5DbGFzc1BhdGhcIixcbiAgICAgICAgICAgIFwia290bGluLmp2bS5Kdm1DbGFzc01hcHBpbmdLdFwiXG4gICAgICAgIF1cbiAgICAgICAgICAgIC5jb25jYXQoYWxsQ2xhc3NJbmZvcy5tYXAoKGVudHJ5KSA9PiB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIGVudHJ5LmdldE5hbWUoKVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KSk7XG4gICAgICAgIGNvbnN0IGp2bUNsYXNzZXMgPSBjbGFzc05hbWVzXG4gICAgICAgICAgICAubWFwKChlbnRyeSkgPT4ge1xuICAgICAgICAgICAgICAgIHRyeSB7XG4gICAgICAgICAgICAgICAgICAgIHJldHVybiBSZWZsZWN0aW9uVXRpbC5jbGFzc0J5TmFtZShlbnRyeSlcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgY2F0Y2ggKGUpIHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICAgICAgfVxuICAgICAgICAgICAgfVxuICAgICAgICAgICAgKVxuICAgICAgICAgICAgLmZpbHRlcigoZW50cnkpID0+IGVudHJ5ICE9IHVuZGVmaW5lZCk7XG4gICAgICAgIGNvbnN0IGp2bUNsYXNzZXNJbktvdGxpbiA9IGp2bUNsYXNzZXNcbiAgICAgICAgICAgIC5tYXAoKGVudHJ5KSA9PiB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIEp2bUNsYXNzTWFwcGluZ0t0LmdldEtvdGxpbkNsYXNzKGVudHJ5KVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KVxuXG4gICAgICAgICAgICAuZmlsdGVyKChlbnRyeSkgPT4gZW50cnkgIT0gbnVsbCk7XG5cbiAgICAgICAgQ2xpZW50LmRpc3BsYXlDaGF0TWVzc2FnZShgY29udmVydGVkIHRvICR7anZtQ2xhc3Nlc0luS290bGluLmxlbmd0aH0ga290bGluIGNsYXNzZXNgKVxuICAgICAgICBjb25zdCBrb3RsaW5DbGFzc2VzID0gamF2YUNsYXNzZXNcbiAgICAgICAgICAgIC5jb25jYXQoW1xuICAgICAgICAgICAgICAgIC8vIFVzaW5nIHRoZSBpbXBvcnRlZCBjbGFzcyBmcm9tIEBlbWJlZGRlZFxuICAgICAgICAgICAgICAgIFJlZmxlY3Rpb25VdGlsLmNsYXNzQnlOYW1lKFxuICAgICAgICAgICAgICAgICAgICBcIm5ldC5jY2JsdWV4LmxpcXVpZGJvdW5jZS5zY3JpcHQuYmluZGluZ3MuZmVhdHVyZXMuU2NyaXB0TW9kdWxlXCJcbiAgICAgICAgICAgICAgICApXG4gICAgICAgICAgICBdKVxuICAgICAgICAgICAgLmNvbmNhdChldmVudEVudHJpZXMubWFwKChlbnRyeTogYW55KSA9PiAoZW50cnkgYXMgQXJyYXk8YW55PilbMV0pKVxuICAgICAgICAgICAgLm1hcChlbnRyeSA9PiB7XG4gICAgICAgICAgICAgICAgdHJ5IHtcbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIEp2bUNsYXNzTWFwcGluZ0t0LmdldEtvdGxpbkNsYXNzKGVudHJ5KVxuICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICBjYXRjaCAoZSkge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gbnVsbDtcbiAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICB9KVxuICAgICAgICAgICAgLmZpbHRlcigoZW50cnkpID0+IGVudHJ5ICE9IHVuZGVmaW5lZClcbiAgICAgICAgICAgIC5jb25jYXQoXG4gICAgICAgICAgICAgICAganZtQ2xhc3Nlc0luS290bGluXG4gICAgICAgICAgICApO1xuXG4gICAgICAgIGNvbnN0IGNsYXNzZXMgPSBuZXcgQXJyYXlMaXN0KGtvdGxpbkNsYXNzZXMpO1xuXG4gICAgICAgIENsaWVudC5kaXNwbGF5Q2hhdE1lc3NhZ2UoYGdlbmVyYXRpbmcgdHlwZXMgZm9yICR7Y2xhc3Nlcy5sZW5ndGh9IGNsYXNzZXNgKVxuICAgICAgICBDbGllbnQuZGlzcGxheUNoYXRNZXNzYWdlKFwidGhpcyBtYXkgdGFrZSBhIHdoaWxlLCBwbGVhc2Ugd2FpdC4uLlwiKTtcbiAgICAgICAgLy8gQHRzLWV4cGVjdC1lcnJvclxuICAgICAgICBjb25zdCBnZW5lcmF0ZWQgPSBuZXcgVHNHZW4oXG4gICAgICAgICAgICBjbGFzc2VzLFxuICAgICAgICAgICAgbmV3IEhhc2hNYXAoKSxcbiAgICAgICAgICAgIG5ldyBBcnJheUxpc3QoKSxcbiAgICAgICAgICAgIG5ldyBBcnJheUxpc3QoKSxcbiAgICAgICAgICAgIFwibnVtYmVyXCIsXG4gICAgICAgICAgICBOVUxMXG4gICAgICAgICk7XG5cbiAgICAgICAgY29uc3QgdG9kYXkgPSBMb2NhbERhdGUubm93KCk7XG4gICAgICAgIGNvbnN0IGZvcm1hdHRlciA9IERhdGVUaW1lRm9ybWF0dGVyLm9mUGF0dGVybigneS5NLmQnKTtcblxuICAgICAgICBDbGllbnQuZGlzcGxheUNoYXRNZXNzYWdlKFwid3JpdGluZyB0eXBlc1wiKTtcbiAgICAgICAgLy8gQHRzLWV4cGVjdC1lcnJvclxuICAgICAgICBjb25zdCBucG1QYWNrID0gbmV3IE5QTUdlbihnZW5lcmF0ZWQsIHBhY2thZ2VOYW1lLFxuICAgICAgICAgICAgYCR7aW5EZXYgPyB0b2RheS5mb3JtYXQoZm9ybWF0dGVyKSA6IExpcXVpZEJvdW5jZS5JTlNUQU5DRS5jbGllbnRWZXJzaW9uXG4gICAgICAgICAgICB9KyR7TGlxdWlkQm91bmNlLklOU1RBTkNFLmNsaWVudEJyYW5jaH0uJHtMaXF1aWRCb3VuY2UuSU5TVEFOQ0UuY2xpZW50Q29tbWl0fWAsXG4gICAgICAgICAgICAvLyBleHRyYUZpbGVzIC0gYWRkIHRoZSBhbWJpZW50IGFuZCBhdWdtZW50YXRpb25zIGZpbGVzXG4gICAgICAgICAgICBgXCJhdWdtZW50YXRpb25zLyoqLyouZC50c1wiLCBcImFtYmllbnQvYW1iaWVudC5kLnRzXCJgLFxuICAgICAgICAgICAgLy8gZXh0cmFUeXBlc1ZlcnNpb24gLSBhZGQgdGhlIGF1Z21lbnRhdGlvbnMgYW5kIGFtYmllbnQgcGF0aHMgIFxuICAgICAgICAgICAgYFwiLi9hdWdtZW50YXRpb25zLypcIiwgXCJhbWJpZW50L2FtYmllbnQuZC50c1wiYCxcbiAgICAgICAgICAgIC8vIG90aGVyRXh0cmFzIC0gYWRkIHRoZSB0eXBlcyBmaWVsZFxuICAgICAgICAgICAgYFwidHlwZXNcIjogXCJhbWJpZW50L2FtYmllbnQuZC50c1wiYCxcbiAgICAgICAgICAgIG51bGxcbiAgICAgICAgKTtcblxuICAgICAgICBucG1QYWNrLndyaXRlUGFja2FnZVRvKFxuICAgICAgICAgICAgLy8gQHRzLWV4cGVjdC1lcnJvclxuICAgICAgICAgICAgUGF0aHMuZ2V0KHBhdGggKyBcIi90eXBlcy1nZW5cIilcbiAgICAgICAgKTtcblxuICAgICAgICBDbGllbnQuZGlzcGxheUNoYXRNZXNzYWdlKFwicHJpbnQgZW1iZWRkZWQgc2NyaXB0IHR5cGVzLCBzZWUgbG9nIGZvciBtb3JlIGluZm8sIHRob3NlIGFyZSBmb3IgbWFpbnRhaW5hY2UgdXNlXCIpXG5cbiAgICAgICAgY29uc3QgZW1iZWRkZWREZWZpbml0aW9uID0gYFxuLy8gYW1iaWVudC50c1xuLy8gaW1wb3J0c1xuaW1wb3J0IFwiLi4vYXVnbWVudGF0aW9ucy9pbmRleC5kLnRzXCJcbiR7amF2YUNsYXNzZXNcbiAgICAgICAgICAgICAgICAubWFwKChjbGF6eikgPT4ge1xuICAgICAgICAgICAgICAgICAgICByZXR1cm4gYGltcG9ydCB7ICR7Z2V0TmFtZShjbGF6eil9IGFzICR7Z2V0TmFtZShjbGF6eil9XyB9IGZyb20gXCIuLi90eXBlcy8ke2NsYXp6Lm5hbWUucmVwbGFjZUFsbChcIi5cIiwgXCIvXCIpfVwiO2A7XG4gICAgICAgICAgICAgICAgfSlcbiAgICAgICAgICAgICAgICAuam9pbihcIlxcblwiKX1cbmRlY2xhcmUgZ2xvYmFsIHtcblxuXG4vLyBleHBvcnRzXG4ke2dsb2JhbEVudHJpZXNcbiAgICAgICAgICAgICAgICAuZmlsdGVyKChlbnRyeSkgPT4gZW50cnlbMV0gIT0gdW5kZWZpbmVkKVxuICAgICAgICAgICAgICAgIC5maWx0ZXIoKGVudHJ5KSA9PiAhKGVudHJ5WzFdIGluc3RhbmNlb2YgQ2xhc3MpKVxuICAgICAgICAgICAgICAgIC5maWx0ZXIoKGVudHJ5KSA9PiBlbnRyeVsxXS5jbGFzcyAhPSB1bmRlZmluZWQpXG4gICAgICAgICAgICAgICAgLm1hcCgoZW50cnkpID0+IGAgICAgZXhwb3J0IGNvbnN0ICR7ZW50cnlbMF19OiAke2dldE5hbWUoZW50cnlbMV0uY2xhc3MpfV87YClcbiAgICAgICAgICAgICAgICAuam9pbihcIlxcblxcblwiKX1cblxuJHtqYXZhQ2xhc3Nlc1xuICAgICAgICAgICAgICAgIC5tYXAoKGNsYXp6KSA9PiB7XG4gICAgICAgICAgICAgICAgICAgIC8vIENoZWNrIGlmIHRoaXMgY2xhc3MgaXMgZXhwb3J0ZWQgYXMgYSBjb25zdHJ1Y3RvciAoYXBwZWFycyBpbiBnbG9iYWxFbnRyaWVzIGFzIENsYXNzKVxuICAgICAgICAgICAgICAgICAgICBjb25zdCBpc0V4cG9ydGVkQXNDbGFzcyA9IGdsb2JhbEVudHJpZXMuc29tZSgoW25hbWUsIHZhbHVlXSkgPT4gXG4gICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZSBpbnN0YW5jZW9mIENsYXNzICYmIHZhbHVlID09PSBjbGF6elxuICAgICAgICAgICAgICAgICAgICApO1xuICAgICAgICAgICAgICAgICAgICBcbiAgICAgICAgICAgICAgICAgICAgaWYgKGlzRXhwb3J0ZWRBc0NsYXNzKSB7XG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCBleHBvcnROYW1lID0gZ2xvYmFsRW50cmllcy5maW5kKChbbmFtZSwgdmFsdWVdKSA9PiBcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICB2YWx1ZSBpbnN0YW5jZW9mIENsYXNzICYmIHZhbHVlID09PSBjbGF6elxuICAgICAgICAgICAgICAgICAgICAgICAgKT8uWzBdO1xuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICAvLyBEZXRlcm1pbmUgaWYgaXQncyBhIGNvbmNyZXRlIGNsYXNzIG9yIGludGVyZmFjZVxuICAgICAgICAgICAgICAgICAgICAgICAgLy8gWW91IG1pZ2h0IG5lZWQgdG8gYWRqdXN0IHRoaXMgbG9naWMgYmFzZWQgb24gaG93IHlvdSBkaXN0aW5ndWlzaCB0aGVtXG4gICAgICAgICAgICAgICAgICAgICAgICBjb25zdCBpc0ludGVyZmFjZSA9IGNsYXp6LmlzSW50ZXJmYWNlPy4oKSB8fCBmYWxzZTsgLy8gQWRqdXN0IHRoaXMgY29uZGl0aW9uIGFzIG5lZWRlZFxuICAgICAgICAgICAgICAgICAgICAgICAgXG4gICAgICAgICAgICAgICAgICAgICAgICBpZiAoaXNJbnRlcmZhY2UpIHtcbiAgICAgICAgICAgICAgICAgICAgICAgICAgICByZXR1cm4gYCAgICBleHBvcnQgY29uc3QgJHtleHBvcnROYW1lfTogJHtnZXROYW1lKGNsYXp6KX1fO2A7XG4gICAgICAgICAgICAgICAgICAgICAgICB9IGVsc2Uge1xuICAgICAgICAgICAgICAgICAgICAgICAgICAgIHJldHVybiBgICAgIGV4cG9ydCBjb25zdCAke2V4cG9ydE5hbWV9OiB0eXBlb2YgJHtnZXROYW1lKGNsYXp6KX1fO2A7XG4gICAgICAgICAgICAgICAgICAgICAgICB9XG4gICAgICAgICAgICAgICAgICAgIH1cbiAgICAgICAgICAgICAgICAgICAgcmV0dXJuIG51bGw7XG4gICAgICAgICAgICAgICAgfSlcbiAgICAgICAgICAgICAgICAuZmlsdGVyKChlbnRyeSkgPT4gZW50cnkgIT09IG51bGwpXG4gICAgICAgICAgICAgICAgLmpvaW4oXCJcXG5cXG5cIil9XG5cbn1cbmBcblxuICAgICAgICBjb25zdCBpbXBvcnRzRm9yU2NyaXB0RXZlbnRQYXRjaCA9IGBcbi8vIGltcG9ydHMgZm9yXG4ke2V2ZW50RW50cmllcy5tYXAoKGVudHJ5OiBhbnkpID0+IGVudHJ5WzFdKS5tYXAoKGtDbGFzc0ltcGw6IGFueSkgPT4gYGltcG9ydCB0eXBlIHsgJHtrQ2xhc3NJbXBsLnNpbXBsZU5hbWV9IH0gZnJvbSAnLi4vdHlwZXMvJHtrQ2xhc3NJbXBsLnF1YWxpZmllZE5hbWUucmVwbGFjZUFsbChcIi5cIiwgXCIvXCIpfS5kLnRzJ2ApLmpvaW4oXCJcXG5cIil9XG5cblxuYDtcbiAgICAgICAgY29uc3Qgb25FdmVudHNGb3JTY3JpcHRQYXRjaCA9IGBcbi8vIG9uIGV2ZW50c1xuJHtldmVudEVudHJpZXMubWFwKChlbnRyeTogYW55KSA9PiBgb24oZXZlbnROYW1lOiBcIiR7ZW50cnlbMF19XCIsIGhhbmRsZXI6ICgke2VudHJ5WzBdfUV2ZW50OiAke2VudHJ5WzFdLnNpbXBsZU5hbWV9KSA9PiB2b2lkKTogVW5pdDtgKS5qb2luKFwiXFxuXCIpfVxuXG5cbmA7XG5cbiAgICAgICAgQ2xpZW50LmRpc3BsYXlDaGF0TWVzc2FnZShcIkdlbmVyYXRlZCBUeXBlU2NyaXB0IGRlZmluaXRpb25zIHN1Y2Nlc3NmdWxseSFcIik7XG4gICAgICAgIENsaWVudC5kaXNwbGF5Q2hhdE1lc3NhZ2UoYE91dHB1dCBwYXRoOiAke3BhdGh9L3R5cGVzLWdlbmApO1xuXG4gICAgICAgIC8vIE91dHB1dCB0aGUgZ2VuZXJhdGVkIGNvbnRlbnQgdG8gY29uc29sZSBmb3IgZGVidWdnaW5nXG4gICAgICAgIGNvbnNvbGUubG9nKGVtYmVkZGVkRGVmaW5pdGlvbik7XG4gICAgICAgIC8vIEB0cy1leHBlY3QtZXJyb3JcbiAgICAgICAgY29uc3QgRmlsZXMgPSBKYXZhLnR5cGUoJ2phdmEubmlvLmZpbGUuRmlsZXMnKVxuICAgICAgICAvLyBAdHMtZXhwZWN0LWVycm9yXG4gICAgICAgIGNvbnN0IGZpbGVQYXRoID0gUGF0aHMuZ2V0KGAke3BhdGh9L3R5cGVzLWdlbi8ke3BhY2thZ2VOYW1lfS9hbWJpZW50L2FtYmllbnQuZC50c2ApO1xuXG4gICAgICAgIC8vIEB0cy1leHBlY3QtZXJyb3JcbiAgICAgICAgRmlsZXMuY3JlYXRlRGlyZWN0b3JpZXMoZmlsZVBhdGguZ2V0UGFyZW50KCkpO1xuXG4gICAgICAgIEZpbGVzLndyaXRlU3RyaW5nKFxuICAgICAgICAgICAgZmlsZVBhdGgsXG4gICAgICAgICAgICBlbWJlZGRlZERlZmluaXRpb24sXG4gICAgICAgICAgICAvLyBAdHMtZXhwZWN0LWVycm9yXG4gICAgICAgICAgICBKYXZhLnR5cGUoXCJqYXZhLm5pby5jaGFyc2V0LlN0YW5kYXJkQ2hhcnNldHNcIikuVVRGXzhcbiAgICAgICAgKVxuXG4gICAgICAgIC8vIFdyaXRlIHRoZSBTY3JpcHRNb2R1bGUgYXVnbWVudGF0aW9uIGZpbGVcbiAgICAgICAgY29uc3QgYXVnbWVudGF0aW9uQ29udGVudCA9IGAvLyBTY3JpcHRNb2R1bGUgYXVnbWVudGF0aW9uIC0gYWRkcyBldmVudCBoYW5kbGVyIGludGVyZmFjZXNcblxuLy8gRXZlbnQgdHlwZSBpbXBvcnRzXG4ke2ltcG9ydHNGb3JTY3JpcHRFdmVudFBhdGNofVxuaW1wb3J0IHR5cGUgeyBVbml0IH0gZnJvbSAnLi4vdHlwZXMva290bGluL1VuaXQnO1xuXG4vLyBBdWdtZW50IFNjcmlwdE1vZHVsZSB3aXRoIHNwZWNpZmljIGV2ZW50IGhhbmRsZXIgb3ZlcmxvYWRzXG5kZWNsYXJlIG1vZHVsZSAnLi4vdHlwZXMvbmV0L2NjYmx1ZXgvbGlxdWlkYm91bmNlL3NjcmlwdC9iaW5kaW5ncy9mZWF0dXJlcy9TY3JpcHRNb2R1bGUnIHtcbiAgICBpbnRlcmZhY2UgU2NyaXB0TW9kdWxlIHtcbiAgICAgICAgb24oZXZlbnROYW1lOiBcImVuYWJsZVwiIHwgXCJkaXNhYmxlXCIsIGhhbmRsZXI6ICgpID0+IHZvaWQpOiBVbml0O1xuXG4gICAgICAgIC8vIG9uIGV2ZW50cyB3aXRoIHNwZWNpZmljIGV2ZW50IHR5cGVzXG4gICAgICAgICR7b25FdmVudHNGb3JTY3JpcHRQYXRjaH1cbiAgICB9XG59XG5gO1xuXG4gICAgICAgIC8vIEB0cy1leHBlY3QtZXJyb3JcbiAgICAgICAgY29uc3QgYXVnbWVudGF0aW9uRmlsZVBhdGggPSBQYXRocy5nZXQoYCR7cGF0aH0vdHlwZXMtZ2VuLyR7cGFja2FnZU5hbWV9L2F1Z21lbnRhdGlvbnMvU2NyaXB0TW9kdWxlLmF1Z21lbnRhdGlvbi5kLnRzYCk7XG5cbiAgICAgICAgLy8gQHRzLWV4cGVjdC1lcnJvclxuICAgICAgICBGaWxlcy5jcmVhdGVEaXJlY3RvcmllcyhhdWdtZW50YXRpb25GaWxlUGF0aC5nZXRQYXJlbnQoKSk7XG5cbiAgICAgICAgRmlsZXMud3JpdGVTdHJpbmcoXG4gICAgICAgICAgICBhdWdtZW50YXRpb25GaWxlUGF0aCxcbiAgICAgICAgICAgIGF1Z21lbnRhdGlvbkNvbnRlbnQsXG4gICAgICAgICAgICAvLyBAdHMtZXhwZWN0LWVycm9yXG4gICAgICAgICAgICBKYXZhLnR5cGUoXCJqYXZhLm5pby5jaGFyc2V0LlN0YW5kYXJkQ2hhcnNldHNcIikuVVRGXzhcbiAgICAgICAgKTtcblxuICAgICAgICBjb25zb2xlLmxvZyhpbXBvcnRzRm9yU2NyaXB0RXZlbnRQYXRjaCk7XG4gICAgICAgIGNvbnNvbGUubG9nKG9uRXZlbnRzRm9yU2NyaXB0UGF0Y2gpO1xuICAgIH0gY2F0Y2ggKGUpIHtcbiAgICAgICAgY29uc29sZS5lcnJvcihlKTtcbiAgICAgICAgQ2xpZW50LmRpc3BsYXlDaGF0TWVzc2FnZShgRXJyb3IgZ2VuZXJhdGluZyBUeXBlU2NyaXB0IGRlZmluaXRpb25zOiAkeyhlIGFzIFRocm93YWJsZSkubWVzc2FnZX1gKTtcbiAgICAgICAgKGUgYXMgRXhjZXB0aW9uKS5wcmludFN0YWNrVHJhY2UoKVxuICAgICAgICB0aHJvdyBlO1xuICAgIH1cbn1cblxuY29uc3QgcGFja2FnZU5hbWUgPSBcImp2bS10eXBlc1wiXG5jb25zdCBwYXRoID0gU2NyaXB0TWFuYWdlci5JTlNUQU5DRS5yb290LnBhdGg7XG5cbi8vIEB0cy1leHBlY3QtZXJyb3JcbmlmIChKYXZhLnR5cGUoXCJqYXZhLmxhbmcuU3lzdGVtXCIpLmdldGVudihcIkNJX0JVSUxEXCIpKSB7XG4gICAgd29yayhwYXRoLCBwYWNrYWdlTmFtZSlcbiAgICBtYy5jbG9zZSgpO1xufVxuXG5zY3JpcHQucmVnaXN0ZXJDb21tYW5kKHtcbiAgICBuYW1lOiBcInRzLWRlZmdlblwiLFxuICAgIGFsaWFzZXM6IFtcInRzZ2VuXCJdLFxuICAgIHBhcmFtZXRlcnM6IFtcbiAgICBdLFxuICAgIG9uRXhlY3V0ZSgpIHtcbiAgICAgICAgLy8gQHRzLWV4cGVjdC1lcnJvclxuICAgICAgICBVbnNhZmVUaHJlYWQucnVuKCgpID0+IHdvcmsocGF0aCwgcGFja2FnZU5hbWUpKTtcbiAgICB9XG59KTtcbiJdfQ==
