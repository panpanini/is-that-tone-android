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
package com.kinnerapriyap.sugar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.kinnerapriyap.sugar.data.GameCardInfo
import com.kinnerapriyap.sugar.data.GameRoom
import com.kinnerapriyap.sugar.data.Player
import com.kinnerapriyap.sugar.data.WordCard
import com.kinnerapriyap.sugar.data.WordCardInfo

const val ROOMS_COLLECTION = "rooms"
const val ROUNDS_INFO_KEY = "roundsInfo"
const val ACTIVE_ROUND_KEY = "activeRound"
const val PLAYERS_KEY = "players"
const val IS_STARTED_KEY = "isStarted"
const val WORD_CARDS_KEY = "wordCards"

const val WORD_CARDS_COLLECTION = "wordCards"
const val LANGUAGE_KEY = "language"

const val MAX_PLAYERS = 4

class MainViewModel : ViewModel() {

    private val db = Firebase.firestore
    private val roomDocument: DocumentReference?
        get() {
            val roomName = roomName.value
            return if (roomName != null && roomName.isNotBlank()) {
                db.collection(ROOMS_COLLECTION).document(roomName)
            } else null
        }

    private val _roomName = MutableLiveData("")
    val roomName: LiveData<String> = _roomName
    fun onRoomNameChanged(newName: String) {
        _roomName.value = newName
    }

    private val _userName = MutableLiveData("")
    val userName: LiveData<String> = _userName
    fun onUserNameChanged(newName: String) {
        _userName.value = newName
    }

    private var _uid: String? = null
    fun onUidChanged(uid: String?) {
        _uid = uid
    }

    private var _gameRoom: GameRoom? = null

    private val _gameCardInfo = MutableLiveData(GameCardInfo())
    val gameCardInfo: LiveData<GameCardInfo> = _gameCardInfo

    private val playerCount: Int
        get() = _gameRoom?.players?.size ?: 0

    private var _scores = MutableLiveData(emptyMap<Player, Int>())
    val scores: LiveData<Map<Player, Int>> = _scores

    private var _wordCards: List<WordCard>? = null

    private var _wordCardInfo = MutableLiveData(WordCardInfo())
    val wordCardInfo: LiveData<WordCardInfo> = _wordCardInfo

