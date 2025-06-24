package com.malopieds.innertune.ui.player

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.view.View
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import coil.imageLoader
import coil.request.ImageRequest
import com.malopieds.innertune.R
import com.malopieds.innertune.models.MediaMetadata
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.ComposeView
import java.io.File
import java.io.FileOutputStream
import android.app.Activity
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.graphics.SolidColor
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlinx.coroutines.withContext
import android.widget.Toast
import androidx.compose.ui.graphics.toArgb

@Composable
fun ShareSongDialog(
    mediaMetadata: MediaMetadata,
    albumArt: String?,
    onDismiss: () -> Unit,
    shareLink: String? = null,
    gradientColors: List<Color> = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val defaultColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.surface,
        Color(0xFF1DB954), // Spotify green
        Color(0xFF191414), // Spotify black
        Color(0xFFFFFFFF), // White
        Color(0xFF000000), // Black
    )
    val artistNames = mediaMetadata.artists.joinToString { it.name }
    val coroutineScope = rememberCoroutineScope()
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Unified state for background selection. Can hold a List<Color> for gradients or a Color for solids.
    var selectedBackground by remember { mutableStateOf<Any>(gradientColors) }

    val backgroundBrush = remember(selectedBackground) {
        when (val selection = selectedBackground) {
            is Color -> SolidColor(selection)
            is List<*> -> Brush.linearGradient(selection.filterIsInstance<Color>())
            else -> Brush.linearGradient(gradientColors) // Fallback
        }
    }

    LaunchedEffect(albumArt) {
        if (albumArt == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(albumArt)
            .allowHardware(false) // Required for software rendering.
            .target { drawable ->
                loadedBitmap = (drawable as? BitmapDrawable)?.bitmap
            }
            .build()
        context.imageLoader.enqueue(request)
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
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(backgroundBrush),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            if (loadedBitmap != null) {
                                Image(
                                    bitmap = loadedBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(120.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            } else {
                                // Placeholder
                                Box(modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Gray)
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = mediaMetadata.title,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                                maxLines = 2
                            )
                            Text(
                                text = artistNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                maxLines = 1
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Color Swatches
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Gradient swatch
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .padding(4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(gradientColors))
                                .clickable(enabled = !isProcessing) { selectedBackground = gradientColors }
                                .border(
                                    width = if (selectedBackground == gradientColors) 2.dp else 0.dp,
                                    color = if (selectedBackground == gradientColors) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                        )
                        // Solid color swatches
                        defaultColors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(color)
                                    .clickable(enabled = !isProcessing) { selectedBackground = color }
                                    .border(
                                        width = if (selectedBackground == color) 2.dp else 0.dp,
                                        color = if (selectedBackground == color) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    // Share Options
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        ShareOptionButton(
                            icon = painterResource(R.drawable.copy),
                            label = "Copy Link",
                            enabled = !isProcessing,
                            onClick = {
                                shareLink?.let {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Music Link", it)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Link copied!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        )
                        ShareOptionButton(
                            icon = painterResource(R.drawable.instagram),
                            label = "Instagram",
                            enabled = !isProcessing,
                            onClick = {
                                if (loadedBitmap == null) {
                                    Toast.makeText(context, "Album art is still loading.", Toast.LENGTH_SHORT).show()
                                    return@ShareOptionButton
                                }
                                coroutineScope.launch {
                                    isProcessing = true
                                    val uri = try {
                                        withContext(Dispatchers.IO) {
                                            val image = createShareBitmap(
                                                context = context,
                                                background = selectedBackground,
                                                albumArt = loadedBitmap!!,
                                                title = mediaMetadata.title,
                                                artist = artistNames
                                            )
                                            val file = File(context.cacheDir, "images/share_image.png")
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
                                        context.startActivity(Intent.createChooser(intent, "Share image via"))
                                        onDismiss()
                                    } else {
                                        if (isActive) {
                                            Toast.makeText(context, "Failed to generate image.", Toast.LENGTH_SHORT).show()
                                        }
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

@Composable
fun ShareOptionButton(
    icon: Painter,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(8.dp)
    ) {
        Icon(
            painter = icon,
            contentDescription = label,
            modifier = Modifier.size(32.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        )
    }
}

private fun createShareBitmap(
    context: Context,
    background: Any,
    albumArt: Bitmap,
    title: String,
    artist: String
): Bitmap {
    val width = 1080
    val height = 1920
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Draw Background
    val paint = Paint()
    when (background) {
        is Color -> {
            paint.color = background.toArgb()
        }
        is List<*> -> {
            val colors = background.filterIsInstance<Color>().map { it.toArgb() }
            if (colors.size > 1) {
                paint.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), colors.toIntArray(), null, Shader.TileMode.CLAMP)
            } else if (colors.isNotEmpty()) {
                paint.color = colors.first()
            }
        }
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

    // Draw Album Art (600x600, centered horizontally, with rounded corners)
    val artSize = 600f
    val cornerRadius = 64f
    val scaledArt = Bitmap.createScaledBitmap(albumArt, artSize.toInt(), artSize.toInt(), true)
    val artLeft = (width - artSize) / 2f
    val artTop = height * 0.25f
    val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
    val artPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawRoundRect(artRect, cornerRadius, cornerRadius, artPaint)
    artPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaledArt, null, artRect, artPaint)


    // Draw Title
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }
    val titleLayout = StaticLayout.Builder
        .obtain(title, 0, title.length, titlePaint, width - 128)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1.0f)
        .setIncludePad(true)
        .build()
    val titleTop = artTop + artSize + 80f
    canvas.save()
    canvas.translate(width / 2f, titleTop)
    titleLayout.draw(canvas)
    canvas.restore()

    // Draw Artist
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        alpha = 200 // ~80%
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }
    val artistTop = titleTop + titleLayout.height + 40f
    val artistLayout = StaticLayout.Builder
        .obtain(artist, 0, artist.length, artistPaint, width - 128)
        .setAlignment(Layout.Alignment.ALIGN_CENTER)
        .setLineSpacing(0f, 1.0f)
        .setIncludePad(true)
        .build()
    canvas.save()
    canvas.translate(width / 2f, artistTop)
    artistLayout.draw(canvas)
    canvas.restore()

    return bitmap
} 