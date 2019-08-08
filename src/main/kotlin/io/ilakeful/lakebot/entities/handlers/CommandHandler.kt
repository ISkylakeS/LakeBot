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

package io.ilakeful.lakebot.entities.handlers

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext

import net.dv8tion.jda.api.entities.MessageType
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

import kotlin.coroutines.CoroutineContext

object CommandHandler : CoroutineContext by newFixedThreadPoolContext(3, "Command-Thread") {
    val registeredCommands = mutableListOf<Command>()
    private val cooldowns = mutableMapOf<String, OffsetDateTime>()
    private fun getPriority(actual: String, expected: Command): Int = when (actual) {
        expected.name -> 2
        in expected.aliases -> 1
        else -> 0
    }
    operator fun iterator() = registeredCommands.iterator()
    operator fun contains(command: String): Boolean = this[command] !== null && this[command] in registeredCommands
    operator fun contains(command: Command): Boolean = command in registeredCommands
    operator fun get(id: Long): Command? = registeredCommands.firstOrNull { it.id == id }
    operator fun get(name: String): Command? = registeredCommands.filter {
        getPriority(name.toLowerCase(), it) > 0
    }.sortedBy { getPriority(name.toLowerCase(), it) }.firstOrNull()
    operator fun plusAssign(command: Command) {
        registeredCommands += command
    }
    operator fun minusAssign(command: Command) {
        registeredCommands -= command
    }
    internal operator fun invoke(event: MessageReceivedEvent) {
        if (!event.author.isBot && !event.author.isFake && event.channelType.isGuild && event.message.type == MessageType.DEFAULT) {
            if (event.message.contentRaw == event.selfUser.asMention) {
                event.jda.retrieveApplicationInfo().queue {
                    event.channel.sendEmbed {
                        color { Immutable.SUCCESS }
                        author("Welcome!") { event.selfUser.effectiveAvatarUrl }
                        description {
                            "Hello! Welcome to ${event.selfUser.name}! Let's get started! " +
                                    "To get documentation, type in the \"${event.guild.prefix}help\" command. " +
                                    "If you have any questions, try to contact ${it.owner.asTag}!"
                        }
                    }.queue()
                }
                return
            }
            if (event.message.contentRaw.startsWith(event.guild.prefix, true)) {
                val args = event.message.contentRaw.split("\\s".toRegex(), 2)
                val command = this[args[0].toLowerCase().substring(event.guild.prefix.length)]
                if (command !== null) {
                    when {
                        command.isDeveloper && !event.author.isLBDeveloper -> event.sendFailure("You don't have permissions to execute this command!").queue()
                        event.author.lakeBan !== null -> event.sendFailure("${event.author.asMention}, sorry! You can't execute this command because you got LakeBan for `${event.author.lakeBan?.getString("reason")}`!").queue()
                        else -> {
                            val processOfTheUser = WAITER_PROCESSES.firstOrNull { event.author in it.users && event.textChannel == it.channel }
                            if (processOfTheUser === null) {
                                CoroutineScope(this).launch {
                                    if (command.cooldown > 0) {
                                        val key = "${command.name}|${event.author.id}"
                                        val time = getRemainingCooldown(key)
                                        if (time > 0) {
                                            val error: String? = getCooldownError(time)
                                            if (error !== null) {
                                                event.channel.sendFailure(error).queue()
                                            } else {
                                                if (args.size > 1) {
                                                    command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                                                } else {
                                                    command(event, emptyArray())
                                                }
                                            }
                                        } else {
                                            applyCooldown(key, command.cooldown)
                                            if (args.size > 1) {
                                                command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                                            } else {
                                                command(event, emptyArray())
                                            }
                                        }
                                    } else {
                                        if (args.size > 1) {
                                            command(event, args[1].split("\\s+".toRegex()).toTypedArray())
                                        } else {
                                            command(event, emptyArray())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private fun getRemainingCooldown(name: String): Long {
        if (name in cooldowns) {
            val time: Long = OffsetDateTime.now().until(cooldowns[name], ChronoUnit.SECONDS)
            if (time <= 0) {
                cooldowns -= name
                return 0
            }
            return time
        }
        return 0
    }
    private fun applyCooldown(name: String, seconds: Long) {
        cooldowns += name to OffsetDateTime.now().plusSeconds(seconds)
    }
    private fun getCooldownError(time: Long): String? {
        if (time <= 0) {
            return null
        }
        return "Please wait $time ${if (time == 1L) "second" else "seconds"} before launching command again!"
    }
}