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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kinnerapriyap.sugar.MainViewModel
import com.kinnerapriyap.sugar.R
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: MainViewModel = viewModel(),
    openGameCard: () -> Unit
) {
    val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val roomName: String by viewModel.roomName.observeAsState("")
    val userName: String by viewModel.userName.observeAsState("")
    Scaffold(
        scaffoldState = scaffoldState
    ) {
        Column(
            modifier = Modifier.padding(20.dp).fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TextField(
                value = roomName,
                onValueChange = { viewModel.onRoomNameChanged(it) },
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.room_name_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Home,
                        contentDescription = stringResource(id = R.string.room_name_leading_icon)
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
            TextField(
                value = userName,
                onValueChange = { viewModel.onUserNameChanged(it) },
                singleLine = true,
                placeholder = { Text(text = stringResource(id = R.string.user_name_placeholder)) },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = stringResource(id = R.string.user_name_leading_icon)
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    if (roomName.isNotBlank() && userName.isNotBlank()) {
                        viewModel.enterRoom(openGameCard)
                    } else {
                        scope.launch {
                            scaffoldState.snackbarHostState
                                .showSnackbar(
                                    "Don't leave the fields blank! :)"
                                )
                        }
                    }
                }
            ) {
                Text(text = stringResource(id = R.string.join_game))
            }
        }
    }
}
