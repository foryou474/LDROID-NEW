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
    val commandHistory: List<String> = emptyList(),
    val lastExitCode: Int? = null
)

data class TerminalCommandHooks(
    val onWorkspaceChanged: suspend () -> Unit = {},
    val onGitStatus: suspend () -> List<TerminalLine> = { emptyList() },
    val onGitDiff: suspend () -> List<TerminalLine> = { emptyList() },
    val onGradleBuild: suspend (String) -> Pair<Int, List<TerminalLine>> = { 0 to emptyList() }
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

    private fun shortPromptName(workDir: String): String {
        val rawName = File(workDir).name.ifBlank { "MaxStarterApp" }
        return rawName.replace(Regex("""_[0-9a-f]{8}$"""), "").ifBlank { rawName }
    }

    fun createSession(workDir: String, title: String = "Shell ${_sessions.value.size + 1}"): String {
        val id = UUID.randomUUID().toString().take(8)
        val promptLabel = shortPromptName(workDir)
        val session = TerminalSession(
            id = id,
            title = title,
            workingDir = workDir,
            lines = listOf(
                TerminalLine(text = "M.A.X. Studio Terminal [$title] — type 'help' for commands", isPrompt = true),
                TerminalLine(text = "max@studio:~/$promptLabel $ ready")
            )
        )
        _sessions.update { it + session }
        return id
    }

    fun closeSession(sessionId: String) {
        cancelCommand(sessionId)
        _sessions.update { list ->
            list.filterNot { it.id == sessionId }
        }
    }

    fun clearSession(sessionId: String) {
        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId) {
                    val promptLabel = shortPromptName(s.workingDir)
                    s.copy(lines = listOf(TerminalLine(text = "max@studio:~/$promptLabel $", isPrompt = true)))
                } else s
            }
        }
    }

    fun updateWorkingDirectory(sessionId: String, newDir: String) {
        _sessions.update { list ->
            list.map { if (it.id == sessionId) it.copy(workingDir = newDir) else it }
        }
    }

    suspend fun runCommand(
        sessionId: String,
        rawCommand: String,
        hooks: TerminalCommandHooks = TerminalCommandHooks()
    ) = withContext(Dispatchers.IO) {
        val cmd = rawCommand.trim()
        if (cmd.isEmpty()) return@withContext

        if (cmd == "clear") {
            _sessions.update { list ->
                list.map { s ->
                    if (s.id == sessionId) s.copy(lines = emptyList(), commandHistory = (s.commandHistory + cmd).takeLast(30), lastExitCode = 0)
                    else s
                }
            }
            return@withContext
        }

        val session = _sessions.value.firstOrNull { it.id == sessionId } ?: return@withContext
        val dir = File(session.workingDir).takeIf { it.exists() && it.isDirectory } ?: context.filesDir
        val promptLabel = shortPromptName(dir.absolutePath)

        _sessions.update { list ->
            list.map { s ->
                if (s.id == sessionId) {
                    s.copy(
                        isRunning = true,
                        commandHistory = (s.commandHistory + cmd).takeLast(30),
                        lines = (s.lines + TerminalLine(text = "max@studio:~/$promptLabel $ $cmd", isPrompt = true)).takeLast(400)
                    )
                } else s
            }
        }

        val parts = cmd.split(Regex("""\s+"""))
        val head = parts.firstOrNull().orEmpty().lowercase()
        val args = parts.drop(1)

        // Built-in workspace-aware commands (guaranteed functional on every Android device/sandbox)
        val builtInResult: Pair<Int, List<TerminalLine>>? = when {
            head == "help" -> 0 to listOf(
                TerminalLine(text = "M.A.X. Studio Workspace Commands:", isPrompt = true),
                TerminalLine(text = "  ls [dir]        List files & folders in workspace"),
                TerminalLine(text = "  pwd             Print current working directory"),
                TerminalLine(text = "  cd <dir>        Change directory"),
                TerminalLine(text = "  cat <file>      Print file contents"),
                TerminalLine(text = "  mkdir <dir>     Create directory"),
                TerminalLine(text = "  touch <file>    Create empty file"),
                TerminalLine(text = "  rm <path>       Delete file or folder"),
                TerminalLine(text = "  git status      Show modified/added/deleted workspace files"),
                TerminalLine(text = "  git diff        Show unified diff against baseline snapshot"),
                TerminalLine(text = "  gradle build    Run full workspace syntax & lint verification"),
                TerminalLine(text = "  clear           Clear terminal output")
            )
            head == "pwd" -> 0 to listOf(
                TerminalLine(text = "~/$promptLabel (${dir.absolutePath})")
            )
            head == "cd" -> {
                val arg = cmd.removePrefix("cd").trim()
                val target = if (arg.isEmpty()) context.filesDir else File(dir, arg).canonicalFile
                if (target.exists() && target.isDirectory) {
                    updateWorkingDirectory(sessionId, target.absolutePath)
                    0 to listOf(TerminalLine(text = target.absolutePath))
                } else {
                    1 to listOf(TerminalLine(text = "cd: no such directory: $arg", isError = true))
                }
            }
            head == "ls" -> handleLs(dir, args)
            head == "cat" -> handleCat(dir, args)
            head == "mkdir" -> handleMkdir(dir, args).also { (code, _) -> if (code == 0) hooks.onWorkspaceChanged() }
            head == "touch" -> handleTouch(dir, args).also { (code, _) -> if (code == 0) hooks.onWorkspaceChanged() }
            head == "rm" -> handleRm(dir, args).also { (code, _) -> if (code == 0) hooks.onWorkspaceChanged() }
            cmd.equals("git status", ignoreCase = true) -> 0 to hooks.onGitStatus()
            cmd.equals("git diff", ignoreCase = true) -> 0 to hooks.onGitDiff()
            head == "gradle" || head == "./gradlew" || head == "gradlew" -> {
                val task = args.firstOrNull() ?: "build"
                hooks.onGradleBuild(task)
            }
            else -> null
        }

        if (builtInResult != null) {
            val (exitCode, outLines) = builtInResult
            _sessions.update { list ->
                list.map { s ->
                    if (s.id == sessionId) {
                        val suffix = if (exitCode == 0) emptyList() else listOf(TerminalLine(text = "[exit $exitCode]", isError = true))
                        s.copy(
                            isRunning = false,
                            lastExitCode = exitCode,
                            lines = (s.lines + outLines + suffix).takeLast(400)
                        )
                    } else s
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

            _sessions.update { list ->
                list.map { s ->
                    if (s.id == sessionId) {
                        s.copy(
                            lastExitCode = exitCode,
                            lines = (s.lines + TerminalLine(
                                text = "[exit $exitCode]",
                                isError = exitCode != 0,
                                isPrompt = exitCode == 0
                            )).takeLast(400)
                        )
                    } else s
                }
            }
        } catch (e: Exception) {
            appendLine(sessionId, TerminalLine(text = "Execution error: ${e.message}", isError = true))
        } finally {
            activeProcesses.remove(sessionId)
            _sessions.update { list ->
                list.map { s -> if (s.id == sessionId) s.copy(isRunning = false) else s }
            }
        }
    }

    private fun handleLs(dir: File, args: List<String>): Pair<Int, List<TerminalLine>> {
        val showAll = args.any { it.startsWith("-") && it.contains("a") }
        val longFmt = args.any { it.startsWith("-") && it.contains("l") }
        val pathArg = args.firstOrNull { !it.startsWith("-") }
        val target = if (pathArg == null) dir else File(dir, pathArg).canonicalFile
        if (!target.exists()) {
            return 1 to listOf(TerminalLine(text = "ls: cannot access '${pathArg ?: "."}': No such file or directory", isError = true))
        }
        val entries = if (target.isDirectory) {
            target.listFiles()
                ?.filter { showAll || !it.name.startsWith(".") }
                ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name })
                .orEmpty()
        } else listOf(target)
        if (entries.isEmpty()) return 0 to listOf(TerminalLine(text = "(empty directory)"))
        val lines = entries.map { f ->
            val label = if (f.isDirectory) "${f.name}/" else f.name
            if (longFmt) {
                val typeChar = if (f.isDirectory) "d" else "-"
                val size = if (f.isDirectory) "-" else "${f.length()}B"
                TerminalLine(text = "$typeChar  ${size.padStart(8)}  $label")
            } else {
                TerminalLine(text = label)
            }
        }
        return 0 to lines
    }

    private fun handleCat(dir: File, args: List<String>): Pair<Int, List<TerminalLine>> {
        val pathArg = args.firstOrNull()
            ?: return 1 to listOf(TerminalLine(text = "usage: cat <relative-file-path>", isError = true))
        val target = File(dir, pathArg).canonicalFile
        if (!target.exists() || !target.isFile) {
            return 1 to listOf(TerminalLine(text = "cat: $pathArg: No such file", isError = true))
        }
        if (target.length() > 128 * 1024) {
            return 1 to listOf(TerminalLine(text = "cat: $pathArg exceeds 128KB terminal preview limit", isError = true))
        }
        val contentLines = runCatching { target.readLines() }.getOrElse {
            return 1 to listOf(TerminalLine(text = "cat: failed reading $pathArg: ${it.message}", isError = true))
        }
        return 0 to contentLines.take(200).mapIndexed { idx, line ->
            TerminalLine(text = "${(idx + 1).toString().padStart(3)} | $line")
        }
    }

    private fun handleMkdir(dir: File, args: List<String>): Pair<Int, List<TerminalLine>> {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            return 1 to listOf(TerminalLine(text = "usage: mkdir <directory>", isError = true))
        }
        val out = mutableListOf<TerminalLine>()
        var code = 0
        for (rel in targets) {
            val target = File(dir, rel).canonicalFile
            if (!target.absolutePath.startsWith(dir.canonicalPath)) {
                out.add(TerminalLine(text = "mkdir: blocked path outside workspace: $rel", isError = true))
                code = 1
                continue
            }
            if (target.exists()) {
                out.add(TerminalLine(text = "mkdir: '$rel' already exists", isError = true))
                code = 1
            } else if (target.mkdirs()) {
                out.add(TerminalLine(text = "Created directory '$rel'"))
            } else {
                out.add(TerminalLine(text = "mkdir: failed to create '$rel'", isError = true))
                code = 1
            }
        }
        return code to out
    }

    private fun handleTouch(dir: File, args: List<String>): Pair<Int, List<TerminalLine>> {
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            return 1 to listOf(TerminalLine(text = "usage: touch <file>", isError = true))
        }
        val out = mutableListOf<TerminalLine>()
        var code = 0
        for (rel in targets) {
            val target = File(dir, rel).canonicalFile
            if (!target.absolutePath.startsWith(dir.canonicalPath)) {
                out.add(TerminalLine(text = "touch: blocked path outside workspace: $rel", isError = true))
                code = 1
                continue
            }
            target.parentFile?.mkdirs()
            if (!target.exists()) target.createNewFile() else target.setLastModified(System.currentTimeMillis())
            out.add(TerminalLine(text = "Updated '$rel'"))
        }
        return code to out
    }

    private fun handleRm(dir: File, args: List<String>): Pair<Int, List<TerminalLine>> {
        val recursive = args.any { it.startsWith("-") && (it.contains("r") || it.contains("f")) }
        val targets = args.filter { !it.startsWith("-") }
        if (targets.isEmpty()) {
            return 1 to listOf(TerminalLine(text = "usage: rm [-rf] <path>", isError = true))
        }
        val out = mutableListOf<TerminalLine>()
        var code = 0
        val rootCanon = dir.canonicalPath
        for (rel in targets) {
            val target = File(dir, rel).canonicalFile
            if (!target.absolutePath.startsWith(rootCanon) || target.absolutePath == rootCanon) {
                out.add(TerminalLine(text = "rm: refused to remove '$rel'", isError = true))
                code = 1
                continue
            }
            if (!target.exists()) {
                out.add(TerminalLine(text = "rm: '$rel': No such file or directory", isError = true))
                code = 1
            } else if (target.isDirectory && !recursive && !target.list().isNullOrEmpty()) {
                out.add(TerminalLine(text = "rm: cannot remove '$rel': Is a non-empty directory (use rm -rf)", isError = true))
                code = 1
            } else {
                val ok = if (target.isDirectory) target.deleteRecursively() else target.delete()
                if (ok) out.add(TerminalLine(text = "Removed '$rel'"))
                else {
                    out.add(TerminalLine(text = "rm: failed removing '$rel'", isError = true))
                    code = 1
                }
            }
        }
        return code to out
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
                "Running inside sandboxed Android OS: workspace shell (ls, cat, mkdir, rm, git, gradle lint) works locally; full APK compilation uses GitHub Actions CI."
            } else {
                "Host toolchain binaries detected on PATH."
            }
        )
    }
}
