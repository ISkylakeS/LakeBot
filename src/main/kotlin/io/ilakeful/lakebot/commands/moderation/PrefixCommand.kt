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

package io.ilakeful.lakebot.commands.moderation

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class PrefixCommand : Command {
    override val name = "prefix"
    override val aliases = listOf("setprefix", "set-prefix")
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <prefix>"
    override val description = "The command changing the server's command prefix"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) = if (Permission.MANAGE_SERVER in event.member!!.permissions || event.author.isLBDeveloper) {
        if (args.isNotEmpty()) {
            val newPrefix = args[0].toLowerCase()
            if (newPrefix.length > 5) {
                event.channel.sendFailure("The argument is unable to be used as a command prefix!").queue()
            } else {
                event.channel.sendConfirmation("Are you sure you want to change prefix for this server?").await {
                    val confirmation = it.awaitNullableConfirmation(event.author)
                    if (confirmation !== null) {
                        it.delete().queue()
                        if (confirmation) {
                            event.guild.setPrefix(newPrefix)
                            event.channel.sendSuccess("Now the command prefix is $newPrefix").queue()
                        } else {
                            event.channel.sendSuccess("Successfully canceled!").queue()
                        }
                    } else {
                        event.channel.sendFailure("Time is up!").queue()
                        it.delete().queue()
                    }
                }
            }
        } else {
            event.channel.sendFailure("You haven't specified any arguments!").queue()
        }
    } else {
        event.channel.sendFailure("You do not have permissions for executing the command!").queue()
    }
}