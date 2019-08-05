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

class JumpBackCommand : Command {
    override val name = "jumpback"
    override val aliases = listOf("windback", "returnto", "jumpbackward", "rewind", "jump-back", "wind-back", "return-to", "jump-backward")
    override val description = "The command rewinding the song back"
    override val usage: (String) -> String = { "${super.usage(it)} <time>" }
    override val examples = { it: String ->
        mapOf(
            "$it$name 1m32s" to "rewinds track from 02:37 to 01:05",
            "$it$name 1h30s" to "rewinds track from 01:02:54 to 02:24",
            "$it$name 5s" to "rewinds track from 00:11 to 00:06"
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
                            if ((AudioUtils[event.guild].audioPlayer.playingTrack.position - time) < 1) {
                                AudioUtils[event.guild].audioPlayer.playingTrack.position = 0
                                event.channel.sendSuccess("Successfully restarted!").queue()
                            } else {
                                AudioUtils[event.guild].audioPlayer.playingTrack.position = AudioUtils[event.guild].audioPlayer.playingTrack.position - time
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