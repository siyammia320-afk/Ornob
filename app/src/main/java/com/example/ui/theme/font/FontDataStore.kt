package com.example.ui.theme.font

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

enum class UidStatus {
    LIVE, DED, DUPLICATE
}

data class CheckedUid(
    val uid: String,
    val status: UidStatus,
    val index: Int
)

object FontDataStore {
    
    // Non-redirecting client to make UID checking super fast
    private val checkerClient = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun verifyFbUid(uid: String): UidStatus = withContext(Dispatchers.IO) {
        val url = "https://graph.facebook.com/$uid/picture?type=normal"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/150.0.7871.46 Mobile Safari/537.36")
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("sec-ch-ua-platform", "\"Android\"")
            .header("sec-ch-ua", "\"Not;A=Brand\";v=\"8\", \"Chromium\";v=\"150\", \"Android WebView\";v=\"150\"")
            .header("sec-ch-ua-mobile", "?1")
            .header("origin", "https://dongvanfb.net")
            .header("x-requested-with", "mark.via.gp")
            .header("sec-fetch-site", "cross-site")
            .header("sec-fetch-mode", "cors")
            .header("sec-fetch-dest", "empty")
            .header("referer", "https://dongvanfb.net/")
            .header("accept-language", "en-US,en;q=0.9,es-US;q=0.8,es;q=0.7")
            .header("priority", "u=1, i")
            .build()

        try {
            checkerClient.newCall(request).execute().use { response ->
                val code = response.code
                if (code in 300..399) {
                    UidStatus.LIVE
                } else if (code == 200) {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString.contains("error") || bodyString.contains("GraphMethodException") || bodyString.contains("Unsupported get request")) {
                        UidStatus.DED
                    } else {
                        UidStatus.LIVE
                    }
                } else {
                    UidStatus.DED
                }
            }
        } catch (e: Exception) {
            UidStatus.DED
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveCkDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var inputText by remember { mutableStateOf("") }
    var isChecking by remember { mutableStateOf(false) }
    var currentProgress by remember { mutableStateOf(0f) }
    var progressText by remember { mutableStateOf("") }
    
    // Lists and counts
    var checkedList by remember { mutableStateOf<List<CheckedUid>>(emptyList()) }
    var liveCount by remember { mutableStateOf(0) }
    var dedCount by remember { mutableStateOf(0) }
    var duplicateCount by remember { mutableStateOf(0) }

    Dialog(
        onDismissRequest = { if (!isChecking) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A) // Dark background Slate 900
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "LIVE CK",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LIVE CK (UID Checker)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isChecking
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray
                        )
                    }
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                // Scrollable content area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Input label and quick buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "পাস্ট করুন UID লিস্ট (লাইন বাই লাইন):",
                            color = Color.LightGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Paste Button
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        if (text.isNotEmpty()) {
                                            inputText = text
                                            Toast.makeText(context, "ক্লিপবোর্ড থেকে পেস্ট করা হয়েছে", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                enabled = !isChecking,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF38BDF8))
                            ) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("পেস্ট করুন", fontSize = 12.sp)
                            }

                            // Clear Button
                            TextButton(
                                onClick = {
                                    inputText = ""
                                    checkedList = emptyList()
                                    liveCount = 0
                                    dedCount = 0
                                    duplicateCount = 0
                                },
                                enabled = !isChecking,
                                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFF87171))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("মুছুন", fontSize = 12.sp)
                            }
                        }
                    }

                    // Multi-line Input Field
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = "যেমন:\n100002139483321\n100023948329482\n100034958294823",
                                color = Color.Gray,
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isChecking
                    )

                    // Progress Loader (Animated)
                    if (isChecking) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { currentProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF10B981),
                                trackColor = Color(0xFF334155)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = progressText,
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Stat Row Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF064E3B)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Live", color = Color(0xFF34D399), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$liveCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Ded Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF7F1D1D)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Ded", color = Color(0xFFFCA5A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$dedCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Duplicate Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF78350F)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Duplicate", color = Color(0xFFFBBF24), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$duplicateCount", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Copy Action Panel (Only if results are available)
                    if (checkedList.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Copy Live Button
                            Button(
                                onClick = {
                                    val lives = checkedList.filter { it.status == UidStatus.LIVE }.joinToString("\n") { it.uid }
                                    if (lives.isNotEmpty()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Live UIDs", lives))
                                        Toast.makeText(context, "লাইভ UIDs কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "কোনো লাইভ UID পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Lives", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Live", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            // Copy Ded Button
                            Button(
                                onClick = {
                                    val deds = checkedList.filter { it.status == UidStatus.DED }.joinToString("\n") { it.uid }
                                    if (deds.isNotEmpty()) {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Dead UIDs", deds))
                                        Toast.makeText(context, "নষ্ট UIDs কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "কোনো নষ্ট UID পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy Deds", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy Ded", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Scrollable list showing the checked UIDs
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        if (checkedList.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "ফলাফল দেখতে উপরে UID বসিয়ে চেক বাটনে ক্লিক করুন",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(checkedList) { item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = Color(0xFF0F172A),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${item.index}.",
                                                color = Color.Gray,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.width(28.dp)
                                            )
                                            Text(
                                                text = item.uid,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }

                                        // Status Badge
                                        val badgeColor = when (item.status) {
                                            UidStatus.LIVE -> Color(0xFF10B981)
                                            UidStatus.DED -> Color(0xFFEF4444)
                                            UidStatus.DUPLICATE -> Color(0xFFFBBF24)
                                        }
                                        val badgeText = when (item.status) {
                                            UidStatus.LIVE -> "Live"
                                            UidStatus.DED -> "Ded"
                                            UidStatus.DUPLICATE -> "Dup"
                                        }

                                        Box(
                                            modifier = Modifier
                                                .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                                .border(1.dp, badgeColor, RoundedCornerShape(6.dp))
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom check button
                Button(
                    onClick = {
                        scope.launch {
                            isChecking = true
                            checkedList = emptyList()
                            liveCount = 0
                            dedCount = 0
                            duplicateCount = 0
                            
                            val rawLines = inputText.split("\n")
                            val cleanUids = rawLines.map { it.trim() }.filter { it.isNotEmpty() }
                            
                            if (cleanUids.isEmpty()) {
                                Toast.makeText(context, "অনুগ্রহ করে অন্তত একটি UID লিখুন!", Toast.LENGTH_SHORT).show()
                                isChecking = false
                                return@launch
                            }

                            val processedList = mutableListOf<CheckedUid>()
                            val seen = mutableSetOf<String>()
                            var liveVal = 0
                            var dedVal = 0
                            var dupVal = 0

                            for (i in cleanUids.indices) {
                                val currentUid = cleanUids[i]
                                currentProgress = (i + 1).toFloat() / cleanUids.size
                                progressText = "যাচাই করা হচ্ছে: ${i + 1} / ${cleanUids.size}"

                                if (seen.contains(currentUid)) {
                                    dupVal++
                                    duplicateCount = dupVal
                                    processedList.add(CheckedUid(currentUid, UidStatus.DUPLICATE, i + 1))
                                    checkedList = processedList.toList()
                                    continue
                                }
                                seen.add(currentUid)

                                // Call FB verification helper
                                val result = FontDataStore.verifyFbUid(currentUid)
                                if (result == UidStatus.LIVE) {
                                    liveVal++
                                    liveCount = liveVal
                                    processedList.add(CheckedUid(currentUid, UidStatus.LIVE, i + 1))
                                } else {
                                    dedVal++
                                    dedCount = dedVal
                                    processedList.add(CheckedUid(currentUid, UidStatus.DED, i + 1))
                                }
                                checkedList = processedList.toList()
                            }

                            Toast.makeText(context, "চেকিং সম্পন্ন হয়েছে! লাইভ: $liveVal, ডেড: $dedVal", Toast.LENGTH_LONG).show()
                            isChecking = false
                        }
                    },
                    enabled = !isChecking && inputText.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (isChecking) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("যাচাই হচ্ছে...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Start Checking")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CHECK UID LIST", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
