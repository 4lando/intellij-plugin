package dev._4lando.intellij.ui.console

import com.jediterm.terminal.TtyConnector
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class LandoTtyConnector(private val process: Process,
                        private val consoleView: JeditermConsoleView
) : TtyConnector {

    var charset: Charset = StandardCharsets.UTF_8
    var localEcho: Boolean = false


    override fun read(buf: CharArray, offset: Int, length: Int): Int {
        return consoleView.readChars(buf, offset, length)
    }

    override fun write(bytes: ByteArray) =
        process.outputStream.write(bytes)

    override fun write(string: String) =
        write(string.toByteArray(charset))

    override fun isConnected(): Boolean = true

    override fun waitFor(): Int = 0

    override fun ready(): Boolean {
        return this.isConnected()
    }

    override fun getName(): String = process.pid().toString()

    override fun close() {
        process.outputStream.close()
        consoleView.output("TTY connection closed.".toByteArray(StandardCharsets.UTF_8))
    }

}