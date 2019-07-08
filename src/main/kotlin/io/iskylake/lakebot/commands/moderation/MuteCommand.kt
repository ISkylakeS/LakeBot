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

package io.iskylake.lakebot.commands.moderation

import io.iskylake.lakebot.Immutable
import io.iskylake.lakebot.USERS_WITH_PROCESSES
import io.iskylake.lakebot.commands.Command
import io.iskylake.lakebot.entities.extensions.*
import io.iskylake.lakebot.utils.TimeUtils

import net.dv8tion.jda.core.Permission.*
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

import java.util.Timer

import kotlin.concurrent.schedule

class MuteCommand : Command {
    override val name = "mute"
    override val description = "The command that mutes the specified member"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <user> <time> <reason>"
    override val examples = fun(prefix: String) = mapOf("$prefix$name ISkylake 1w12h flood" to "mutes ISkylake for 7.5 days because of flood")
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val arguments = event.argsRaw?.split(Regex("\\s+"), 3) ?: emptyList()
        if (arguments.isNotEmpty()) {
            when {
                MANAGE_ROLES !in event.member.permissions -> event.sendError("You don't have required permissions!").queue()
                MANAGE_ROLES !in event.guild.selfMember.permissions -> event.sendError("LakeBot doesn't have required permissions!").queue()
                arguments.size in 1..2 -> event.sendError("You didn't specify reason or time!").queue()
                "([1-9][0-9]*)([smhw])".toRegex().findAll(arguments[1]).toList().isEmpty() -> event.sendError("That's not a valid format of time!").queue()
                TimeUtils.parseTime(arguments[1]) > TimeUtils.weeksToMillis(2) -> event.sendError("You can indicate the time until 2 weeks!").queue()
                !event.guild.isMuteRoleEnabled -> event.sendError("The mute role isn't enabled!").queue()
                else -> {
                    val time = TimeUtils.parseTime(arguments[1])
                    val reason = arguments[2]
                    when {
                        arguments[0] matches Regex.DISCORD_USER -> {
                            val member = event.guild.getMemberById(arguments[0].replace(Regex.DISCORD_USER, "\$1"))
                            if (member !== null) {
                                muteUser(event, time, reason) { member }
                            } else {
                                event.sendError("Couldn't find that user!").queue()
                            }
                        }
                        event.guild.searchMembers(arguments[0]).isNotEmpty() -> {
                            val list = event.guild.searchMembers(arguments[0]).take(5)
                            if (list.size > 1) {
                                event.channel.sendMessage(buildEmbed {
                                    color { Immutable.SUCCESS }
                                    author("Select The User:") { event.selfUser.effectiveAvatarUrl }
                                    for ((index, member) in list.withIndex()) {
                                        appendln { "${index + 1}. ${member.user.tag}" }
                                    }
                                    footer { "Type in \"exit\" to kill the process" }
                                }).await {
                                    USERS_WITH_PROCESSES += event.author
                                    selectUser(event, it, list, time, reason)
                                }
                            } else {
                                muteUser(event, time, reason) { list.first() }
                            }
                        }
                        arguments[0] matches Regex.DISCORD_ID && event.guild.getMemberById(arguments[0]) !== null -> {
                            val member = event.guild.getMemberById(arguments[0])
                            muteUser(event, time, reason) { member }
                        }
                        else -> event.sendError("Couldn't find that user!").queue()
                    }
                }
            }
        } else {
            event.sendError("You specified no content!").queue()
        }
    }
    suspend fun muteUser(event: MessageReceivedEvent, time: Long, reason: String, lazyMember: () -> Member) {
        val member = lazyMember()
        val user = member.user
        if (user != event.author && event.member.canInteract(member) && event.guild.selfMember.canInteract(member)) {
            event.channel.sendConfirmation("Are you sure you want to mute this member?").await {
                val confirmation = it.awaitNullableConfirmation(event.author)
                if (confirmation !== null) {
                    it.delete().queue()
                    if (confirmation) {
                        val timeAsText = TimeUtils.asText(time)
                        val role = event.guild.getRoleById(event.guild.muteRole)
                        event.guild.putMute(user, event.author, reason, time)
                        val embed = buildEmbed {
                            color { Immutable.SUCCESS }
                            author("LakeMute!") { event.selfUser.effectiveAvatarUrl }
                            field(true, "Guild:") { event.guild.name.escapeDiscordMarkdown() }
                            field(true, "Moderator:") { event.author.tag.escapeDiscordMarkdown() }
                            field(reason.length < 27, "Reason:") { reason }
                            field(timeAsText.length < 27, "Time:") { timeAsText }
                            timestamp()
                        }
                        event.sendSuccess("${user.tag} was successfully muted!").queue()
                        event.guild.controller.addSingleRoleToMember(member, role).queue()
                        user.privateChannel.sendMessage(embed).queue(null) { _ -> }
                        val timer = Timer()
                        timer.schedule(time) {
                            try {
                                if (role in member.roles) {
                                    event.guild.controller.removeSingleRoleFromMember(member, role).queue()
                                }
                            } catch (ignored: Exception) {
                            } finally {
                                event.guild.clearMute(user)
                            }
                        }
                    } else {
                        event.sendSuccess("Process was canceled!").queue()
                    }
                } else {
                    it.delete().queue()
                    event.sendError("Time is up!").queue()
                }
            }
        } else {
            event.sendError("You can't mute that user!").queue()
        }
    }
    suspend fun selectUser(event: MessageReceivedEvent, msg: Message, members: List<Member>, time: Long, reason: String) {
        val c = event.channel.awaitMessage(event.author)?.contentRaw
        if (c !== null) {
            if (c.isInt) {
                if (c.toInt() in 1..members.size) {
                    msg.delete().queue()
                    muteUser(event, time, reason) { members[c.toInt() - 1] }
                    USERS_WITH_PROCESSES -= event.author
                } else {
                    event.sendError("Try again!").await { selectUser(event, msg, members, time, reason) }
                }
            } else if (c.toLowerCase() == "exit") {
                msg.delete().queue()
                USERS_WITH_PROCESSES -= event.author
                event.sendSuccess("Process successfully stopped!").queue()
            } else {
                event.sendError("Try again!").await { selectUser(event, msg, members, time, reason) }
            }
        } else {
            msg.delete().queue()
            USERS_WITH_PROCESSES -= event.author
            event.sendError("Time is up!").queue()
        }
    }
}