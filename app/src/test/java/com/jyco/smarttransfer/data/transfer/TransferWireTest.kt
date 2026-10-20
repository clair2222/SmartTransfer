package com.jyco.smarttransfer.data.transfer

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.*

class TransferWireTest {
    @Test fun roundTripMultipleItemsIncludingEmptyAndLargeBinary() = runBlocking {
        val items = listOf(ByteArray(0), ByteArray(200_123) { (it % 256).toByte() }, "안녕하세요\nSMS".toByteArray())
        val wire = ByteArrayOutputStream()
        val output = DataOutputStream(wire)
        items.forEach { TransferWire.writeChunks(it.inputStream(), output) }
        output.writeInt(12345) // Next protocol field must remain unread.
        val input = DataInputStream(wire.toByteArray().inputStream())
        items.forEach { expected ->
            val received = ByteArrayOutputStream()
            TransferWire.readChunks(input, received)
            assertArrayEquals(expected, received.toByteArray())
        }
        assertEquals(12345, input.readInt())
    }

    @Test fun rejectsInvalidChunkSizes() = runBlocking {
        for (size in listOf(-1, 65_537, Int.MAX_VALUE)) {
            val wire = ByteArrayOutputStream()
            DataOutputStream(wire).writeInt(size)
            try {
                TransferWire.readChunks(DataInputStream(wire.toByteArray().inputStream()), ByteArrayOutputStream())
                fail("Accepted invalid chunk size $size")
            } catch (_: IllegalArgumentException) { }
        }
    }

    @Test fun rejectsTruncatedPayload() = runBlocking {
        val wire = ByteArrayOutputStream()
        DataOutputStream(wire).apply { writeInt(10); write(byteArrayOf(1, 2)) }
        try {
            TransferWire.readChunks(DataInputStream(wire.toByteArray().inputStream()), ByteArrayOutputStream())
            fail("Accepted truncated payload")
        } catch (_: EOFException) { }
    }
}
