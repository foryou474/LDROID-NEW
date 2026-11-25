package com.example.data.terminal

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.os.StatFs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class TerminalLine(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isError: Boolean = false,
    val isPrompt: Boolean = false
)

data class TerminalSession(
    val id: String,
    val title: String,
    val workingDir: String,
    val lines: List<TerminalLine> = emptyList(),
    val isRunning: Boolean = false,
    val commandHistory: List<String> = emptyList()
)

data class ToolchainProbeReport(
    val androidVersion: String,
    val apiLevel: Int,
    val deviceModel: String,
    val supportedAbis: List<String>,
    val cpuCores: Int,
    val totalRamMb: Long,
    val availRamMb: Long,
    val javaHeapUsedMb: Long,
    val javaHeapMaxMb: Long,
    val nativeHeapMb: Long,
    val freeStorageMb: Long,
    val shellPath: String?,
    val toyboxAvailable: Boolean,
    val adbBinaryFound: Boolean,
    val gradleBinaryFound: Boolean,
    val javaCompilerFound: Boolean,
    val virtualizationNote: String
)

class TerminalSessionManager(private val context: Context) {

    private val _sessions = MutableStateFlow<List<TerminalSession>>(emptyList())
    val sessions: StateFlow<List<TerminalSession>> = _sessions.asStateFlow()

    private val activeProcesses = ConcurrentHashMap<String, Process>()

    fun ensureDefaultSession(workDir: String): String {
        val current = _sessions.value
        if (current.isNotEmpty()) return current.first().id
        return createSession(workDir, "Shell 1")
    }

    fun createSession(workDir: String, title: String = "Shell ${_sessions.value.size + 1}"): String {
        val id = UUID.randomUUID().toString().take(8)
        val session = TerminalSession(
            id = id,
            title = title,
            workingDir = workDir,
            lines = listOf(
                TerminalLine(text = "LDROID Terminal Session [$title]", isPrompt = true),
                TerminalLine(text = "Working dir: $workDir"),
                TerminalLine(text = "Type a shell command (e.g., ls -la, pwd, uname -a, env) or 'clear'.")
            )
        )
        _sessions.update { it + session }
        return id
    }

    fun closeSession(sessionId: String) {
        cancelCommand(sessionId)
        _sessions.update { list ->
            val filtered = list.filterNot { it.id == sessionId }
            filtered
        }
    }

    fun updateWorkingDirectory(sessionId: String, newDir: String) {
        _sessions.update { list ->
            list.map { if (it.id == sessionId) it.copy(workingDir = newDir) else it }
        }
    }

    suspend fun runCommand(sessionId: String, rawCommand: String) = withContext(Dispatchers.IO) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return@withContext

        if (cmd == "clear") {
            _sessions.update { list ->
                list.map { s ->
                    if (s.id == sessionId) s.copy(lines = emptyList(), commandHistory = (s.commandHistory + cmd).takeLast(30))
                    else s
                }
            }
            return@withContext
        }

