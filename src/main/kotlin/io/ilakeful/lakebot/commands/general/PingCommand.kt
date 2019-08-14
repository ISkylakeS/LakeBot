/*
 * Copyright 2017-2019 (c) Alexander "ILakeful" Shevchenko
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

package io.ilakeful.lakebot.commands.general

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class PingCommand : Command {
    override val name = "ping"
    override val aliases = listOf("delay", "response")
    override val description = "The command sending LakeBot's current response time separately from the statistics"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = event.jda.restPing.queue {
        event.channel.sendEmbed {
            field(true, "Rest Ping") { "$it ms" }
            field(true, "WebSocket Ping") { "${event.jda.gatewayPing} ms" }
            color { Immutable.SUCCESS }
        }.queue()
    }
}