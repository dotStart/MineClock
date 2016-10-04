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
package rocks.spud.minecraft.mineclock.attach;

import com.sun.tools.attach.AgentInitializationException;
import com.sun.tools.attach.AgentLoadException;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides a simple JDK based attachment system.
 *
 * <strong>Note:</strong> This abstraction exists to avoid exceptions occurring when no JDK is
 * installed in the current environment.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
class ToolsMinecraftAttachment implements MinecraftAttachment {
    private final List<String> machineIds = new ArrayList<>();

    ToolsMinecraftAttachment() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void refresh() {
        VirtualMachine.list().stream()
                .filter((d) -> !this.machineIds.contains(d.id()) && d.displayName().startsWith("net.minecraft.client.main.Main"))
                .findAny()
                .ifPresent((d) -> {
                    try {
                        String path = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI().getPath();

                        if (System.getProperty("development-mode") != null) {
                            System.err.println("Relying on maven generated jar in working directory!");
                            path = Paths.get("attach/target/rocks.spud.minecraft.mineclock.attach-shaded.jar").toAbsolutePath().toString();
                        }

                        System.out.println("Attaching to VM using agent from " + path);

                        VirtualMachine vm = d.provider().attachVirtualMachine(d);
                        vm.loadAgent(path);
                        vm.detach();

                        this.machineIds.add(d.id());
                    } catch (URISyntaxException ex) {
                        System.err.println("JAR is located in invalid path: " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (AttachNotSupportedException | IOException ex) {
                        System.err.println("Attaching not supported or failed: " + ex.getMessage());
                        ex.printStackTrace();
                    } catch (AgentLoadException | AgentInitializationException ex) {
                        System.err.println("Could not load or initialize agent: " + ex.getMessage());
                        ex.printStackTrace();
                    }
                });
    }
}
