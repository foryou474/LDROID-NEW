package com.example.data.audit

import com.example.model.MilestoneAuditItem

object RoadmapRegistry {
    val m01ToM50Audit: List<MilestoneAuditItem> = listOf(
        MilestoneAuditItem("M01", "Architecture & Gradle Foundation", "VERIFIED", "MVVM + Repository + Room + DataStore + AGP 9.1 / SDK 36"),
        MilestoneAuditItem("M02", "Premium Design System & Animations", "VERIFIED", "10 complete themes, semantic tokens, WCAG AA checks, Inter + JetBrains Mono"),
        MilestoneAuditItem("M03", "Dashboard & Navigation", "VERIFIED", "5 primary destinations + adaptive NavigationBar/NavigationRail + Developer Lab"),
        MilestoneAuditItem("M04", "Project Manager & Templates", "VERIFIED", "Compose/Multi-Module/Web templates, SAF import, safe Zip-Slip ZIP importer"),
        MilestoneAuditItem("M05", "File Explorer & Workspace", "VERIFIED", "Create, rename, copy, delete, search, ZIP browser, Image/PDF/Binary previews"),
        MilestoneAuditItem("M06", "Professional Code Editor", "VERIFIED", "Syntax highlighting, line numbers, find/replace, undo/redo, encoding/LF/CRLF, atomic save"),
        MilestoneAuditItem("M07", "Language Intelligence & LSP Integration", "IMPLEMENTED", "On-device static analyzer for Kotlin/XML/JSON/Manifest & Compose AST preview"),
        MilestoneAuditItem("M08", "Advanced Visual Builder", "VERIFIED", "Visual component tree editor + real Kotlin Compose & Android XML codegen"),
        MilestoneAuditItem("M09", "AI Gateway (BYOK)", "VERIFIED", "Encrypted AndroidKeyStore vault + real Gemini/OpenAI/Anthropic OkHttp client"),
        MilestoneAuditItem("M10", "AI Agent Engine & Multi-File Changes", "VERIFIED", "Architecture graph scanner + reviewable diff proposals requiring explicit approval"),
        MilestoneAuditItem("M11", "Advanced Integrated Terminal", "VERIFIED", "Real ProcessBuilder shell sessions, streaming stdout/stderr, history & process cancel"),
        MilestoneAuditItem("M12", "Local Execution & Toolchain Manager", "IMPLEMENTED", "Live ART/shell/storage/ABI probe; host Gradle/JDK flagged when absent on phone"),
        MilestoneAuditItem("M13", "Backend, Cloud Sync & Storage", "IMPLEMENTED", "Offline-first guest mode + SAF ZIP backup; remote cloud sync optional"),
        MilestoneAuditItem("M14", "Cloud Build Infrastructure", "IMPLEMENTED", "Gradle task/module scanner + GitHub Actions CI workflow generator & monitor"),
        MilestoneAuditItem("M15", "Preview Center & Device Lab", "VERIFIED", "Code Live Preview (last-good fallback) + Visual UI Live Preview + device frames"),
        MilestoneAuditItem("M16", "Advanced Debugger & Logcat", "IMPLEMENTED", "Real own-PID Logcat reader with tag/priority filters & AI explanation"),
        MilestoneAuditItem("M17", "APK/AAB Build, Signing & Export", "VERIFIED", "Real APK/AAB/ZIP analyzer: DEX, Manifest, certs, size breakdown & SHA-256"),
        MilestoneAuditItem("M18", "Git & Version Control", "VERIFIED", "Local baseline unified diff tracker + GitHub REST repo & workflow inspector"),
        MilestoneAuditItem("M19", "Dependency, Asset, API & Database Studios", "IMPLEMENTED", "Manifest/XML/JSON/Version Catalog inspection & asset previews"),
        MilestoneAuditItem("M20", "Core Testing, Security & Performance", "VERIFIED", "Secret scanner, permission heuristics & Robolectric/unit test suite"),
        MilestoneAuditItem("M21", "Account & Authentication", "VERIFIED", "Guest-first operation; optional encrypted BYOK & GitHub PAT tokens"),
        MilestoneAuditItem("M22", "Complete Settings & Personalization", "VERIFIED", "10 themes, live preview Apply/Cancel, JSON export/import, reduced motion"),
        MilestoneAuditItem("M23", "Advanced QA Automation", "IMPLEMENTED", "On-device Testing Lab + Robolectric CUJ tests + GitHub Actions CI suite"),
        MilestoneAuditItem("M24", "Performance Monitoring & Crash Analytics", "VERIFIED", "Live JVM/Native heap, RAM, CPU & storage profiler"),
        MilestoneAuditItem("M25", "Backup & Disaster Recovery", "VERIFIED", "Workspace snapshots + full project ZIP export via SAF"),
        MilestoneAuditItem("M32", "Secrets & Environment Manager", "VERIFIED", "Keystore AES-GCM vault, secret scanning & redaction before AI dispatch"),
        MilestoneAuditItem("M33", "Code Quality & Static Analysis", "VERIFIED", "Kotlin/XML/JSON/Manifest lint & balanced-syntax checks"),
        MilestoneAuditItem("M34", "Software Supply Chain & SBOM", "IMPLEMENTED", "Gradle dependency inspection, SHA-256 artifact digests & CI workflow"),
        MilestoneAuditItem("M42", "Multi-Module Build Engineering", "IMPLEMENTED", "Multi-module template, version catalog & module dependency scanner"),
        MilestoneAuditItem("M47", "Accessibility Studio", "VERIFIED", "WCAG 2.2 AA contrast audit across all 10 themes + 48dp touch targets"),
        MilestoneAuditItem("M50", "AI Reliability & Final Release Gate", "IMPLEMENTED", "Traceability matrix, Robolectric verification & GitHub Actions release pipeline")
    )
}
