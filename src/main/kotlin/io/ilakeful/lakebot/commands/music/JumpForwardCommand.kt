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

package io.ilakeful.lakebot.commands.music

import io.ilakeful.lakebot.commands.Command
import io.ilakeful.lakebot.entities.extensions.*
import io.ilakeful.lakebot.utils.AudioUtils
import io.ilakeful.lakebot.utils.TimeUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class JumpForwardCommand : Command {
    override val name = "jumpforward"
    override val aliases = listOf("fastforward", "fast-forward")
    override val description = "The command fast-forwarding a song for a moment that comes at the specified time"
    override val usage: (String) -> String = { "${super.usage(it)} <time>" }
    override val examples = { it: String ->
        mapOf(
            "$it$name 1m32s" to "fast-forwards track from 01:05 to 02:37",
            "$it$name 1h30s" to "fast-forwards track from 02:24 to 01:02:54",
            "$it$name 5s" to "fast-forwards track from 00:06 to 00:11"
        )
    }
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        if (event.argsRaw !== null) {
            if (!event.member!!.isConnected) {
                event.sendFailure("You're not in the voice channel!").queue()
            } else {
                if (AudioUtils[event.guild].audioPlayer.playingTrack !== null) {
                    if (TimeUtils.TIME_REGEX.containsMatchIn(args[0])) {
                        try {
                            val time = TimeUtils.parseTime(args[0])
                            val limit = AudioUtils[event.guild].audioPlayer.playingTrack.duration - AudioUtils[event.guild].audioPlayer.playingTrack.position
                            if (time > limit) {
                                event.sendFailure("You can't make track jump to that position!").queue()
                            } else {
                                AudioUtils[event.guild].audioPlayer.playingTrack.position = AudioUtils[event.guild].audioPlayer.playingTrack.position + time
                                event.channel.sendSuccess("Jumped to the specified position (${TimeUtils.asDuration(AudioUtils[event.guild].audioPlayer.playingTrack.position)})!").queue()
                            }
                        } catch (e: Exception) {
                            event.sendFailure("That's not a valid time format!").queue()
                        }
                    } else {
                        event.sendFailure("That's not a valid time format!").queue()
                    }
                } else {
                    event.sendFailure("There is no track that is being played now!").queue()
                }
            }
        } else {
            event.sendFailure("You specified no time!").queue()
        }
    }
}