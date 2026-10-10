package com.muse.app

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private sealed interface Block {
    data class Code(val lang: String, val code: String) : Block
    data class Heading(val level: Int, val text: String) : Block
    data class Item(val marker: String, val text: String, val indent: Int) : Block
    data class Para(val text: String) : Block
    data class Table(val rows: List<List<String>>) : Block
    data object Rule : Block
}

private val bulletRe = Regex("^[-*•]\\s+(.*)$")
private val numberedRe = Regex("^(\\d+[.)])\\s+(.*)$")
private val sepCellRe = Regex("^:?-{2,}:?$")

private fun parse(src: String): List<Block> {
    val out = mutableListOf<Block>()
    val lines = src.split("\n")
    var i = 0
    while (i < lines.size) {
        val line = lines[i]
        val t = line.trim()
        if (t.startsWith("```")) {
            val lang = t.removePrefix("```").trim()
            val code = StringBuilder()
            i++
            while (i < lines.size && !lines[i].trim().startsWith("```")) {
                code.append(lines[i]).append('\n')
                i++
            }
            out.add(Block.Code(lang, code.toString().trimEnd('\n')))
            i++
            continue
        }
        if (t.startsWith("|")) {
            val rows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                val cells = lines[i].trim().trim('|').split("|").map { it.trim() }
                if (!cells.all { sepCellRe.matches(it) }) rows.add(cells)
                i++
            }
            out.add(Block.Table(rows))
            continue
        }
        val indent = ((line.length - line.trimStart().length) / 2).coerceAtMost(3)
        val bullet = bulletRe.find(t)
        val numbered = numberedRe.find(t)
        if (t.isEmpty()) {
            // spacing handled by the column arrangement
        } else if (t.startsWith("#")) {
            val lvl = t.takeWhile { it == '#' }.length
            if (lvl in 1..6 && t.getOrNull(lvl) == ' ') out.add(Block.Heading(lvl, t.substring(lvl + 1)))
            else out.add(Block.Para(t))
        } else if (t == "---" || t == "***") {
            out.add(Block.Rule)
        } else if (bullet != null) {
            out.add(Block.Item("•", bullet.groupValues[1], indent))
        } else if (numbered != null) {
            out.add(Block.Item(numbered.groupValues[1], numbered.groupValues[2], indent))
        } else if (t.startsWith(">")) {
            out.add(Block.Para(t.removePrefix(">").trim()))
        } else {
            out.add(Block.Para(t))
        }
        i++
    }
    return out
}

private fun inline(s: String, codeBg: Color, link: Color): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < s.length) {
        val c = s[i]
        if (s.startsWith("**", i)) {
            val e = s.indexOf("**", i + 2)
            if (e > i + 2) {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(s.substring(i + 2, e)) }
                i = e + 2
                continue
            }
        }
        if (c == '`') {
            val e = s.indexOf('`', i + 1)
            if (e > i + 1) {
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBg)) {
                    append(s.substring(i + 1, e))
                }
                i = e + 1
                continue
            }
        }
        if (c == '*' && i + 1 < s.length && s[i + 1] != ' ' && s[i + 1] != '*') {
            val e = s.indexOf('*', i + 1)
            if (e > i + 1 && s[e - 1] != ' ') {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(s.substring(i + 1, e)) }
                i = e + 1
                continue
            }
        }
        if (c == '[') {
            val e = s.indexOf(']', i + 1)
            if (e > i + 1 && s.getOrNull(e + 1) == '(') {
                val f = s.indexOf(')', e + 2)
                if (f > e + 2) {
                    val url = s.substring(e + 2, f)
                    val style = TextLinkStyles(SpanStyle(color = link, textDecoration = TextDecoration.Underline))
                    withLink(LinkAnnotation.Url(url, style)) { append(s.substring(i + 1, e)) }
                    i = f + 1
                    continue
                }
            }
        }
        append(c)
        i++
    }
}

@Composable
fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Serif,
    fontSize: TextUnit = 16.sp,
) {
    val cs = MaterialTheme.colorScheme
    val blocks = remember(text) { parse(text) }
    val base = TextStyle(
        fontFamily = fontFamily,
        fontSize = fontSize,
        lineHeight = fontSize * 1.55f,
        color = cs.onBackground,
    )
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { b ->
            when (b) {
                is Block.Para -> Text(inline(b.text, cs.surfaceVariant, cs.primary), style = base)
                is Block.Heading -> {
                    val size = when (b.level) { 1 -> 22.sp; 2 -> 19.sp; else -> 17.sp }
                    Text(
                        inline(b.text, cs.surfaceVariant, cs.primary),
                        style = base.copy(fontSize = size, lineHeight = size * 1.3f, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                is Block.Item -> Row(Modifier.padding(start = (b.indent * 16).dp)) {
                    Text(b.marker, style = base, color = cs.onSurfaceVariant, modifier = Modifier.width(26.dp))
                    Text(
                        inline(b.text, cs.surfaceVariant, cs.primary),
                        style = base,
                        modifier = Modifier.weight(1f),
                    )
                }
                is Block.Code -> CodeBlock(b.lang, b.code)
                is Block.Table -> TableBlock(b.rows)
                is Block.Rule -> HorizontalDivider(color = cs.outlineVariant)
            }
        }
    }
}

@Composable
private fun CodeBlock(lang: String, code: String) {
    val cs = MaterialTheme.colorScheme
    val clipboard = LocalClipboardManager.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(lang.ifBlank { "code" }, fontSize = 12.sp, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
            TextButton(onClick = { clipboard.setText(AnnotatedString(code)) }) {
                Text("Copy", fontSize = 12.sp, color = cs.onSurfaceVariant)
            }
        }
        Text(
            code,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            color = cs.onSurface,
            modifier = Modifier.horizontalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun TableBlock(rows: List<List<String>>) {
    if (rows.isEmpty()) return
    val cs = MaterialTheme.colorScheme
    val cols = rows.maxOf { it.size }
    val widths = List(cols) { j -> rows.maxOf { r -> r.getOrNull(j)?.length ?: 0 } }
    val text = buildString {
        rows.forEachIndexed { idx, r ->
            append(List(cols) { j -> (r.getOrNull(j) ?: "").padEnd(widths[j]) }.joinToString("  │  "))
            append('\n')
            if (idx == 0 && rows.size > 1) {
                append(widths.joinToString("──┼──") { "─".repeat(it) })
                append('\n')
            }
        }
    }.trimEnd('\n')
    Text(
        text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        color = cs.onSurface,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant)
            .horizontalScroll(rememberScrollState()).padding(12.dp),
    )
}
