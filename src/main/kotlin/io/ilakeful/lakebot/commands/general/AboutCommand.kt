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

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.handlers.CommandHandler
import io.ilakeful.lakebot.utils.TimeUtils.asText

import io.iskylake.weather.LakeWeatherInfo

import net.dv8tion.jda.api.JDAInfo
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class AboutCommand : Command {
    override val name = "about"
    override val aliases = listOf("info", "stats")
    override val description = "The command sending complete information about LakeBot"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        event.jda.restPing.queue { ping ->
            event.jda.retrieveApplicationInfo().queue { appInfo ->
                val embed = buildEmbed {
                    color { Immutable.SUCCESS }
                    author(event.selfUser.name) { event.selfUser.effectiveAvatarUrl }
                    thumbnail { event.selfUser.effectiveAvatarUrl }
                    footer { "Last Reboot" }
                    timestamp { event.jda.startDate }
                    description {
                        val count = event.jda.guildCache.count { it.selfMember.isConnected }
                        val end = if (count > 1) "s" else ""
                        val description = if (count < 1) "Currently isn't streaming music on any of all servers." else "Currently streaming music on **$count server$end**."
                        """${event.selfUser.name} is the next generation of [KabyBot](https://github.com/KabyBot/KabyBot). It is Discord multi-purpose bot coded in 100% [Kotlin](https://kotlinlang.org/).
                            |$description
                            |
                            |**[Invite Link](https://discordapp.com/oauth2/authorize?client_id=${event.selfUser.id}&permissions=${Immutable.PERMISSIONS}&scope=bot)** • **[GitHub Repository](${Immutable.GITHUB_REPOSITORY})**
                            |**Creator/Developer**: ${appInfo.owner.asTag}
                            |**Commands**: ${CommandHandler.registeredCommands.size}
                            |**Bot Version**: ${Immutable.VERSION}
                            |**JDA Version**: ${JDAInfo.VERSION}
                            |**LakeWeather Version**: ${LakeWeatherInfo.VERSION}
                            |**LavaPlayer Version**: ${PlayerLibrary.VERSION}
                            |**Java Version**: ${System.getProperty("java.version") ?: "Unknown"}
                            |**Kotlin Version**: ${System.getProperty("kotlin.version") ?: KotlinVersion.CURRENT}
                            |**Guilds**: ${event.jda.guildCache.size()}
                            |**Users**: ${event.jda.userCache.size()}
                            |**Active Threads**: ${Thread.activeCount()}
                            |**Uptime**: ${asText(event.jda.uptime)}
                            |**Ping**: $ping ms
                        """.trimMargin()
                    }
                }
                event.channel.sendMessage(embed).queue()
            }
        }
    }
}