/*
 * Copyright 2017-2018 (c) Alexander "ISkylake" Shevchenko
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

package io.iskylake.lakebot.entities.extensions

import io.iskylake.lakebot.entities.handlers.CommandHandler

import kotlinx.coroutines.experimental.async

import net.dv8tion.jda.core.requests.RestAction

import kotlin.coroutines.experimental.CoroutineContext

suspend inline fun <reified T> RestAction<T>.await(context: CoroutineContext = CommandHandler, noinline func: suspend (T) -> Unit) = queue {
    async(context) {
        func(it)
    }
}