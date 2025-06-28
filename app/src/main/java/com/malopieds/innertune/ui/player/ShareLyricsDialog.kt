package com.malopieds.innertune.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat
import com.malopieds.innertune.R
import com.malopieds.innertune.models.MediaMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.draw.clip
import com.malopieds.innertune.ui.player.isDark
import com.malopieds.innertune.LocalPlayerConnection
import com.malopieds.innertune.lyrics.LyricsUtils
import kotlin.math.max
import kotlin.math.min
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

@Composable
fun ShareLyricsDialog(
    lyrics: String,
    mediaMetadata: MediaMetadata,
    onDismiss: () -> Unit,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()
    var isProcessing by remember { mutableStateOf(false) }
    val paletteColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        Color(0xFF333333),
        Color(0xFF666666),
        Color(0xFFCCCCCC)
    )
    var selectedBackground by remember { mutableStateOf<Any>(gradientColors) }

    val backgroundBrush = remember(selectedBackground) {
        when (val selection = selectedBackground) {
            is Color -> SolidColor(selection)
            is List<*> -> Brush.linearGradient(selection.filterIsInstance<Color>())
            else -> Brush.linearGradient(gradientColors)
        }
    }
    val isDarkBackground = remember(selectedBackground) {
        when (val bg = selectedBackground) {
            is Color -> bg.isDark()
            is List<*> -> true
            else -> true
        }
    }
    val textColor = if (isDarkBackground) Color.White else Color.Black
    val artistNames = mediaMetadata.artists.joinToString { it.name }
    val playerConnection = LocalPlayerConnection.current

    // Parse lyrics into lines for selection
    val lyricLines = remember(lyrics) { lyrics.lines() }
    val parsedLyrics = remember(lyrics) {
        if (lyrics.startsWith("[")) LyricsUtils.parseLyrics(lyrics) else null
    }

    val (visibleLines, initialSelection) = remember(lyricLines, parsedLyrics, playerConnection) {
        val currentPos = playerConnection?.player?.currentPosition ?: 0L
        val totalLines = parsedLyrics?.size ?: lyricLines.size
        val currentIndex = parsedLyrics?.let { LyricsUtils.findCurrentLineIndex(it, currentPos) } ?: (lyricLines.indexOfFirst { it.isNotBlank() }.takeIf { it != -1 } ?: 0)

        val selectionStart = currentIndex
        val selectionEnd = min(totalLines - 1, currentIndex + 9)
        val initialSelectedIndices = (selectionStart..selectionEnd).toSet()

        // Show all lines, not just a snippet
        val allLinesWithIndex = lyricLines.mapIndexed { index, line -> (index) to line }
        allLinesWithIndex to initialSelectedIndices
    }

    var selectedIndices by remember { mutableStateOf(initialSelection) }
    var userHasSelected by remember { mutableStateOf(false) }

    val shareableLyrics = remember(selectedIndices, lyricLines) {
        lyricLines.filterIndexed { index, _ -> selectedIndices.contains(index) }.joinToString("\n")
    }

    Dialog(onDismissRequest = { if (!isProcessing) onDismiss() }) {
        Box(contentAlignment = Alignment.Center) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                tonalElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Preview Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.7f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxHeight()
                        ) {
                            // Scrollable lyrics preview
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                contentPadding = PaddingValues(top = 50.dp, bottom = 12.dp)
                            ) {
                                items(visibleLines, key = { it.first }) { (idx, line) ->
                                    val isSelected = selectedIndices.contains(idx)
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = (if (isSelected) MaterialTheme.colorScheme.primary else textColor).copy(alpha = if (isSelected || !userHasSelected) 1f else 0.5f),
                                        maxLines = 2,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                if (!userHasSelected) {
                                                    userHasSelected = true
                                                    selectedIndices = setOf(idx)
                                                } else {
                                                    selectedIndices = if (isSelected) {
                                                        selectedIndices - idx
                                                    } else {
                                                        selectedIndices + idx
                                                    }
                                                }
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = textColor,
                                maxLines = 2,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = artistNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 1,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Color Swatches
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gradient swatch
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(gradientColors))
                                .clickable(enabled = !isProcessing) { selectedBackground = gradientColors }
                                .border(
                                    width = if (selectedBackground == gradientColors) 2.dp else 0.dp,
                                    color = if (selectedBackground == gradientColors) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                        )
                        paletteColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(color)
                                    .clickable(enabled = !isProcessing) { selectedBackground = color }
                                    .border(
                                        width = if (selectedBackground == color) 2.dp else 0.dp,
                                        color = if (selectedBackground == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "To share on Instagram, tap 'Share as Image' and select 'Stories' in the share sheet.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    // Share Options
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShareOptionButton(
                            icon = painterResource(R.drawable.copy),
                            label = "Copy Lyrics",
                            enabled = !isProcessing,
                            onClick = {
                                clipboardManager.setText(AnnotatedString(shareableLyrics))
                                Toast.makeText(context, "Lyrics copied!", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        )
                        ShareOptionButton(
                            icon = painterResource(R.drawable.image),
                            label = "Share as Image",
                            enabled = !isProcessing,
                            onClick = {
                                coroutineScope.launch {
                                    isProcessing = true
                                    val uri = try {
                                        withContext(Dispatchers.IO) {
                                            val image = createLyricsShareBitmap(
                                                context = context,
                                                background = selectedBackground,
                                                lyrics = shareableLyrics,
                                                title = mediaMetadata.title,
                                                artist = artistNames,
                                                textColor = textColor
                                            )
                                            val file = File(context.cacheDir, "images/share_lyrics.png")
                                            file.parentFile?.mkdirs()
                                            FileOutputStream(file).use { out ->
                                                image.compress(Bitmap.CompressFormat.PNG, 100, out)
                                            }
                                            FileProvider.getUriForFile(context, context.packageName + ".provider", file)
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                        null
                                    } finally {
                                        isProcessing = false
                                    }

                                    if (uri != null) {
                                        val intent = Intent(Intent.ACTION_SEND).apply {
                                            type = "image/png"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(intent, "Share lyrics image via"))
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Failed to generate image.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    }
                }
            }
            if (isProcessing) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false, onClick = {}),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private fun createLyricsShareBitmap(
    context: Context,
    background: Any,
    lyrics: String,
    title: String,
    artist: String,
    textColor: Color
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw Background
    when (background) {
        is Color -> canvas.drawColor(background.toArgb())
        is List<*> -> {
            val colors = (background as? List<Color>)?.map { it.toArgb() }?.toIntArray()
            if (colors != null) {
                val shader = android.graphics.LinearGradient(0f, 0f, 0f, height.toFloat(), colors, null, android.graphics.Shader.TileMode.CLAMP)
                val paint = Paint().apply { this.shader = shader }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            }
        }
    }

    // Draw App Logo
    val logoDrawable = ContextCompat.getDrawable(context, R.drawable.ic_launcher_background)
    if (logoDrawable != null) {
        val logoTargetWidth = 100
        val logoMargin = 64
        val aspectRatio = logoDrawable.intrinsicWidth.toFloat() / logoDrawable.intrinsicHeight.toFloat()
        val logoHeight = (logoTargetWidth / aspectRatio).toInt()
        val logoLeft = width - logoTargetWidth - logoMargin
        val logoTop = height - logoHeight - logoMargin
        val logoBitmap = Bitmap.createBitmap(logoTargetWidth, logoHeight, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBitmap)
        logoDrawable.setBounds(0, 0, logoCanvas.width, logoCanvas.height)
        logoDrawable.draw(logoCanvas)
        val paint = Paint().apply { alpha = (255 * 0.7).toInt() }
        canvas.drawBitmap(logoBitmap, logoLeft.toFloat(), logoTop.toFloat(), paint)
    }

    // Draw Lyrics
    val lyricsPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = 44f
        textAlign = Paint.Align.CENTER
    }
    val lyricsLayout = StaticLayout.Builder.obtain(lyrics, 0, lyrics.length, lyricsPaint, width - 120)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(16)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()
    val lyricsTop = 220f
    canvas.save()
    canvas.translate(width / 2f, lyricsTop)
    lyricsLayout.draw(canvas)
    canvas.restore()

    // Draw Title
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = 70f
        textAlign = Paint.Align.CENTER
    }
    val titleLayout = StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width - 160)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(2)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    // Draw Artist
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        this.alpha = (255 * 0.8f).toInt()
        textSize = 48f
        textAlign = Paint.Align.CENTER
    }
    val artistLayout = StaticLayout.Builder.obtain(artist, 0, artist.length, artistPaint, width - 160)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(1)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    // Draw title and artist below lyrics
    val titleTop = lyricsTop + lyricsLayout.height + 40f
    canvas.save()
    canvas.translate(width / 2f, titleTop)
    titleLayout.draw(canvas)
    canvas.restore()

    val artistTop = titleTop + titleLayout.height + 20f
    canvas.save()
    canvas.translate(width / 2f, artistTop)
    artistLayout.draw(canvas)
    canvas.restore()

    return bitmap
} 