package com.essential.services.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

fun parseSimpleMarkdown(markdown: String): List<Pair<String, AnnotatedString>> {
    val lines = markdown.lines()
    val parsed = mutableListOf<Pair<String, AnnotatedString>>()
    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        if (trimmed.startsWith("#")) {
            val titleText = trimmed.removePrefix("#").trimStart('#').trim()
            val annotated = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(titleText)
                }
            }
            parsed.add("HEADER" to annotated)
        } else {
            val cleanLine = if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                "• " + trimmed.substring(1).trim()
            } else trimmed

            val annotated = buildAnnotatedString {
                val parts = cleanLine.split("**")
                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(part)
                        }
                    } else {
                        append(part)
                    }
                }
            }
            parsed.add("ITEM" to annotated)
        }
    }
    return parsed
}

@Composable
fun CreditsMarkdownUI(
    fileName: String = "credits.md",
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var markdownContent by remember { mutableStateOf("") }

    LaunchedEffect(fileName) {
        try {
            markdownContent = context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (_: Exception) {
            markdownContent = "# Credits to...\n\n- **mariohdel14nasa-lang**: Developer and Maintainer\n- **sameerasw**: Original UI"
        }
    }

    val parsedLines = remember(markdownContent) { parseSimpleMarkdown(markdownContent) }

    Column(modifier = modifier.padding(16.dp)) {
        parsedLines.forEach { (type, text) ->
            if (type == "HEADER") {
                Text(
                    text = text,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            } else {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
