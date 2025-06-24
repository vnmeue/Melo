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
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

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
    val artistNames = mediaMetadata.artists.joinToString { it.name }
    val coroutineScope = rememberCoroutineScope()
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var squareBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var paletteColors by remember { mutableStateOf<List<Color>>(emptyList()) }

    // Unified state for background selection. Can hold a List<Color> for gradients or a Color for solids.
    var selectedBackground by remember { mutableStateOf<Any>(gradientColors) }
    val isArtistShare = mediaMetadata.duration == 0 // Heuristic: artist shares have duration 0

    val backgroundBrush = remember(selectedBackground) {
        when (val selection = selectedBackground) {
            is Color -> SolidColor(selection)
            is List<*> -> Brush.linearGradient(selection.filterIsInstance<Color>())
            else -> Brush.linearGradient(gradientColors) // Fallback
        }
    }

    val isDarkBackground = remember(selectedBackground) {
        when (val bg = selectedBackground) {
            is Color -> bg.isDark()
            is List<*> -> true // Assume gradients are dark
            else -> true
        }
    }
    val textColor = if (isDarkBackground) Color.White else Color.Black

    LaunchedEffect(loadedBitmap) {
        if (loadedBitmap == null) return@LaunchedEffect
        // Center-crop to square
        val bmp = loadedBitmap!!
        val size = minOf(bmp.width, bmp.height)
        val x = (bmp.width - size) / 2
        val y = (bmp.height - size) / 2
        squareBitmap = Bitmap.createBitmap(bmp, x, y, size, size)
        Palette.from(squareBitmap!!).generate { palette ->
            val swatches = palette?.swatches?.sortedByDescending { it.population } ?: emptyList()
            val dark = swatches.filter { it.isDark() }.take(3).map { Color(it.rgb) }
            val light = swatches.filterNot { it.isDark() }.take(3).map { Color(it.rgb) }
            paletteColors = light + dark
            // Only auto-select a dark color for artist shares
            if (isArtistShare && dark.isNotEmpty() && (selectedBackground == gradientColors || selectedBackground == paletteColors)) {
                selectedBackground = dark.first()
            }
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
                            if (squareBitmap != null) {
                                Image(
                                    bitmap = squareBitmap!!.asImageBitmap(),
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
                                color = textColor,
                                maxLines = 2
                            )
                            Text(
                                text = artistNames,
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor.copy(alpha = 0.8f),
                                maxLines = 1
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
                        // Solid color swatches from palette
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
                            icon = painterResource(R.drawable.url_1423_svgrepo_com),
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
                            icon = painterResource(R.drawable.image),
                            label = "Share as Image",
                            enabled = !isProcessing,
                            onClick = {
                                if (squareBitmap == null) {
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
                                                albumArt = squareBitmap!!,
                                                title = mediaMetadata.title,
                                                artist = artistNames,
                                                textColor = textColor
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

        // Calculate dimensions while preserving aspect ratio
        val aspectRatio = logoDrawable.intrinsicWidth.toFloat() / logoDrawable.intrinsicHeight.toFloat()
        val logoHeight = (logoTargetWidth / aspectRatio).toInt()
        val logoLeft = width - logoTargetWidth - logoMargin
        val logoTop = height - logoHeight - logoMargin

        // Create a bitmap and canvas to draw the drawable onto
        val logoBitmap = Bitmap.createBitmap(logoTargetWidth, logoHeight, Bitmap.Config.ARGB_8888)
        val logoCanvas = Canvas(logoBitmap)
        logoDrawable.setBounds(0, 0, logoCanvas.width, logoCanvas.height)
        logoDrawable.draw(logoCanvas)

        val paint = Paint().apply {
            alpha = (255 * 0.7).toInt() // 70% opacity
        }
        canvas.drawBitmap(logoBitmap, logoLeft.toFloat(), logoTop.toFloat(), paint)
    }

    // Draw Album Art (600x600, centered horizontally, with rounded corners)
    val artSize = 800
    val cornerRadius = 60f
    val artLeft = (width - artSize) / 2f
    val artTop = height * 0.25f
    val artRect = RectF(artLeft, artTop, artLeft + artSize, artTop + artSize)
    val scaledArt = Bitmap.createScaledBitmap(albumArt, artSize, artSize, true)

    val artPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    canvas.drawRoundRect(artRect, cornerRadius, cornerRadius, artPaint)
    artPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
    canvas.drawBitmap(scaledArt, null, artRect, artPaint)

    // Draw Title
    val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        textSize = 80f
        textAlign = Paint.Align.CENTER
    }
    val titleLayout = android.text.StaticLayout.Builder.obtain(title, 0, title.length, titlePaint, width - 160)
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(2)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(width / 2f, artTop + artSize + 80f)
    titleLayout.draw(canvas)
    canvas.restore()

    // Draw Artist
    val artistPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor.toArgb()
        this.alpha = (255 * 0.8f).toInt()
        textSize = 50f
        textAlign = Paint.Align.CENTER
    }
    val artistLayout = android.text.StaticLayout.Builder.obtain(artist, 0, artist.length, artistPaint, width - 160)
        .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
        .setMaxLines(1)
        .setEllipsize(android.text.TextUtils.TruncateAt.END)
        .build()

    canvas.save()
    canvas.translate(width / 2f, artTop + artSize + 80f + titleLayout.height + 20f)
    artistLayout.draw(canvas)
    canvas.restore()

    return bitmap
}

fun Palette.Swatch.isDark(): Boolean {
    val hsl = this.hsl
    return hsl[2] < 0.5f
}

fun Color.isDark(): Boolean {
    // Luminance formula
    val red = red * 255
    val green = green * 255
    val blue = blue * 255
    val luminance = (0.2126 * red + 0.7152 * green + 0.0722 * blue) / 255
    return luminance < 0.5
}

@Composable
fun SharePlaylistDialog(
    playlistName: String,
    coverUrl: String?,
    songCount: Int,
    onDismiss: () -> Unit,
    shareLink: String? = null
) {
    val context = LocalContext.current
    var loadedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    LaunchedEffect(coverUrl) {
        if (coverUrl == null) return@LaunchedEffect
        val request = ImageRequest.Builder(context)
            .data(coverUrl)
            .allowHardware(false)
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
                    // Playlist Cover
                    if (loadedBitmap != null) {
                        Image(
                            bitmap = loadedBitmap!!.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Gray)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                    Text(
                        text = "$songCount songs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                        maxLines = 1
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(onClick = { onDismiss() }) {
                            Text("Close")
                        }
                        Spacer(Modifier.width(8.dp))
                        if (shareLink != null) {
                            TextButton(onClick = {
                                isProcessing = true
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareLink)
                                }
                                context.startActivity(Intent.createChooser(intent, "Share Playlist"))
                                isProcessing = false
                                onDismiss()
                            }) {
                                Text("Share")
                            }
                        }
                    }
                }
            }
        }
    }
} 