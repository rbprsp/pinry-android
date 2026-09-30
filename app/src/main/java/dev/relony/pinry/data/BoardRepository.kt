package dev.relony.pinry.data

import dev.relony.pinry.data.api.Board
import dev.relony.pinry.data.api.BoardName
import dev.relony.pinry.data.api.BoardUpdate
import dev.relony.pinry.data.api.NewBoard
import dev.relony.pinry.data.api.Page
import dev.relony.pinry.data.api.PinryApi
import dev.relony.pinry.data.net.withMediaFrom
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.HttpUrl

sealed interface BoardChange {
    data class Created(val board: Board) : BoardChange
    /** Renamed, made private/public, or its pins (count, cover) changed. */
    data class Updated(val board: Board) : BoardChange
    data class Deleted(val id: Int) : BoardChange
    data class PinAdded(val boardId: Int, val pinId: Int) : BoardChange
    data class PinRemoved(val boardId: Int, val pinId: Int) : BoardChange
}

class BoardRepository(private val api: () -> PinryApi, private val baseUrl: () -> HttpUrl) {
    private val _changes = MutableSharedFlow<BoardChange>(extraBufferCapacity = 16)

    /** Edits made in this app, so open screens can update without reloading. */
    val changes: SharedFlow<BoardChange> = _changes

    suspend fun page(username: String, offset: Int): Page<Board> {
        val page = api().boards(username, offset)
        val base = baseUrl()
        return page.copy(results = page.results.map { it.withMediaFrom(base) })
    }

    suspend fun board(id: Int): Board = api().board(id).withMediaFrom(baseUrl())

    suspend fun names(username: String): List<BoardName> = api().boardNames(username)

    suspend fun create(name: String): Board =
        api().createBoard(NewBoard(name.trim())).withMediaFrom(baseUrl()).also { _changes.emit(BoardChange.Created(it)) }

    suspend fun rename(id: Int, name: String): Board = update(id, BoardUpdate(name = name.trim()))

    suspend fun setPrivate(id: Int, isPrivate: Boolean): Board = update(id, BoardUpdate(isPrivate = isPrivate))

    suspend fun delete(id: Int) {
        api().deleteBoard(id)
        _changes.emit(BoardChange.Deleted(id))
    }

    suspend fun addPin(boardId: Int, pinId: Int) {
        update(boardId, BoardUpdate(pinsToAdd = listOf(pinId)))
        _changes.emit(BoardChange.PinAdded(boardId, pinId))
    }

    suspend fun removePin(boardId: Int, pinId: Int) {
        update(boardId, BoardUpdate(pinsToRemove = listOf(pinId)))
        _changes.emit(BoardChange.PinRemoved(boardId, pinId))
    }

    private suspend fun update(id: Int, update: BoardUpdate): Board =
        api().updateBoard(id, update).withMediaFrom(baseUrl()).also { _changes.emit(BoardChange.Updated(it)) }
}
