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

package io.ilakeful.lakebot.commands.`fun`

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class SayCommand : Command {
    override val name = "say"
    override val aliases = listOf("announce")
    override val description = "The command sending your message on behalf of LakeBot"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <content>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            event.channel.sendEmbed {
                author(event.author.asTag) { event.author.effectiveAvatarUrl }
                color { Immutable.SUCCESS }
                image { event.message.attachments.firstOrNull { it.isImage }?.url }
                description { arguments }
            }.queue {
                try {
                    event.message.delete().queue()
                } catch (ignored: Exception) {
                }
            }
        } else {
            if (event.message.attachments.any { it.isImage }) {
                event.channel.sendEmbed {
                    image { event.message.attachments.first { it.isImage }.url }
                    author(event.author.asTag) { event.author.effectiveAvatarUrl }
                    color { Immutable.SUCCESS }
                }.queue {
                    try {
                        event.message.delete().queue()
                    } catch (ignored: Exception) {
                    }
                }
            } else {
                event.channel.sendFailure("You haven't specified any arguments!").queue()
            }
        }
    }
}