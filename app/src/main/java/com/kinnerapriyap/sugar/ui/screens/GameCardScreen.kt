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
package com.kinnerapriyap.sugar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinnerapriyap.sugar.MainViewModel
import com.kinnerapriyap.sugar.R
import com.kinnerapriyap.sugar.data.GameCardInfo
import com.kinnerapriyap.sugar.ui.screens.CardState.Companion.getCardState

enum class CardState {
    DONE, CURRENT, TO_PLAY;

    companion object {
        fun getCardState(roundNo: String, activeRound: Int?) =
            when {
                roundNo < activeRound.toString() -> DONE
                roundNo == activeRound.toString() -> CURRENT
                else -> TO_PLAY
            }
    }
}

@ExperimentalFoundationApi
@Composable
fun GameCardScreen(
    viewModel: MainViewModel = viewModel(),
    openWordCard: () -> Unit,
    showGameOver: () -> Unit
) {
    val gameCardInfo: GameCardInfo by viewModel.gameCardInfo.observeAsState(GameCardInfo())
    Scaffold {
        val hasOverlay =
            !gameCardInfo.isStarted || gameCardInfo.isGameOver || gameCardInfo.isRoundOver
        when {
            !gameCardInfo.isStarted ->
                StartDimOverlay(
                    isActivePlayer = gameCardInfo.isActivePlayer,
                    startGame = { viewModel.startGame(openWordCard) }
                )
            gameCardInfo.isGameOver -> showGameOver.invoke()
            gameCardInfo.isRoundOver ->
                RoundOverDimOverlay(
                    isActivePlayer = gameCardInfo.isActivePlayer,
                    activeRound = gameCardInfo.activeRound ?: 0,
                    goToNextRound = { viewModel.goToNextRound() }
                )
            else -> {
                // do nothing
            }
        }
        Column(
            modifier = Modifier.padding(20.dp).fillMaxHeight(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(
                    if (gameCardInfo.isActivePlayer) R.string.your_turn_to_act
                    else R.string.your_turn_to_guess
                )
            )
            Spacer(modifier = Modifier.height(20.dp))
            GameCards(
                gameCardInfo = gameCardInfo,
                openWordCard = openWordCard,
                hasOverlay = hasOverlay
            )
        }
    }
}

@ExperimentalFoundationApi
@Composable
fun GameCards(gameCardInfo: GameCardInfo, openWordCard: () -> Unit, hasOverlay: Boolean) {
    LazyVerticalGrid(cells = GridCells.Fixed(2)) {
        items(gameCardInfo.answers.toList()) { (roundNo, answer) ->
            val cardState = getCardState(roundNo, gameCardInfo.activeRound)
            Card(
                modifier = Modifier
                    .size(100.dp)
                    .padding(20.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .wrapContentSize()
                    .clickable(
                        role = Role.Button,
                        enabled = cardState == CardState.CURRENT && !hasOverlay
                    ) {
                        openWordCard.invoke()
                    },
                backgroundColor = when (cardState) {
                    CardState.DONE -> Color.LightGray
                    CardState.CURRENT -> MaterialTheme.colors.primary
                    CardState.TO_PLAY -> MaterialTheme.colors.secondary
                },
                elevation = 4.dp,
                border = BorderStroke(2.dp, MaterialTheme.colors.onBackground)
            ) {
                Text(
                    text = "$roundNo : ${answer ?: "-"}",
                    modifier = Modifier.wrapContentSize().padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun StartDimOverlay(
    isActivePlayer: Boolean,
    startGame: () -> Unit
) {
    DimOverlay(alpha = 0x66) {
        if (isActivePlayer) {
            Button(onClick = { startGame.invoke() }) {
                Text(text = stringResource(id = R.string.start_game))
            }
        } else {
            Text(text = stringResource(id = R.string.waiting_to_start))
        }
    }
}

@Composable
fun RoundOverDimOverlay(
    isActivePlayer: Boolean,
    activeRound: Int,
    goToNextRound: () -> Unit
) {
    DimOverlay(alpha = 0x66) {
        if (isActivePlayer) {
            Button(onClick = { goToNextRound.invoke() }) {
                Text(text = stringResource(id = R.string.go_to_next_round))
            }
        } else {
            Text(
                text = stringResource(
                    id = R.string.round_over,
                    formatArgs = arrayOf(activeRound)
                )
            )
        }
    }
}

@Composable
fun DimOverlay(
    alpha: Int = 0x66,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(1f)
            .background(Color(0, 0, 0, alpha)),
        contentAlignment = Alignment.Center,
        content = content
    )
}
