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
import io.ilakeful.lakebot.WAITER_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.WaiterProcess
import io.ilakeful.lakebot.entities.extensions.*

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class GuessGameCommand : Command {
    override val name = "guess"
    override val aliases = listOf("guessnum", "guessgame", "guessnumber", "guess-the-number", "guess-game")
    override val description = "The command launching a game in which you must guess the number in the range from 5 through the specified number (the limit is 500000). In order to kill the game, type in \"exit\"."
    override val usage = fun(prefix: String) = "${super.usage(prefix)} <limit>"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (args.isNotEmpty()) {
            if (args[0].isInt) {
                val max = args[0].toInt()
                if (max in 5..500000) {
                    val toGuess = Int.random(1..max)
                    val round = 1
                    event.channel.sendMessage(buildEmbed {
                        author { "Attempt #$round" }
                        description { "Let's go!" }
                        footer { "Type in \"exit\" to kill the process" }
                        color { Immutable.SUCCESS }
                    }).await {
                        val process = WaiterProcess(mutableListOf(event.author), event.textChannel, this)
                        WAITER_PROCESSES += process
                        awaitInt(round, toGuess, event, process)
                    }
                } else {
                    event.sendFailure("The number is not in the correct range!").queue()
                }
            } else {
                event.sendFailure("You must specify a maximum number!").queue()
            }
        } else {
            event.sendFailure("You specified no content!").queue()
        }
    }
    private suspend fun awaitInt(
            round: Int,
            toGuess: Int,
            event: MessageReceivedEvent,
            process: WaiterProcess
    ) {
        var attempt = round
        val content = event.channel.awaitMessage(event.author)?.contentRaw
        if (content !== null) {
            when {
                content.isInt -> {
                    val input = content.toInt()
                    when {
                        input == toGuess -> {
                            event.channel.sendSuccess("GG! Game took $attempt attempt${if (attempt == 1) "" else "s"}!").queue()
                            WAITER_PROCESSES -= process
                        }
                        input > toGuess -> {
                            attempt++
                            event.channel.sendMessage(buildEmbed {
                                color { Immutable.FAILURE }
                                description { "It's too large!" }
                                author { "Attempt #$attempt" }
                            }).await { awaitInt(attempt, toGuess, event, process) }
                        }
                        input < toGuess -> {
                            attempt++
                            event.channel.sendMessage(buildEmbed {
                                color { Immutable.FAILURE }
                                description { "It's too small!" }
                                author { "Attempt #$attempt" }
                            }).await { awaitInt(attempt, toGuess, event, process) }
                        }
                    }
                }
                content.toLowerCase() == "exit" -> {
                    WAITER_PROCESSES -= process
                    event.sendSuccess("Process successfully stopped!").queue()
                }
                else -> event.sendFailure("Try again!").await { awaitInt(attempt, toGuess, event, process) }
            }
        } else {
            WAITER_PROCESSES -= process
            event.sendFailure("Time is up!").queue()
        }
    }
}