        val session = _sessions.value.firstOrNull { it.id == sessionId } ?: return@withContext
        val dir = File(session.workingDir).takeIf { it.exists() && it.isDirectory } ?: context.filesDir

        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId) {
                    s.copy(
                        isRunning = true,
                        commandHistory = (s.commandHistory + cmd).takeLast(30),
                        lines = (s.lines + TerminalLine(text = "$ $cmd", isPrompt = true)).takeLast(400)
                    )
                } else s
            }
        }

        // Intercept explicit `cd` so working directory persists cleanly inside the session
        if (cmd == "cd" || cmd.startsWith("cd ")) {
            val arg = cmd.removePrefix("cd").trim()
            val target = if (arg.isEmpty()) context.filesDir else File(dir, arg).canonicalFile
            val outputLine = if (target.exists() && target.isDirectory) {
                updateWorkingDirectory(sessionId, target.absolutePath)
                TerminalLine(text = target.absolutePath)
            } else {
                TerminalLine(text = "cd: no such directory: $arg", isError = true)
            }
            _sessions.update { list ->
                list.map { s ->
                    if (s.id == sessionId) s.copy(isRunning = false, lines = (s.lines + outputLine).takeLast(400))
                    else s
                }
            }
            return@withContext
        }

        try {
            val pb = ProcessBuilder("/system/bin/sh", "-c", cmd)
                .directory(dir)
                .redirectErrorStream(false)
            val proc = pb.start()
            activeProcesses[sessionId] = proc

            val stdoutThread = Thread {
                BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        appendLine(sessionId, TerminalLine(text = line, isError = false))
                        line = reader.readLine()
                    }
                }
            }
            val stderrThread = Thread {
                BufferedReader(InputStreamReader(proc.errorStream)).use { reader ->
                    var line = reader.readLine()
                    while (line != null) {
                        appendLine(sessionId, TerminalLine(text = line, isError = true))
                        line = reader.readLine()
                    }
                }
            }
            stdoutThread.start()
            stderrThread.start()
            val exitCode = proc.waitFor()
            stdoutThread.join(1500)
            stderrThread.join(1500)
            activeProcesses.remove(sessionId)

            appendLine(
                sessionId,
                TerminalLine(
                    text = "[exit $exitCode]",
                    isError = exitCode != 0,
                    isPrompt = exitCode == 0
                )
            )
        } catch (e: Exception) {
            appendLine(sessionId, TerminalLine(text = "Execution error: ${e.message}", isError = true))
        } finally {
            activeProcesses.remove(sessionId)
            _sessions.update { list ->
                list.map { s -> if (s.id == sessionId) s.copy(isRunning = false) else s }
            }
        }
    }

    fun cancelCommand(sessionId: String) {
        val proc = activeProcesses.remove(sessionId)
        if (proc != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                proc.destroyForcibly()
            } else {
                proc.destroy()
            }
            appendLine(sessionId, TerminalLine(text = "^C Process cancelled by user", isError = true))
        }
        _sessions.update { list ->
            list.map { s -> if (s.id == sessionId) s.copy(isRunning = false) else s }
        }
    }

    private fun appendLine(sessionId: String, line: TerminalLine) {
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId) s.copy(lines = (s.lines + line).takeLast(400)) else s
            }
        }
    }

    suspend fun readOwnProcessLogcat(filterTag: String = "", minPriority: String = "V"): List<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pid = android.os.Process.myPid()
                val cmd = if (filterTag.isBlank()) {
                    "logcat -d -v time --pid=$pid *:$minPriority | tail -n 120"
                } else {
                    "logcat -d -v time --pid=$pid | grep -i '${filterTag.replace("'", "")}' | tail -n 120"
                }
                val proc = ProcessBuilder("/system/bin/sh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start()
                val lines = proc.inputStream.bufferedReader().readLines()
                proc.waitFor()
                if (lines.isEmpty()) {
                    listOf("No matching logcat entries returned for PID $pid.")
                } else lines
            }.getOrElse { e ->
                listOf("Logcat unavailable in current sandbox: ${e.message}")
            }
        }

    suspend fun probeToolchainAndDevice(): ToolchainProbeReport = withContext(Dispatchers.IO) {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val runtime = Runtime.getRuntime()
        val heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)
        val heapMax = runtime.maxMemory() / (1024L * 1024L)
        val nativeHeap = Debug.getNativeHeapAllocatedSize() / (1024L * 1024L)

        val statFs = StatFs(context.filesDir.absolutePath)
        val freeMb = statFs.availableBytes / (1024L * 1024L)

        fun binaryExists(bin: String): Boolean {
            return runCatching {
                val p = ProcessBuilder("/system/bin/sh", "-c", "command -v $bin").start()
                p.waitFor() == 0
            }.getOrDefault(false)
        }

        val shellBin = listOf("/system/bin/sh", "/bin/sh").firstOrNull { File(it).exists() }
        val toybox = binaryExists("toybox")
        val adb = binaryExists("adb")
        val gradle = binaryExists("gradle")
        val javac = binaryExists("javac") || binaryExists("java")

        ToolchainProbeReport(
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            apiLevel = Build.VERSION.SDK_INT,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            supportedAbis = Build.SUPPORTED_ABIS.toList(),
            cpuCores = runtime.availableProcessors(),
            totalRamMb = memInfo.totalMem / (1024L * 1024L),
            availRamMb = memInfo.availMem / (1024L * 1024L),
            javaHeapUsedMb = heapUsed,
            javaHeapMaxMb = heapMax,
            nativeHeapMb = nativeHeap,
            freeStorageMb = freeMb,
            shellPath = shellBin,
            toyboxAvailable = toybox,
            adbBinaryFound = adb,
            gradleBinaryFound = gradle,
            javaCompilerFound = javac,
            virtualizationNote = if (!gradle && !adb) {
                "Running inside sandboxed Android OS: local shell works, while full Gradle/JDK/AVD builds require host CI (GitHub Actions) or Termux/host toolchain."
            } else {
                "Host toolchain binaries detected on PATH."
            }
        )
    }
}
