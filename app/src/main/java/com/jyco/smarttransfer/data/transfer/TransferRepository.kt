package com.jyco.smarttransfer.data.transfer

import android.content.Context
import android.provider.OpenableColumns
import com.google.gson.Gson
import com.jyco.smarttransfer.data.socket.SocketManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

data class TransferProgress(
    val busy: Boolean = false,
    val completed: Int = 0,
    val total: Int = 0,
    val message: String = "Ready",
    val error: String? = null
)

/** Version 1: magic, item count, then UTF type/name and length-prefixed chunks (0 ends item).
 * Receiver acknowledges only after all items have been saved. No media is loaded into memory.
 */
class TransferRepository(private val context: Context, private val sockets: SocketManager) {
    private val gson = Gson()
    private val mutex = Mutex()
    private val _progress = MutableStateFlow(TransferProgress())
    val progress = _progress.asStateFlow()

    suspend fun send(items: List<TransferItem>) = session {
        require(items.isNotEmpty()) { "Select at least one item" }
        require(items.size <= MAX_ITEMS) { "Too many items in one transfer" }
        val socket = sockets.getSocket() ?: error("Device disconnected")
        socket.soTimeout = 60_000
        val output = sockets.transferOutput()
        val input = sockets.transferInput()
        _progress.value = TransferProgress(true, total = items.size, message = "Sending")
        output.writeInt(MAGIC)
        output.writeInt(items.size)
        items.forEachIndexed { index, item ->
            currentCoroutineContext().ensureActive()
            val uri = when (item) {
                is TransferItem.Photo -> item.uri
                is TransferItem.Video -> item.uri
                else -> null
            }
            val type = when (item) {
                is TransferItem.Photo -> "photo"
                is TransferItem.Video -> "video"
                is TransferItem.Contact -> "contact"
                is TransferItem.Message -> "message"
            }
            var name = "$type.json"
            if (uri != null) {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) name = it.getString(0) ?: type
                }
            }
            output.writeUTF(type)
            output.writeUTF(name.take(200))
            val source = if (uri != null) {
                context.contentResolver.openInputStream(uri) ?: throw IOException("Cannot open selected media")
            } else {
                val value = when (item) {
                    is TransferItem.Contact -> item.contact
                    is TransferItem.Message -> item.message
                    else -> error("Unsupported item")
                }
                gson.toJson(value).byteInputStream(Charsets.UTF_8)
            }
            source.use {
                TransferWire.writeChunks(it, output) { currentCoroutineContext().ensureActive() }
            }
            _progress.value = _progress.value.copy(completed = index + 1)
        }
        output.flush()
        check(input.readInt() == ACK) { "Receiver did not confirm transfer" }
        socket.soTimeout = 0
        _progress.value = _progress.value.copy(busy = false, message = "Transfer complete")
    }

    suspend fun receive() = session {
        val socket = sockets.getSocket() ?: error("Device disconnected")
        val input = sockets.transferInput()
        _progress.value = TransferProgress(true, message = "Waiting for sender")
        socket.soTimeout = 0
        check(input.readInt() == MAGIC) { "Unsupported transfer protocol" }
        socket.soTimeout = 60_000
        val total = input.readInt()
        require(total in 1..MAX_ITEMS) { "Invalid item count" }
        val directory = File(context.filesDir, "received/${UUID.randomUUID()}")
        check(directory.mkdirs()) { "Cannot create receive directory" }
        try {
            _progress.value = TransferProgress(true, total = total, message = "Receiving")
            repeat(total) { index ->
                currentCoroutineContext().ensureActive()
                val type = input.readUTF()
                require(type in setOf("photo", "video", "contact", "message")) { "Unsupported content type" }
                val name = input.readUTF().replace(Regex("[^a-zA-Z0-9._-]"), "_").take(200)
                val target = File(directory, "${index}_${name.ifBlank { type }}")
                target.outputStream().use { output ->
                    TransferWire.readChunks(input, output) { currentCoroutineContext().ensureActive() }
                }
                _progress.value = _progress.value.copy(completed = index + 1)
            }
            sockets.transferOutput().apply { writeInt(ACK); flush() }
            socket.soTimeout = 0
            _progress.value = _progress.value.copy(busy = false,
                message = "Received $total items. Saved in app storage: received/${directory.name}")
        } catch (error: Exception) {
            directory.deleteRecursively()
            throw error
        }
    }

    private suspend fun session(block: suspend () -> Unit) = withContext(Dispatchers.IO) {
        check(mutex.tryLock()) { "A transfer is already running" }
        try {
            check(sockets.authenticated && sockets.isConncted()) { "Connect and authenticate the device first" }
            block()
        } catch (error: Exception) {
            sockets.close()
            _progress.value = _progress.value.copy(busy = false, error = error.message ?: "Transfer failed", message = "Transfer failed")
            throw error
        } finally {
            mutex.unlock()
        }
    }

    fun cancel() { sockets.close() }

    private companion object {
        const val MAGIC = 0x53544631
        const val ACK = 0x53544F4B
        const val MAX_ITEMS = 100_000
    }
}