    fun enterRoom(openGameCard: () -> Unit) {
        (roomDocument ?: return).get()
            .addOnSuccessListener { doc ->
                val gameRoom = doc.toObject<GameRoom>()
                when {
                    gameRoom == null ->
                        createRoom(openGameCard)
                    gameRoom.players.firstOrNull { it.uid == _uid } != null ->
                        rejoinRoom(openGameCard)
                    !gameRoom.isStarted && gameRoom.players.size < MAX_PLAYERS ->
                        joinRoom(gameRoom, openGameCard)
                    else -> {
                        // TODO: Show room is occupied or player count is full
                    }
                }
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    private fun rejoinRoom(openGameCard: () -> Unit) = goToGameCard(openGameCard)

    private fun joinRoom(gameRoom: GameRoom, openGameCard: () -> Unit) {
        val players = gameRoom.players.toMutableList().apply {
            add(Player(_uid, userName.value))
        }
        (roomDocument ?: return)
            .update(mapOf(PLAYERS_KEY to players))
            .addOnSuccessListener {
                goToGameCard(openGameCard)
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    private fun createRoom(openGameCard: () -> Unit) {
        val newRoom = GameRoom(
            players = listOf(Player(_uid, userName.value))
        )
        (roomDocument ?: return)
            .set(newRoom)
            .addOnSuccessListener {
                goToGameCard(openGameCard)
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    private lateinit var listener: ListenerRegistration

    private fun goToGameCard(openGameCard: () -> Unit) {
        openGameCard.invoke()
        listener = (roomDocument ?: return)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    // TODO: Handle error
                    return@addSnapshotListener
                }

                val gameRoom = snapshot.toObject<GameRoom>()
                _gameRoom = gameRoom ?: return@addSnapshotListener
                val answers =
                    if (gameRoom.roundsInfo.isEmpty()) {
                        (1..playerCount).toList().map { it.toString() to null }.toMap()
                    } else {
                        gameRoom.roundsInfo.mapValues {
                            _uid?.let { uid ->
                                it.value.getOrDefault(uid, null)
                            }
                        }
                    }
                val isRoundOver =
                    gameRoom.roundsInfo
                        .getOrDefault(gameRoom.activeRound.toString(), emptyMap())
                        .size == playerCount
                val isGameOver = gameRoom.activeRound > gameRoom.players.size
                if (isGameOver) setScores()

                val selectedAnswerChar = answers.getOrDefault(gameRoom.activeRound.toString(), null)
                val activePlayerAnswer =
                    gameRoom.roundsInfo.getOrDefault(gameRoom.activeRound.toString(), emptyMap())
                        .getOrDefault(
                            gameRoom.players.getOrNull(gameRoom.activeRound - 1)?.uid ?: "",
                            null
                        )
                val usedAnswers =
                    answers.values.toMutableList()
                        .apply { this.remove(selectedAnswerChar) }
                        .filterNotNull()
                val isActivePlayer = _uid?.let { uid -> gameRoom.isActivePlayer(uid) } ?: false
                val allowChange =
                    (isActivePlayer && selectedAnswerChar == null) || (!isActivePlayer && activePlayerAnswer != null)

                _wordCardInfo.value =
                    _wordCardInfo.value?.copy(
                        wordCard = gameRoom.wordCards.getOrNull(gameRoom.activeRound - 1),
                        selectedAnswerChar = selectedAnswerChar,
                        usedAnswers = usedAnswers,
                        allowChange = allowChange,
                        instruction = when {
                            isActivePlayer -> R.string.choose_act
                            !allowChange -> R.string.waiting_for_active_player
                            else -> R.string.choose_guess
                        }
                    )

                _gameCardInfo.value = GameCardInfo(
                    answers = answers,
                    activeRound = gameRoom.activeRound,
                    isStarted = gameRoom.isStarted,
                    isActivePlayer = isActivePlayer,
                    isRoundOver = isRoundOver,
                    isGameOver = isGameOver
                )
            }
    }

    private fun setScores() {
        val gameRoom = _gameRoom ?: return
        _scores.value = gameRoom.players.mapIndexed { index, player ->
            var score = 0
            gameRoom.roundsInfo.forEach { (roundNo, answers) ->
                if (player.uid != null) {
                    val roundAnswer =
                        answers.getValue(
                            gameRoom.players.getOrNull(roundNo.toInt() - 1)?.uid ?: ""
                        )
                    score += when {
                        roundNo.toInt() - 1 == index ->
                            answers.filterValues { it == roundAnswer }.size - 1
                        answers.getValue(player.uid) == roundAnswer -> 1
                        else -> 0
                    }
                }
            }
            player to score
        }.toMap()
    }

    fun finishGame() {
        listener.remove()
    }

    fun startGame(openWordCard: () -> Unit) {
        db.collection(WORD_CARDS_COLLECTION)
            .whereEqualTo(LANGUAGE_KEY, "tl")
            .get()
            .addOnSuccessListener { querySnapshot ->
                querySnapshot.documents.shuffle()
                _wordCards =
                    querySnapshot.documents
                        .take(playerCount)
                        .mapNotNull { it.toObject<WordCard>() }

                initialiseRoom(openWordCard)
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    private fun initialiseRoom(openWordCard: () -> Unit) {
        val initialRoomInfo =
            (1..playerCount).toList().map { it.toString() to emptyMap<String, String?>() }.toMap()
        (roomDocument ?: return)
            .update(
                mapOf(
                    IS_STARTED_KEY to true,
                    ACTIVE_ROUND_KEY to 1,
                    ROUNDS_INFO_KEY to initialRoomInfo,
                    WORD_CARDS_KEY to _wordCards
                )
            )
            .addOnSuccessListener {
                openWordCard.invoke()
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    fun setAnswer(selectedAnswer: String, navigateBack: () -> Unit) {
        val newRoundsInfo =
            _gameRoom?.roundsInfo?.mapValues { roundInfo ->
                if (roundInfo.key.toInt() == _gameRoom?.activeRound) {
                    roundInfo.value.toMutableMap().apply {
                        _uid?.let { put(it, selectedAnswer) }
                    }
                } else roundInfo.value
            }
        (roomDocument ?: return)
            .update(mapOf(ROUNDS_INFO_KEY to newRoundsInfo))
            .addOnSuccessListener {
                navigateBack.invoke()
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }

    fun goToNextRound() {
        val incrementedActiveRound = _gameRoom?.activeRound?.plus(1)
        (roomDocument ?: return)
            .update(mapOf(ACTIVE_ROUND_KEY to incrementedActiveRound))
            .addOnSuccessListener {
                _gameCardInfo.value = _gameCardInfo.value?.copy(isRoundOver = false)
            }
            .addOnFailureListener {
                // TODO: Handle error
            }
    }
}
