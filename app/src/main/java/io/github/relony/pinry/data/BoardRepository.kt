package io.github.relony.pinry.data

import io.github.relony.pinry.data.api.Board
import io.github.relony.pinry.data.api.BoardName
import io.github.relony.pinry.data.api.BoardUpdate
import io.github.relony.pinry.data.api.NewBoard
import io.github.relony.pinry.data.api.PinryApi

class BoardRepository(private val api: () -> PinryApi) {
    suspend fun names(username: String): List<BoardName> = api().boardNames(username)

    suspend fun create(name: String): Board = api().createBoard(NewBoard(name.trim()))

    suspend fun addPin(boardId: Int, pinId: Int) {
        api().updateBoard(boardId, BoardUpdate(pinsToAdd = listOf(pinId)))
    }
}
