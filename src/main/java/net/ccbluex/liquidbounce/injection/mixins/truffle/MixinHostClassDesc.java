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

package net.ccbluex.liquidbounce.injection.mixins.truffle;

import kotlin.Lazy;
import kotlin.LazyKt;
import net.ccbluex.liquidbounce.interfaces.MemberRetriever;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.ccbluex.liquidbounce.utils.mappings.EnvironmentRemapper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

@Pseudo
@Mixin(targets = "com/oracle/truffle/host/HostClassDesc$Members")
public abstract class MixinHostClassDesc {

    @Unique
    @SuppressWarnings("unchecked")
    private static final Lazy<BiFunction<Object, Object, Object>> mergeMethod = LazyKt.lazy(() -> {
        try {
            var clazz = Class.forName("com.oracle.truffle.host.HostClassDesc$Members");
            var field = clazz.getDeclaredField("MERGE");
            return (BiFunction<Object, Object, Object>) field.get(null);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    });

    @Shadow(remap = false)
    @Final
    Map<String, Object> methods;

    @Shadow(remap = false)
    @Final
    Map<String, Object> fields;

    @Shadow(remap = false)
    @Final
    Map<String, Object> staticFields;

    @Shadow(remap = false)
    @Final
    Map<String, Object> staticMethods;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false)
    private void remapClassDesc(CallbackInfo ci) {
        remapFieldEntries(fields, MixinHostClassDesc::getField);
        remapFieldEntries(staticFields, MixinHostClassDesc::getField);

        remapMethodEntries(methods);
        remapMethodEntries(staticMethods);
    }

    @Unique
    private static Object[] getOverloadsFromHostMethodDesc(Object value) throws
            IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        var clazz = value.getClass();
        var getOverloads = clazz.getMethod("getOverloads");
        getOverloads.setAccessible(true);
        return (Object[]) getOverloads.invoke(value);
    }

    @Unique
    private static @Nullable Executable getReflectionMethodFromSingleMethod(Object value)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        var clazz = value.getClass();
        if (NOT_MAPPED.contains(clazz.getName())) {
            return null;
        }
        var descMethod = clazz.getDeclaredMethod("getReflectionMethod");
        descMethod.setAccessible(true);
        return (Executable) descMethod.invoke(value);
    }

    @Unique
    private static void remapMethodEntries(Map<String, Object> map) {
        if (map.isEmpty()) {
            return;
        }

        var entries = new HashMap<>(map).entrySet();

        for (var entry : entries) {
            var key = entry.getKey();
            var value = entry.getValue();
            try {
                // Get all overload methods
                for (var overload : getOverloadsFromHostMethodDesc(value)) {
                    var method = getReflectionMethodFromSingleMethod(overload);
                    var remappedName = remapDescriptor(method);

                    if (remappedName == null) {
                        continue;
                    }

                    if (map.containsKey(remappedName)) {
                        var mergedMethod = mergeMethod.getValue().apply(map.get(remappedName), value);
                        map.remove(key);
                        map.put(remappedName, mergedMethod);
                    } else {
                        map.remove(key);
                        map.put(remappedName, value);
                    }
                }
            } catch (Exception e) {
                ClientUtilsKt.getLogger().error("Failed to remap method: {}", key, e);
            }
        }
    }

    @Unique
    private static void remapFieldEntries(Map<String, Object> map, MemberRetriever retriever) {
        if (map.isEmpty()) {
            return;
        }

        var remappedEntries = new HashMap<String, Object>();
        var iterator = map.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var key = entry.getKey();
            var value = entry.getValue();
            try {
                var member = retriever.getMember(value);
                var remapped = remapDescriptor(member);
                if (remapped != null) {
                    iterator.remove();
                    remappedEntries.put(remapped, value);
                }
            } catch (ReflectiveOperationException e) {
                ClientUtilsKt.getLogger().error("Failed to remap field: {}", key, e);
            }
        }
        map.putAll(remappedEntries);
    }

    /**
     * Skip synthetic array field/method with singleton check
     */
    @Unique
    private static final Set<String> NOT_MAPPED = Set.of(
            "com.oracle.truffle.host.HostFieldDesc$SyntheticArrayLengthField",
            "com.oracle.truffle.host.HostMethodDesc$SingleMethod$SyntheticArrayCloneMethod"
    );

    @Unique
    private static @Nullable Member getField(Object o) throws IllegalAccessException, NoSuchFieldException {
        var clazz = o.getClass();
        if (NOT_MAPPED.contains(clazz.getName())) {
            return null;
        }
        var descField = clazz.getDeclaredField("field");
        descField.setAccessible(true);
        return (Member) descField.get(o);
    }

    @Unique
    private static @Nullable String remapDescriptor(@Nullable Member member) {
        if (member == null) {
            return null;
        }

        var name = member.getName();
        var owner = member.getDeclaringClass();
        var remapped = switch (member) {
            case Method ignored -> EnvironmentRemapper.INSTANCE.remapMethod(owner, name);
            case Field ignored -> EnvironmentRemapper.INSTANCE.remapField(owner, name);
            default -> null;
        };

        if (remapped == null) {
            ClientUtilsKt.getLogger().error("Unknown member type {} of class {}", member.getClass().getName(), owner.getName());
        }

        // If the name is the same, return the original field
        if (name.equals(remapped)) {
            return null;
        }

//        ClientUtilsKt.getLogger().debug("Remapped descriptor: {} in {} to {}", name, member.getDeclaringClass().getName(), remapped);
        return remapped;
    }

}
