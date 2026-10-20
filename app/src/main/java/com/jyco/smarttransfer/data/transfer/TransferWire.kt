package com.jyco.smarttransfer.data.transfer

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream

internal object TransferWire {
    private const val CHUNK_SIZE = 64 * 1024

    suspend fun writeChunks(source: InputStream, output: DataOutputStream, checkActive: suspend () -> Unit = {}) {
        val buffer = ByteArray(CHUNK_SIZE)
        while (true) {
            checkActive()
            val count = source.read(buffer)
            if (count < 0) break
            if (count == 0) continue
            output.writeInt(count)
            output.write(buffer, 0, count)
        }
        output.writeInt(0)
    }

    suspend fun readChunks(input: DataInputStream, target: OutputStream, checkActive: suspend () -> Unit = {}) {
        val buffer = ByteArray(CHUNK_SIZE)
        while (true) {
            checkActive()
            val count = input.readInt()
            if (count == 0) break
            require(count in 1..CHUNK_SIZE) { "Invalid payload chunk" }
            input.readFully(buffer, 0, count)
            target.write(buffer, 0, count)
        }
    }
}
