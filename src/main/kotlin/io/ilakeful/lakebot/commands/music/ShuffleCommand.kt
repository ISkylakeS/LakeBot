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
import io.ilakeful.lakebot.entities.extensions.isConnected
import io.ilakeful.lakebot.entities.extensions.sendFailure
import io.ilakeful.lakebot.entities.extensions.sendSuccess
import io.ilakeful.lakebot.utils.AudioUtils

import net.dv8tion.jda.api.events.message.MessageReceivedEvent

class ShuffleCommand : Command {
    override val name = "shuffle"
    override val aliases = listOf("rq", "randomize-queue")
    override val description = "The command shuffling the current queue"
    override suspend fun invoke(event: MessageReceivedEvent, args: Array<String>) {
        val manager = AudioUtils[event.guild]
        if (manager.audioPlayer.playingTrack !== null) {
            if (event.member!!.isConnected) {
                manager.trackScheduler.receiveQueue {
                    it.shuffle()
                    event.channel.sendSuccess("The queue has been successfully shuffled!").queue()
                }
            } else {
                event.channel.sendFailure("You are not connected to the voice channel!").queue()
            }
        } else {
            event.channel.sendFailure("No track is currently playing!").queue()
        }
    }
}