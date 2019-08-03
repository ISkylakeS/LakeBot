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

import com.markozajc.akiwrapper.Akiwrapper
import com.markozajc.akiwrapper.Akiwrapper.Answer
import com.markozajc.akiwrapper.AkiwrapperBuilder
import com.markozajc.akiwrapper.core.entities.Server

import io.ilakeful.lakebot.Immutable
import io.ilakeful.lakebot.USERS_WITH_PROCESSES
import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.entities.handlers.CommandHandler

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class AkinatorCommand : Command {
    val languages: Map<String, Server.Language> = mapOf(
            "russian" to Server.Language.RUSSIAN,
            "english" to Server.Language.ENGLISH,
            "arabic" to Server.Language.ARABIC,
            "chinese" to Server.Language.CHINESE,
            "dutch" to Server.Language.DUTCH,
            "french" to Server.Language.FRENCH,
            "german" to Server.Language.GERMAN,
            "hebrew" to Server.Language.HEBREW,
            "italian" to Server.Language.ITALIAN,
            "japanese" to Server.Language.JAPANESE,
            "korean" to Server.Language.KOREAN,
            "polish" to Server.Language.POLISH,
            "portuguese" to Server.Language.PORTUGUESE,
            "spanish" to Server.Language.SPANISH,
            "turkish" to Server.Language.TURKISH
    )
    override val name = "akinator"
    override val description = """The command launching Akinator game.
        |After the game launch you're supposed to answer with **YES**, **NO**, **DONT KNOW**, **PROBABLY**, or **PROBABLY NOT** until Akinator guesses your character. Type in **BACK** in order to return to previous question. Type in "exit" in order to terminate the current game. Type in "aliases" in order to get more ways of answering.
    """.trimMargin()
    override val usage = { it: String -> "${super.usage(it)} <language (optional)>" }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        try {
            val language = languages[args.firstOrNull()?.toLowerCase() ?: "english"] ?: throw IllegalArgumentException("That's invalid language!")
            val api = AkiwrapperBuilder()
                    .setLocalization(language)
                    .setFilterProfanity(!event.textChannel.isNSFW)
                    .build()
            event.channel.sendMessage(buildEmbed {
                author { "Question #${api.currentQuestion!!.step + 1}:" }
                description { api.currentQuestion!!.question.capitalize() }
                footer { "Type in \"exit\" to kill the process" }
                color { Immutable.SUCCESS }
            }).await {
                USERS_WITH_PROCESSES += event.author
                awaitAnswer(event, api)
            }
        } catch (e: Exception) {
            event.sendFailure(e.message ?: "Something went wrong!").queue()
        }
    }
    private suspend fun awaitAnswer(event: MessageReceivedEvent, wrapper: Akiwrapper) {
        val content = event.channel.awaitMessage(event.author)?.contentRaw?.toLowerCase()
        if (content !== null) {
            val possibleAnswers = setOf("help", "back", "b", "return", "alias", "aliases", "y", "n", "d", "p", "pn", "dk", "perhaps", "likely", "unlikely", "nope", "yes", "yup", "yep", "yeah", "yea", "no", "no u", "nah", "dont know", "dunno", "idk", "don't know", "probably", "probably not", "exit")
            if (content in possibleAnswers) {
                when (content) {
                    "exit" -> event.sendConfirmation("Are you sure you want to exit?").await {
                        val bool = it.awaitNullableConfirmation(event.author)
                        if (bool !== null) {
                            if (bool) {
                                it.delete().queue()
                                event.sendSuccess("Successfully stopped!").queue()
                                USERS_WITH_PROCESSES -= event.author
                            } else {
                                it.delete().queue()
                                event.sendSuccess("Let's go!").await {
                                    awaitAnswer(event, wrapper)
                                }
                            }
                        }
                    }
                    "alias", "aliases" -> event.sendMessage(buildEmbed {
                        color { Immutable.SUCCESS }
                        author("Aliases For Answers:") { event.selfUser.effectiveAvatarUrl }
                        field(title = "Yes:") { "\u2022 Yeah\n\u2022 Yep\n\u2022 Yup\n\u2022 Y\n\u2022 Yea" }
                        field(title = "No:") { "\u2022 Nah\n\u2022 No u\n\u2022 Nope\n\u2022 N" }
                        field(title = "Don't Know:") { "\u2022 Don't Know\n\u2022 Idk\n\u2022 Dk\n\u2022 Dunno\n\u2022 D" }
                        field(title = "Probably:") { "\u2022 P\n\u2022 Perhaps\n\u2022 Likely" }
                        field(title = "Probably Not:") { "\u2022 PN\n\u2022 Unlikely" }
                        field(title = "Back:") { "\u2022 Return\n\u2022 B" }
                    }).await { awaitAnswer(event, wrapper) }
                    "help" -> {
                        CommandHandler["help"]!!(event, arrayOf(name))
                        awaitAnswer(event, wrapper)
                    }
                    else -> {
                        if (wrapper.currentQuestion !== null && wrapper.guesses.filter { it.probability > 0.825 }.isEmpty()) {
                            val answer = when (content) {
                                "yes", "yeah", "yep", "yup", "y", "yea" -> Answer.YES
                                "no", "nah", "no u", "n", "nope" -> Answer.NO
                                "dont know", "idk", "dunno", "don't know", "d" -> Answer.DONT_KNOW
                                "probably", "p", "perhaps", "likely" -> Answer.PROBABLY
                                "probably not", "pn", "unlikely" -> Answer.PROBABLY_NOT
                                else -> Answer.DONT_KNOW
                            }
                            /*if (content == "back" || content == "return" || content == "b") {
                                //if (wrapper.currentQuestion?.step != 0) {
                                    wrapper.undoAnswer()
                                //}
                            } else {
                                wrapper.answerCurrentQuestion(answer)
                            }
                            event.channel.sendMessage(buildEmbed {
                                author { "Question #${wrapper.currentQuestion!!.step + 1}:" }
                                description { wrapper.currentQuestion!!.question.capitalize() }
                                footer { "Type in \"exit\" to kill the process" }
                                color { Immutable.SUCCESS }
                            }).await {
                                awaitAnswer(event, wrapper)
                            }*/
                            event.channel.sendMessage(buildEmbed {
                                if (content == "back" || content == "return" || content == "b") {
                                    val q = if (wrapper.currentQuestion?.step != 0) wrapper.undoAnswer()!! else wrapper.currentQuestion!!
                                    author { "Question #${q.step + 1}:" }
                                    description { q.question.capitalize() }
                                    footer { "Type in \"exit\" to kill the process" }
                                    color { Immutable.SUCCESS }
                                } else {
                                    val q = wrapper.answerCurrentQuestion(answer)!!
                                    author { "Question #${q.step + 1}:" }
                                    description { q.question.capitalize() }
                                    footer { "Type in \"exit\" to kill the process" }
                                    color { Immutable.SUCCESS }
                                }
                            }).await {
                                awaitAnswer(event, wrapper)
                            }
                        } else {
                            val guess = wrapper.guesses.first { it.probability > 0.825 }
                            val embed = buildEmbed {
                                field(true, "Name:") { guess.name }
                                field(true, "Description:") { guess.description?.capitalize() ?: "None" }
                                image { guess.image?.toString() }
                                color { Immutable.SUCCESS }
                            }
                            event.channel.sendMessage(embed).queue()
                            USERS_WITH_PROCESSES -= event.author
                        }
                    }
                }
            } else {
                event.sendFailure("Incorrect answer! Use `aliases` or `help` to get documentation!").await {
                    awaitAnswer(event, wrapper)
                }
            }
        } else {
            USERS_WITH_PROCESSES -= event.author
            event.sendFailure("Time is up!").queue()
        }
    }
}