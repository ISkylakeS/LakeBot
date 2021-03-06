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

@file:Suppress("UNCHECKED_CAST", "UNUSED")
package io.ilakeful.lakebot.entities

import io.ilakeful.lakebot.entities.annotations.Author

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

import kotlin.coroutines.CoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

import kotlinx.coroutines.*

import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import java.util.concurrent.Executors.newFixedThreadPool
import kotlin.concurrent.thread

private val coroutineDispatcher = newFixedThreadPool(3) { Thread(it, "EventWaiter") }
        .asCoroutineDispatcher()
/**
 * <b>EventWaiter</b> is designed to await JDA events and, after that, handle them and return their instances.
 *
 * This class also includes several useful utilities for use in commands.
 *
 * <b>NOTE</b>: Do NOT forget to add EventWaiter as an event listener to your JDA instance!
 *
 * <p>
 *  <i>Some pieces of code have been inspired from LaxusBot</i>
 *  <a href="https://github.com/LaxusBot/Laxus/blob/master/commons/jda/src/main/kotlin/xyz/laxus/jda/listeners/EventWaiter.kt">GitHub Link</a>
 * </p>
 */
@Author("ILakeful", "TheMonitorLizard (LaxusBot implementation)")
object EventWaiter : EventListener, CoroutineContext by coroutineDispatcher, AutoCloseable by coroutineDispatcher {
    val tasks = ConcurrentHashMap<KClass<*>, MutableSet<AwaitableTask<*>>>()
    inline fun <reified E: GenericEvent> receiveTask() = tasks[E::class]
    inline fun <reified E: GenericEvent> receiveEvent(
        delay: Long = -1,
        unit: TimeUnit = TimeUnit.SECONDS,
        noinline condition: suspend (E) -> Boolean
    ): Deferred<E?> = receiveEvent(E::class, condition, delay, unit)
    @Author("ILakeful")
    suspend inline fun <reified E: GenericEvent> receiveEventRaw(
            delay: Long = -1,
            unit: TimeUnit = TimeUnit.SECONDS,
            noinline check: suspend (E) -> Boolean
    ): E? = receiveEventRaw(E::class, check, delay, unit)
    /**
     * Waits a predetermined amount of time for an {@link net.dv8tion.jda.api.events.Event event},
     * receives and returns it
     *
     * @param <T> the type of awaiting event
     * @param type the {@link kotlin.reflect.KClass} of event to wait for
     * @param check checks the match of the received event with the expected by the specified condition
     * @param delay the maximum amount of time to wait for. Default is -1 (infinity awaiting)
     * @param unit measurement of the timeout
     *
     * @return deferred value (non-blocking cancelable future) of possibly null event instance
     */
    @Author("ILakeful", "TheMonitorLizard (LaxusBot implementation)")
    fun <E: GenericEvent> receiveEvent(type: KClass<E>, check: suspend (E) -> Boolean, delay: Long = -1, unit: TimeUnit = TimeUnit.SECONDS): Deferred<E?> {
        val deferred = CompletableDeferred<E?>()
        val eventSet = taskSetType(type)
        val waiting = AwaitableTask(check, deferred)
        eventSet += waiting
        if (delay > 0) {
            CoroutineScope(this).launch {
                delay(unit.toMillis(delay))
                eventSet -= waiting
                deferred.complete(null)
            }
        }
        return deferred
    }
    @Author("ILakeful")
    suspend fun <E: GenericEvent> receiveEventRaw(
            type: KClass<E>,
            check: suspend (E) -> Boolean,
            delay: Long = -1,
            unit: TimeUnit = TimeUnit.SECONDS
    ): E? = receiveEvent(type, check, delay, unit).await()
    /**
     * Waits for a reaction for {@link net.dv8tion.jda.api.entities.Message message}
     *
     * @param msg the message from which the reaction is received
     * @param author the user from whom the reaction must be received
     * @param delay the maximum amount of time to wait for. Default is 1 minute.
     * @param unit measurement of the timeout
     *
     * @return boolean value (true, if user reacted with "\u2705", or false, if user reacted with "\u274E" or reaction wasn't received)
     */
    @Author("ILakeful")
    suspend fun awaitConfirmation(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Boolean {
        msg.addReaction("\u2705").complete()
        msg.addReaction("\u274E").complete()
        return this.receiveEventRaw<MessageReactionAddEvent>(delay, unit) {
            val emote = it.reactionEmote.name
            it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
        }?.reactionEmote?.name == "\u2705"
    }
    @Author("ILakeful")
    fun awaitConfirmationAsync(
            msg: Message,
            author: User,
            delay: Long = 1,
            unit: TimeUnit = TimeUnit.MINUTES,
            action: (Boolean) -> Unit = {}
    ): Deferred<Boolean> = CoroutineScope(this).async {
        val bool = awaitConfirmation(msg, author, delay, unit)
        action(bool)
        bool
    }
    /**
     * Waits for a reaction for {@link net.dv8tion.jda.api.entities.Message message}
     *
     * @param msg the message from which the reaction is received
     * @param author the user from whom the reaction must be received
     * @param delay the maximum amount of time to wait for. Default is 1 minute.
     * @param unit measurement of the timeout
     *
     * @return possibly-null boolean value (true, if user reacted with "\u2705", or false, if user reacted with "\u274E")
     */
    @Author("ILakeful")
    suspend fun awaitNullableConfirmation(msg: Message, author: User, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Boolean? {
        msg.addReaction("\u2705").complete()
        msg.addReaction("\u274E").complete()
        val name = this.receiveEventRaw<MessageReactionAddEvent>(delay, unit) {
            val emote = it.reactionEmote.name
            it.user == author && it.messageIdLong == msg.idLong && (emote == "\u2705" || emote == "\u274E")
        }?.reactionEmote?.name
        return if (name != null) name == "\u2705" else null
    }
    @Author("ILakeful")
    fun awaitNullableConfirmationAsync(
            msg: Message,
            author: User,
            delay: Long = 1,
            unit: TimeUnit = TimeUnit.MINUTES,
            action: (Boolean?) -> Unit = {}
    ): Deferred<Boolean?> = CoroutineScope(this).async {
        val bool = awaitNullableConfirmation(msg, author, delay, unit)
        action(bool)
        bool
    }
    /**
     * Waits for an {@link net.dv8tion.jda.api.entities.Message message},
     * receives and returns it
     *
     * @param user current user the user from whom the message is received
     * @param channel the channel in which the message is received
     *
     * @return possibly-null received message
     */
    @Author("ILakeful")
    suspend fun awaitMessage(user: User, channel: MessageChannel, delay: Long = 1, unit: TimeUnit = TimeUnit.MINUTES): Message? {
        return this.receiveEventRaw<MessageReceivedEvent>(delay, unit) {
            it.author == user && it.channel == channel
        }?.message
    }
    @Author("ILakeful")
    fun awaitMessageAsync(
            user: User,
            channel: MessageChannel,
            delay: Long = 1,
            unit: TimeUnit = TimeUnit.MINUTES,
            action: (Message?) -> Unit = {}
    ): Deferred<Message?> = CoroutineScope(this).async {
        val msg = awaitMessage(user, channel, delay, unit)
        action(msg)
        msg
    }
    override fun onEvent(event: GenericEvent) {
        CoroutineScope(this).launch {
            val type = event::class
            dispatchEventType(event, type)
            type.allSuperclasses.forEach { dispatchEventType(event, it) }
        }
    }
    private fun <E: GenericEvent> taskSetType(type: KClass<E>): MutableSet<AwaitableTask<E>> {
        return tasks.computeIfAbsent(type) {
            ConcurrentHashMap.newKeySet()
        } as MutableSet<AwaitableTask<E>>
    }
    private suspend fun <E: GenericEvent> dispatchEventType(event: E, type: KClass<*>) {
        val set = tasks[type] ?: return
        val filtered = set.filterTo(hashSetOf()) {
            val waiting = (it as AwaitableTask<E>)
            waiting(event)
        }
        set -= filtered
        if (set.isEmpty()) {
            tasks -= type
        }
    }
    class AwaitableTask<in E: GenericEvent>(
            private val condition: suspend (E) -> Boolean,
            private val completion: CompletableDeferred<in E?>
    ) {
        suspend operator fun invoke(event: E): Boolean = try {
            if (condition(event)) {
                completion.complete(event)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            completion.completeExceptionally(t)
            true
        }
    }
}