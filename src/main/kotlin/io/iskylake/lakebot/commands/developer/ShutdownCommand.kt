/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.iskylake.lakebot.commands.developer

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.selfUser
import io.iskylake.lakebot.entities.extensions.sendConfirmation
import io.iskylake.lakebot.entities.extensions.sendError
import io.iskylake.lakebot.entities.extensions.sendSuccess

import kotlin.system.exitProcess

import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class ShutdownCommand : Command {
    override val name = "shutdown"
    override val description = "The command that shutdowns LakeBot"
    override fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        event.sendConfirmation("Are you sure want to shutdown ${event.selfUser.name}?").queue {
            val confirmation = EventWaiter.awaitNullableConfirmation(it, event.author)
            if (confirmation !== null) {
                if (confirmation) {
                    Immutable.LOGGER.info("LakeBot is going offline!")
                    event.channel.sendSuccess("Successfully disconnected!").queue { _ ->
                        it.delete().queue({
                            event.jda.shutdownNow()
                            exitProcess(0)
                        }) {
                            event.jda.shutdownNow()
                            exitProcess(0)
                        }
                    }
                } else {
                    it.delete().queue()
                    event.sendSuccess("Successfully cancelled!").queue()
                }
            } else {
                it.delete().queue()
                event.sendError("Time is up!").queue()
            }
        }
    }
}