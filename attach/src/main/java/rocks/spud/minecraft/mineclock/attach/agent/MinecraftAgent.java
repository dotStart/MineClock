/*
 * Copyright 2016 Johannes Donath <johannesd@torchmind.com>
 * and other copyright owners as documented in the project's IP log.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * 	http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rocks.spud.minecraft.mineclock.attach.agent;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

import rocks.spud.minecraft.mineclock.attach.agent.common.ClockMessage;
import rocks.spud.minecraft.mineclock.attach.agent.common.ClockProtocol;

/**
 * Provides an agent type which is loaded into a running instance of Minecraft sometime after its
 * launch in order to access its variables from the outside.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class MinecraftAgent {
    private static ClockProtocol protocol;
    private static long lastUpdate = 0;

    public static void agentmain(@Nonnull String args, @Nonnull Instrumentation instrumentation) {
        protocol = new ClockProtocol();
        protocol.listen();

        try {
            ClassPool pool = ClassPool.getDefault();

            try {
                Class.forName("net.minecraftforge.MinecraftForge");
            } catch (ClassNotFoundException ex) {
                final CtClass type = pool.get("net.minecraft.client.ClientBrandRetriever");
                final CtMethod tickMethod = type.getDeclaredMethod("getClientModName");
                tickMethod.setBody("return \"MineClock\";");
                instrumentation.redefineClasses(new ClassDefinition(Class.forName("net.minecraft.client.ClientBrandRetriever"), type.toBytecode()));
                type.detach();
            }

            // 1.10 & 1.9.4 & 1.9 & 1.8.9 & 1.8
            if (matches("bcx", "z") || matches("bcd", "z") || matches("bcf", "z") || matches("ave", "A") || matches("bsu", "z")) {
                final String name;
                String tickName = "t";
                String worldTimeName = "Q";
                String totalWorldTimeName = "P";
                String isRainingName = "W";

                if (matches("bcx", "z")) {
                    name = "bcx";
                } else if (matches("bcd", "z")) {
                    name = "bcd";
                } else if (matches("bcf", "z")) {
                    name = "bcf";
                } else if (matches("ave", "A")) {
                    name = "ave";
                    tickName = "s";
                    worldTimeName = "L";
                    totalWorldTimeName = "K";
                    isRainingName = "S";
                } else {
                    name = "bsu";
                    tickName = "r";
                    worldTimeName = "L";
                    totalWorldTimeName = "K";
                    isRainingName = "S";
                }

                final CtClass type = pool.get(name);
                final CtMethod tickMethod = type.getDeclaredMethod(tickName);
                tickMethod.insertBefore(
                        // @formatter:off
                    "if (this.h != null && this.h.e() != null) {" +
                            "rocks.spud.minecraft.mineclock.attach.agent.MinecraftAgent.pushUpdate(this.h.e()." + worldTimeName + "(), this.h.e()." + totalWorldTimeName + "(), this.h.e()." + isRainingName + "());" +
                    "}"
                    // @formatter:on
                );

                instrumentation.redefineClasses(new ClassDefinition(Class.forName(name), type.toBytecode()));
                type.detach();
            }
            // 1.7.10
            else if (matches("bao", "B")) {
                final CtClass type = pool.get("bao");
                final CtMethod tickMethod = type.getDeclaredMethod("p");
                tickMethod.insertBefore(
                        // @formatter:off
                        "if (this.h != null && this.h.o != null) {" +
                                "rocks.spud.minecraft.mineclock.attach.agent.MinecraftAgent.pushUpdate(this.h.o.J(), this.h.o.I(), this.h.o.Q());" +
                        "}"
                        // @formatter:on
                );

                instrumentation.redefineClasses(new ClassDefinition(Class.forName("bao"), type.toBytecode()));
                type.detach();
            }
        } catch (CannotCompileException | ClassNotFoundException | NotFoundException | IOException | UnmodifiableClassException ex) {
            System.err.println("Could not transform tick loop: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    /**
     * Checks whether the supplied type name and method name match the requirements to be a
     * reference to the Minecraft main class.
     *
     * @param typeName   a type name.
     * @param methodName a method name.
     * @return true if matches, false otherwise.
     */
    private static boolean matches(@Nonnull String typeName, @Nonnull String methodName) {
        try {
            Class<?> type = Class.forName(typeName);
            Method method = type.getDeclaredMethod(methodName);

            return Modifier.isStatic(method.getModifiers()) && method.getReturnType().equals(type);
        } catch (ClassNotFoundException | NoSuchMethodException ex) {
            return false;
        }
    }

    /**
     * Pushes a new update to the server.
     */
    public static void pushUpdate(@Nonnegative long worldTime, @Nonnegative long worldAge, boolean raining) {
        if (lastUpdate + 100 < worldAge) {
            protocol.push(new ClockMessage(worldTime, worldAge, raining));
            lastUpdate = worldAge;
        }
    }
}
