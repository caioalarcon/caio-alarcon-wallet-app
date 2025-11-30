package com.example.carteiradepagamentos

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.streams.toList
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchitectureTest {

    private val sourceRoot: Path = Paths.get("app/src/main/java/com/example/carteiradepagamentos")

    @Test
    fun `domain layer should not depend on android, data or ui`() {
        val violations = findViolations(
            layerPath = "domain",
            forbiddenImports = listOf(
                "import android.",
                "import androidx.",
                "import com.example.carteiradepagamentos.data",
                "import com.example.carteiradepagamentos.ui",
            )
        )

        assertTrue("Domain layer contains forbidden imports:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `data layer should not depend on ui`() {
        val violations = findViolations(
            layerPath = "data",
            forbiddenImports = listOf("import com.example.carteiradepagamentos.ui")
        )

        assertTrue("Data layer depends on ui:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    @Test
    fun `ui layer should only rely on domain interfaces`() {
        val violations = findViolations(
            layerPath = "ui",
            forbiddenImports = listOf("import com.example.carteiradepagamentos.data")
        )

        assertTrue("UI layer depends on data implementations:\n" + violations.joinToString("\n"), violations.isEmpty())
    }

    private fun findViolations(layerPath: String, forbiddenImports: List<String>): List<String> {
        val layerRoot = sourceRoot.resolve(layerPath)
        if (!Files.exists(layerRoot)) return emptyList()

        return Files.walk(layerRoot)
            .filter { path -> path.isRegularFile() && path.toString().endsWith(".kt") }
            .flatMap { path ->
                Files.readAllLines(path).mapIndexedNotNull { index, line ->
                    val trimmedLine = line.trim()
                    val forbidden = forbiddenImports.firstOrNull { trimmedLine.startsWith(it) }
                    if (forbidden != null) {
                        "${layerRoot.relativize(path)}:${index + 1} -> $forbidden"
                    } else {
                        null
                    }
                }.stream()
            }
            .toList()
    }
}
