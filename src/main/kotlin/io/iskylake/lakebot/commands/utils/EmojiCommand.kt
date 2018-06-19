/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.iskylake.lakebot.commands.utils

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.commands.CommandCategory
import io.iskylake.lakebot.entities.extensions.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import java.time.format.DateTimeFormatter

class EmojiCommand : Command {
    override val name = "emoji"
    override val aliases = listOf("emote", "char", "character")
    override val description = "beta"
    override val category = CommandCategory.BETA
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            if (event.message.emotes.isNotEmpty()) {
                val emote = event.message.emotes[0]
                val embed = buildEmbed {
                    color { Immutable.SUCCESS }
                    thumbnail { emote.imageUrl }
                    footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                    field(true, "Name:") { emote.name }
                    field(true, "ID:") { emote.id }
                    field(true, "Animated:") { if (emote.isAnimated) "Yes" else "No" }
                    field(true, "Creation Date:") { emote.creationTime.format(DateTimeFormatter.RFC_1123_DATE_TIME).replace(" GMT", "") }
                    field(true, "Guild:") { emote.guild.name.escapeDiscordMarkdown() }
                    field(true, "Mention:") { "`${emote.asMention}`" }
                }
                event.channel.sendMessage(embed).queue()
            } else {
                val input = arguments.replace(" ", "")
                if (input.length > 15) {
                    event.sendError("Content can't be longer than 15 characters!").queue()
                } else {
                    val embed = buildEmbed {
                        color { Immutable.SUCCESS }
                        footer(event.author.effectiveAvatarUrl) { "Requested by ${event.author.tag}" }
                        for (point in input.codePoints()) {
                            val chars = Character.toChars(point)
                            var hex = point.toHex().toUpperCase()
                            while (hex.length < 4) {
                                hex = "0$hex"
                            }
                            append { "\n\\u$hex" }
                            append { " | " }
                            if (chars.size > 1) {
                                var first = chars[0].toInt().toHex().toUpperCase()
                                var second = chars[1].toInt().toHex().toUpperCase()
                                while (first.length < 4) {
                                    first = "0$first"
                                }
                                while (second.length < 4) {
                                    second = "0$second"
                                }
                                append { "\\u$first\\u$second" }
                                append { " | " }
                            }
                            append { String(chars) }
                            append { " | " }
                            append { point.toChar().name?.capitalizeAll(true) ?: "Unknown" }
                        }
                    }
                    event.channel.sendMessage(embed).queue()
                }
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
}