package com.nikolasguillen.questlog.desktop

import com.nikolasguillen.questlog.core.common.desktop.appDataDirectory
import com.sun.jna.Native
import com.sun.jna.win32.StdCallLibrary
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.PosixFilePermissions
import kotlin.concurrent.thread

private const val LOCK_FILE_NAME = "instance.lock"
private const val SOCKET_FILE_NAME = "instance.sock"

// A line is at most "OPEN_GAME 2147483647"; anything longer is not ours.
private const val MAX_LINE_BYTES = 64
private const val READ_DEADLINE_MILLIS = 2_000L
private const val READ_POLL_MILLIS = 10L

/** `AllowSetForegroundWindow` takes a process id, or this value for "any process". */
private const val ASFW_ANY = -1

internal interface User32 : StdCallLibrary {
    fun AllowSetForegroundWindow(dwProcessId: Int): Boolean
}

/**
 * Keeps one QuestLog window per user (R11, FR-006).
 *
 * The first process holds an exclusive lock on `instance.lock` and listens on a Unix-domain socket `instance.sock`, both
 * in the app data directory. A later process fails to take the lock, writes one [SingleInstanceMessage] line to the
 * socket and exits; the first process receives it in [messages]. Closing the instance releases the lock and removes the
 * socket, and a crash leaves only a stale socket file, which the next first process deletes before it listens.
 */
internal class SingleInstance private constructor(
    private val lockChannel: FileChannel,
    private val lock: FileLock,
    private val socketPath: Path,
    private val server: ServerSocketChannel?
) : AutoCloseable {

    private val received = Channel<SingleInstanceMessage>(Channel.UNLIMITED)

    /** The messages later launches sent, in order. Collected by the one window, which acts on each. */
    val messages: Flow<SingleInstanceMessage> = received.receiveAsFlow()

    private fun listen() {
        val server = server ?: return
        thread(name = "single-instance-listener", isDaemon = true) {
            while (true) {
                val connection = try {
                    server.accept()
                } catch (_: ClosedChannelException) {
                    return@thread
                } catch (_: IOException) {
                    continue
                }
                thread(name = "single-instance-connection", isDaemon = true) { receive(connection) }
            }
        }
    }

    private fun receive(connection: SocketChannel) {
        connection.use { channel ->
            try {
                val line = channel.readLine() ?: return
                SingleInstanceMessage.parse(line)?.let { received.trySend(it) }
            } catch (_: IOException) {
                // A peer that hangs up halfway has nothing to say.
            }
        }
    }

    override fun close() {
        received.close()
        runCatching { server?.close() }
        runCatching { Files.deleteIfExists(socketPath) }
        runCatching { lock.release() }
        runCatching { lockChannel.close() }
    }

    companion object {

        /**
         * Takes the instance for this process, or, when another one runs, hands [handOver] to it and returns `null`:
         * the caller then exits. The first process listens before it returns.
         *
         * @param directory Where the lock and the socket live; the app data directory unless a test supplies one.
         */
        fun acquireOrHandOver(handOver: SingleInstanceMessage, directory: Path = appDataDirectory()): SingleInstance? {
            Files.createDirectories(directory)
            val channel = FileChannel.open(
                directory.resolve(LOCK_FILE_NAME),
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE
            )
            val lock = try {
                channel.tryLock()
            } catch (_: OverlappingFileLockException) {
                // Held by this very process: the same answer as a lock held by another one.
                null
            }
            val socketPath = directory.resolve(SOCKET_FILE_NAME)
            if (lock == null) {
                channel.close()
                send(handOver, socketPath)
                return null
            }
            return SingleInstance(channel, lock, socketPath, bind(socketPath)).also { it.listen() }
        }

        // The lock is held, so no live process owns the socket file: a leftover one is stale.
        private fun bind(socketPath: Path): ServerSocketChannel? = try {
            Files.deleteIfExists(socketPath)
            ServerSocketChannel.open(StandardProtocolFamily.UNIX).apply {
                bind(UnixDomainSocketAddress.of(socketPath))
                // Other local users have no business writing to it. Windows has no POSIX permissions.
                runCatching {
                    Files.setPosixFilePermissions(socketPath, PosixFilePermissions.fromString("rw-------"))
                }
            }
        } catch (_: IOException) {
            // The window still works without it; a second launch just cannot reach it. Unix socket paths are short on
            // macOS, so a very long home directory is the usual cause.
            null
        }

        private fun send(message: SingleInstanceMessage, socketPath: Path) {
            allowForeground()
            try {
                SocketChannel.open(StandardProtocolFamily.UNIX).use { channel ->
                    channel.connect(UnixDomainSocketAddress.of(socketPath))
                    val bytes = (message.line + "\n").toByteArray(StandardCharsets.UTF_8)
                    val buffer = ByteBuffer.wrap(bytes)
                    while (buffer.hasRemaining()) channel.write(buffer)
                }
            } catch (_: IOException) {
                // The first process is starting or going away; there is nobody to tell, and this one exits anyway.
            }
        }

        // Windows lets only the foreground process give the focus away; this process, about to exit, hands it on so
        // the running window comes to the front instead of only flashing in the taskbar.
        private fun allowForeground() {
            if (!System.getProperty("os.name").orEmpty().startsWith("Windows", ignoreCase = true)) return
            runCatching { Native.load("user32", User32::class.java).AllowSetForegroundWindow(ASFW_ANY) }
        }
    }
}

/** The first line of the connection, or `null` when it never came within the deadline or was too long. */
private fun SocketChannel.readLine(): String? {
    configureBlocking(false)
    val line = ByteArrayOutputStream()
    val buffer = ByteBuffer.allocate(MAX_LINE_BYTES)
    val deadline = System.nanoTime() + READ_DEADLINE_MILLIS * 1_000_000
    while (System.nanoTime() < deadline) {
        buffer.clear()
        val count = read(buffer)
        if (count > 0) {
            buffer.flip()
            while (buffer.hasRemaining()) {
                val byte = buffer.get()
                if (byte == '\n'.code.toByte()) return line.toString(StandardCharsets.UTF_8)
                line.write(byte.toInt())
                if (line.size() > MAX_LINE_BYTES) return null
            }
        } else if (count < 0) {
            // The peer closed without a line break: what it sent is still one line.
            return line.toString(StandardCharsets.UTF_8).takeIf { line.size() > 0 }
        } else {
            Thread.sleep(READ_POLL_MILLIS)
        }
    }
    return null
}
