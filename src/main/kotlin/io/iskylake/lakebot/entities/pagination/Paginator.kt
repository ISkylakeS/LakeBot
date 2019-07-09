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

@file:Suppress("UNUSED")
package io.iskylake.lakebot.entities.pagination

import com.google.common.collect.Lists
import io.iskylake.lakebot.entities.EventWaiter

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.requests.RestAction

import java.util.concurrent.TimeUnit

interface Paginator<out T> {
    companion object {
        const val BIG_LEFT = "\u23EA"
        const val LEFT = "\u25C0"
        const val STOP = "\u23FA"
        const val RIGHT = "\u25B6"
        const val BIG_RIGHT = "\u23E9"
    }
    val event: MessageReceivedEvent
    val list: List<T>
    val pageSize: Int
    val action: (Message) -> Unit
        get() = {
            try {
                it.clearReactions().queue(null) { _ -> }
            } catch (ignored: Exception) {
            }
        }
    val pages: List<List<T>>
        get() = Lists.partition(list, pageSize)
    operator fun get(num: Int = 1): MessageEmbed
    operator fun invoke(page: Int = 1) = accept(event.channel.sendMessage(this[page]), page)
    fun accept(rest: RestAction<Message>, pageNum: Int = 1) = rest.queue { m ->
        if (pages.size > 1) {
            m.addReaction(BIG_LEFT).queue()
            m.addReaction(LEFT).queue()
            m.addReaction(STOP).queue()
            m.addReaction(RIGHT).queue()
            m.addReaction(BIG_RIGHT).queue({ waiter(m, pageNum) }) {
                waiter(m, pageNum)
            }
        } else {
            action(m)
        }
    }
    fun waiter(msg: Message, num: Int = 1) {
        CoroutineScope(EventWaiter).async {
            val e = EventWaiter.receiveEventRaw<MessageReactionAddEvent>(1, TimeUnit.MINUTES) {
                val isValidEmote = BIG_LEFT == it.reactionEmote.name || BIG_RIGHT == it.reactionEmote.name || LEFT == it.reactionEmote.name || STOP == it.reactionEmote.name || RIGHT == it.reactionEmote.name
                it.messageId == msg.id && isValidEmote && event.author == it.user
            }
            if (e !== null) {
                var newPageNum = num
                when (e.reactionEmote.name) {
                    BIG_LEFT -> newPageNum = 1
                    LEFT -> {
                        if (newPageNum > 1) {
                            newPageNum--
                        }
                    }
                    RIGHT -> {
                        if (newPageNum < pages.size) {
                            newPageNum++
                        }
                    }
                    BIG_RIGHT -> newPageNum = pages.size
                    STOP -> {
                        action(msg)
                        return@async
                    }
                }
                try {
                    e.reaction.removeReaction(e.user).queue()
                } catch (ignored: Exception) {
                }
                msg.editMessage(this@Paginator[newPageNum]).queue { msg -> waiter(msg, newPageNum) }
            } else {
                action(msg)
            }
        }
    }
}