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
package rocks.spud.minecraft.mineclock.attach.agent.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provides a message representation in order to sanely transmit the relevant information between
 * the server and the client application.
 *
 * @author <a href="mailto:johannesd@torchmind.com">Johannes Donath</a>
 */
public class ClockMessage {
    private final long worldTime;
    private final long worldAge;
    private final boolean raining;

    @JsonCreator
    public ClockMessage(@JsonProperty("worldTime") long worldTime, @JsonProperty("worldAge") long worldAge, @JsonProperty("raining") boolean raining) {
        this.worldTime = worldTime;
        this.worldAge = worldAge;
        this.raining = raining;
    }

    public long getWorldTime() {
        return this.worldTime;
    }

    public long getWorldAge() {
        return this.worldAge;
    }

    public boolean isRaining() {
        return this.raining;
    }
}
