package dev._4lando.intellij

import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.project.Project
import dev._4lando.intellij.services.LandoProjectService
import dev._4lando.intellij.ui.console.JeditermConsoleView
import java.io.File
import java.io.OutputStream

/**
 * Class for executing Lando commands.
 * @param command The Lando command to execute.
 */
class LandoExec(private val command: String) {
    val landoBin = "lando"

    private var directory: String = ""

    private var format: String = ""

    private var attachToConsole: Boolean = true

    var project: Project? = null

    /**
     * Sets the directory for the Lando command.
     * @param path The directory path.
     * @return The LandoExec instance.
     */
    fun directory(path: String): LandoExec {
        directory = path
        return this
    }

    /**
     * Sets the format for the Lando command.
     * @param format The format.
     * @return The LandoExec instance.
     */
    fun format(format: Format): LandoExec {
        this.format = format.format
        return this
    }

    /**
     * Sets the Lando command to fetch JSON.
     * @return The LandoExec instance.
     */
    fun fetchJson(): LandoExec {
        attachToConsole = false
        return format(Format.JSON)
    }

    /**
     * Runs the Lando command.
     * @return The ProcessHandler for the process running the Lando command.
     */
    fun run(): ProcessHandler {
        // Create a new process builder with the Lando binary and the command
        val processBuilder = ProcessBuilder(landoBin, command)

        // Get the LandoAppService instance
        val appService = LandoAppService.getInstance()

        // Default to the project root as working directory if none is set
        if (directory.isEmpty() && project != null) {
            directory = LandoProjectService.getInstance(project!!).appRoot?.path!!
        } else if (directory.isEmpty()) {
            directory = System.getProperty("user.dir")
        }
        // Set the directory for the process builder
        processBuilder.directory(File(directory))

        // If a format is set, add it to the command
        if (format.isNotEmpty()) {
            processBuilder.command().add(format)
        }

        // Start the process
        val process = processBuilder.start()

        // Attach process to console view to display output
        var landoConsoleView: JeditermConsoleView? = null
        if (attachToConsole) {
            // Create a new console view with the process
            landoConsoleView = JeditermConsoleView(appService.thisProject.project, process)
        }

        // Create a new process handler
        val processHandler = object : ProcessHandler() {
            // Method to destroy the process
            override fun destroyProcessImpl() {
                process.destroy()
            }

            // Method to detach the process
            override fun detachProcessImpl() {
                destroyProcessImpl()
            }

            // Method to check if detach is default
            override fun detachIsDefault(): Boolean = false

            // Method to get the process input
            override fun getProcessInput(): OutputStream? = null

            // Method to start the notification
            override fun startNotify() {
                super.startNotify()
                if (landoConsoleView == null) {
                    return
                }
                // Start a new thread to read the process output and print it to the console view
                Thread {
                    val reader = process.inputStream.bufferedReader()
                    reader.useLines { lines ->
                        lines.forEach { line ->
                            // Parse the line to remove any ANSI escape codes
                            val formattedLine = AnsiEscapeCodeParser.parse(line)
                            // Print the formatted line to the console view
                            landoConsoleView.print(formattedLine, ConsoleViewContentType.NORMAL_OUTPUT)
                        }
                    }
                }.start()
            }
        }

        // Attach the process handler to the console view
        landoConsoleView?.attachToProcess(processHandler)
        // Start the process handler notification
        processHandler.startNotify()

        // Return the process handler
        return processHandler
    }

    /**
     * Companion object for LandoExec class.
     */
    companion object {
        /**
         * Enum for Lando command formats.
         */
        enum class Format(val format: String) {
            JSON("--format json"),
        }
    }

    /**
     * Object for parsing ANSI escape codes.
     */
    object AnsiEscapeCodeParser {
        fun parse(text: String): String {
            // Basic ANSI escape code removal
            return text.replace(Regex("\\x1B\\[[;\\d]*m"), "")
        }
    }
}