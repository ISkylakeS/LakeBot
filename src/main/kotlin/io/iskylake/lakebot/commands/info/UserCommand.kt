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

package io.iskylake.lakebot.commands.info

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.commands.CommandCategory
import io.iskylake.lakebot.entities.EventWaiter
import io.iskylake.lakebot.entities.extensions.*

import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent

import java.util.concurrent.TimeUnit

class UserCommand : Command {
    override val name = "user"
    override val description = "N/A"
    override val category = CommandCategory.BETA
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw
        if (arguments !== null) {
            when {
                event.message.mentionedMembers.isNotEmpty() -> userMenu(event, event.message.mentionedMembers[0])
                event.guild.searchMembers(arguments).isNotEmpty() -> {
                    val list = event.guild.searchMembers(arguments).take(5)
                    if (list.size > 1) {
                        event.channel.sendMessage(buildEmbed {
                            color { Immutable.SUCCESS }
                            author { "Select The User:" }
                            for (member in list) {
                                appendln { "\u2022 ${member.user.tag}" }
                            }
                        }).await {
                            USERS_WITH_PROCESSES += event.author
                            selectUser(event, it, list)
                        }
                    } else {
                        userMenu(event, list[0])
                    }
                }
                args[0] matches Regex.DISCORD_ID && event.guild.getMemberById(args[0]) !== null -> {
                    val member = event.guild.getMemberById(args[0])
                    userMenu(event, member)
                }
                else -> event.sendError("Couldn't find that user!").queue()
            }
        } else {
            invoke(event, arrayOf(event.author.asMention))
        }
    }
    suspend fun userMenu(event: MessageReceivedEvent, member: Member) = event.channel.sendMessage(buildEmbed {
        color { Immutable.SUCCESS }
        author { "Select The Action:" }
        description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Avatar" }
    }).await {
        USERS_WITH_PROCESSES += event.author
        it.addReaction("\u0031\u20E3").complete()
        it.addReaction("\u0032\u20E3").complete()
        it.addReaction("\u274C").complete()
        val e = EventWaiter.receiveEventRaw<MessageReactionAddEvent>(1, TimeUnit.MINUTES) { e ->
            val name = e.reactionEmote.name
            val condition = name == "\u0031\u20E3" || name == "\u0032\u20E3" || name == "\u274C"
            e.messageIdLong == it.idLong && e.user == event.author && condition
        }
        if (e !== null) {
            when (e.reactionEmote.name) {
                "\u0031\u20E3" -> {
                    it.delete().queue()
                    USERS_WITH_PROCESSES -= event.author
                }
                "\u0032\u20E3" -> {
                    it.delete().queue()
                    AvatarCommand()(event, arrayOf(member.user.asMention))
                    USERS_WITH_PROCESSES -= event.author
                }
                "\u274C" -> {
                    it.delete().queue()
                    event.sendSuccess("Process successfully stopped!").queue()
                    USERS_WITH_PROCESSES -= event.author
                }
            }
        } else {
            event.sendError("Time is up!").queue()
            USERS_WITH_PROCESSES -= event.author
        }
    }
    suspend fun selectUser(event: MessageReceivedEvent, msg: Message, members: List<Member>) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    userMenu(event, members[c.toInt() - 1])
                    USERS_WITH_PROCESSES -= event.author
                } else {
                    event.sendError("Try again!").await { selectUser(event, msg, members) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendError("Try again!").await { selectUser(event, msg, members) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendError("Time is up!").queue()
        }
    }
}