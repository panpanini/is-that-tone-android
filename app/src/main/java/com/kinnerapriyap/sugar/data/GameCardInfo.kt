/*
 * Copyright 2021 Kinnera Priya Putti
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kinnerapriyap.sugar.data

data class GameCardInfo(
    // roundAsString to answer
    val answers: Map<String, String?> = emptyMap(),
    val activeRound: Int? = null,
    val isStarted: Boolean = false,
    val isActivePlayer: Boolean = false,
    val isRoundOver: Boolean = false,
    val isGameOver: Boolean = false,
)
