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

import javax.annotation.Nonnull;

/**
 * Provides an interface for handling attachment to Minecraft VMs.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public interface MinecraftAttachment {

    /**
     * Checks whether the attachment is available in the current environment.
     *
     * @return true if available, false otherwise.
     */
    static boolean isAvailable() {
        try {
            Class.forName("com.sun.tools.attach.VirtualMachine");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Creates a new attachment instance.
     *
     * @return an attachment.
     */
    @Nonnull
    static MinecraftAttachment getAttachment() {
        return new ToolsMinecraftAttachment();
    }

    /**
     * Attaches to and instruments all new Minecraft instances.
     */
    void refresh();
}
