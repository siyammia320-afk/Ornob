package com.example

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isPlaying by MusicManager.isPlaying.collectAsState()
    val currentIndex by MusicManager.currentSongIndex.collectAsState()
    val currentTitle by MusicManager.currentSongTitle.collectAsState()
    val statusText by MusicManager.statusText.collectAsState()

    // Smooth Infinite Rotation Animation for the Vinyl Disc when music is playing
    val infiniteTransition = rememberInfiniteTransition(label = "music_transition")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "disc_rotation"
    )

    // Animated glow scale
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Dialog(
        onDismissRequest = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF070B14), // Deep cyber dark
            border = BorderStroke(
                1.5.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFFF43F5E), // Rose Pink
                        Color(0xFFA855F7), // Cyber Purple
                        Color(0xFF00E5FF)  // Electric Cyan
                    )
                )
            ),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF280B22),
                            border = BorderStroke(1.dp, Color(0xFFF43F5E)),
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = "Music",
                                    tint = Color(0xFFFB7185),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "মিউজিক প্লেয়ার",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "Background Music (ভিডিও থেকে অডিও)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 10.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDismiss()
                        },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF94A3B8)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Center Animated Vinyl Disc / Equalizer Card
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .scale(if (isPlaying) pulseScale else 1f),
                    contentAlignment = Alignment.Center
                ) {
                    // Outer Vinyl Ring
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(if (isPlaying) rotationAngle else 0f),
                        shape = CircleShape,
                        color = Color(0xFF13111C),
                        border = BorderStroke(
                            2.dp,
                            Brush.sweepGradient(
                                listOf(
                                    Color(0xFFF43F5E),
                                    Color(0xFFA855F7),
                                    Color(0xFF00E5FF),
                                    Color(0xFFF43F5E)
                                )
                            )
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // Inner Groove Rings
                            Surface(
                                modifier = Modifier.size(70.dp),
                                shape = CircleShape,
                                color = Color(0xFF1E1035),
                                border = BorderStroke(1.dp, Color(0x33A855F7))
                            ) {}
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = CircleShape,
                                color = Color(0xFF280B22),
                                border = BorderStroke(1.dp, Color(0xFFF43F5E))
                            ) {}
                        }
                    }

                    // Center Animated Icon
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.GraphicEq else Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = if (isPlaying) Color(0xFF00E5FF) else Color(0xFFF43F5E),
                        modifier = Modifier.size(28.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Song Title & Status Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F172A),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = currentTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) Color(0xFF10B981) else Color(0xFFEF4444))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    color = if (isPlaying) Color(0xFF34D399) else Color(0xFF94A3B8)
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Song Selection Chips (Total 3 songs)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MusicManager.songLabels.forEachIndexed { idx, label ->
                        val isSelected = currentIndex == idx
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    MusicManager.playSong(context, idx)
                                },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF280B22) else Color(0xFF1E293B),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFF43F5E) else Color(0xFF334155)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "গান ${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color(0xFFFB7185) else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Main Controls (Previous, Play/Pause, Next, Stop)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            MusicManager.previousSong(context)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFF475569), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipPrevious,
                            contentDescription = "Previous Song",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Big Play/Pause Button (Vibrant Rose-Purple Gradient)
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            MusicManager.togglePlayPause(context)
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFFF43F5E), Color(0xFFA855F7))),
                                CircleShape
                            ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(0.dp),
                        shape = CircleShape
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Next Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            MusicManager.nextSong(context)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF1E293B), CircleShape)
                            .border(1.dp, Color(0xFF475569), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SkipNext,
                            contentDescription = "Next Song",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Stop Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            MusicManager.stopSong()
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color(0xFF2D1515), CircleShape)
                            .border(1.dp, Color(0xFFEF4444), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Song",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Guide Box for MT Manager
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0B132B),
                    border = BorderStroke(1.dp, Color(0xFF1E293B))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = Color(0xFF00E5FF),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MT Manager ফোল্ডার গাইড:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E5FF)
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "APK ওপেন করে res/song/ ফোল্ডারে song1.mp4, song2.mp4, song3.mp4 রাখলে অ্যাপ নিজে থেকেই সেগুলো অডিও হিসেবে বাজাবে। অ্যাপ মিনিমাইজ করলেও গান ব্যাকগ্রাউন্ডে চলতে থাকবে।",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                lineHeight = 14.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
