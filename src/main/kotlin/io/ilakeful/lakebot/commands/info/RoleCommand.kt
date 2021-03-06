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

package io.ilakeful.lakebot.commands.info

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.commands.utils.ColorCommand
import io.ilakeful.lakebot.entities.EventWaiter
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.ImageUtils

import net.dv8tion.jda.api.entities.*
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent

import org.ocpsoft.prettytime.PrettyTime

import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.TimeUnit

class RoleCommand : Command {
    override val name = "role"
    override val aliases = listOf("roleinfo", "rolemenu", "role-info", "role-menu")
    override val description = "The command sending complete information about the specified role"
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <role>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        event.retrieveRoles(
                command = this,
                massMention = false,
                predicate = { !it.isPublicRole }
        ) {
            roleMenu(event, it)
        }
    }
    inline fun roleInfo(author: User, lazy: () -> Role) = buildEmbed {
        val role = lazy()
        val prettyTime = PrettyTime()
        color { role.color }
        if (role.color !== null) {
            thumbnail { "attachment://${role.color!!.rgb.toHex().takeLast(6)}.png" }
        }
        footer(author.effectiveAvatarUrl) { "Requested by ${author.asTag}" }
        timestamp()
        field(true, "Name:") { role.name.escapeDiscordMarkdown() }
        field(true, "ID:") { role.id }
        field(true, "Mention:") { "`${role.asMention}`" }
        field(true, "Members:") { "${role.members.size}" }
        field(true, "Position:") { "#${role.position + 1}" }
        field(true, "Hoisted:") { role.isHoisted.asString() }
        field(true, "Mentionable:") { role.isMentionable.asString() }
        field(true, "Color:") { role.color?.rgb?.toHex()?.takeLast(6)?.prepend("#") ?: "Default" }
        field(true, "Creation Date:") {
            val date = role.timeCreated
            val formatted = date.format(DateTimeFormatter.RFC_1123_DATE_TIME).removeSuffix("GMT").trim()
            val ago = prettyTime.format(Date.from(date.toInstant()))
            "$formatted ($ago)"
        }
        if (role.keyPermissions.isNotEmpty()) {
            field(title = "Key Permissions:") { role.keyPermissions.map { it.getName() }.joinToString() }
        }
    }
    suspend fun roleMenu(event: MessageReceivedEvent, role: Role) = if (role.color === null) {
        val embed = roleInfo(event.author) { role }
        event.channel.sendMessage(embed).queue()
    } else {
        event.channel.sendEmbed {
            color { Immutable.SUCCESS }
            author { "Select Action:" }
            description { "\u0031\u20E3 \u2014 Get Information\n\u0032\u20E3 \u2014 Get Color Information" }
        }.await {
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
                        val embed = roleInfo(event.author) { role }
                        val color = ImageUtils.getColorImage(role.color!!, 250, 250)
                        event.channel.sendMessage(embed).addFile(color, "${role.color!!.rgb.toHex().takeLast(6)}.png").queue()
                    }
                    "\u0032\u20E3" -> {
                        it.delete().queue()
                        ColorCommand()(event, arrayOf(role.color!!.rgb.toHex().takeLast(6)))
                    }
                    "\u274C" -> {
                        it.delete().queue()
                        event.channel.sendSuccess("Successfully canceled!").queue()
                    }
                }
            } else {
                it.delete().queue()
                event.channel.sendFailure("Time is up!").queue()
            }
        }
    }
}