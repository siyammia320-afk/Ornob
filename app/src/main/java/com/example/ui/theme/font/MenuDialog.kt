package com.example.ui.theme.font

import android.content.Context
import android.net.Uri
import android.webkit.CookieManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.random.Random

object ProfilePicUploader {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private fun extractFormAction(html: String): String? {
        val formRegex = Regex("""<form\s+[^>]*action=["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
        val match = formRegex.find(html) ?: return null
        var url = match.groupValues[1]
        if (url.startsWith("/")) {
            url = "https://mbasic.facebook.com$url"
        } else if (!url.startsWith("http")) {
            url = "https://mbasic.facebook.com/$url"
        }
        return url.replace("&amp;", "&")
    }

    private fun extractInputVal(html: String, name: String): String? {
        val regex1 = Regex("""name=["']$name["']\s+value=["']([^"']*)["']""", RegexOption.IGNORE_CASE)
        val match1 = regex1.find(html)
        if (match1 != null) return match1.groupValues[1]

        val regex2 = Regex("""value=["']([^"']*)["']\s+name=["']$name["']""", RegexOption.IGNORE_CASE)
        val match2 = regex2.find(html)
        if (match2 != null) return match2.groupValues[1]

        return null
    }

    suspend fun uploadProfilePic(context: Context, cookies: String, imagePath: String): String = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) {
            return@withContext "Error: ছবি ফাইলটি খুঁজে পাওয়া যায়নি!"
        }

        try {
            // Step 1: Load mbasic profile picture page
            val request1 = Request.Builder()
                .url("https://mbasic.facebook.com/profile_picture/")
                .header("Cookie", cookies)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L) AppleWebKit/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .build()

            httpClient.newCall(request1).execute().use { response1 ->
                val html1 = response1.body?.string() ?: ""
                
                if (html1.contains("login_form") || html1.contains("cookie") && !html1.contains("fb_dtsg")) {
                    return@withContext "Error: কুকি এক্সপায়ার হয়েছে অথবা লগইন নেই!"
                }

                val action1 = extractFormAction(html1) 
                    ?: return@withContext "Error: ফেসবুক আপলোড ফর্ম পাওয়া যায়নি। ব্রাউজারে আইডি লগইন আছে কিনা চেক করুন।"
                
                val fbDtsg1 = extractInputVal(html1, "fb_dtsg") ?: ""
                val jazoest1 = extractInputVal(html1, "jazoest") ?: ""

                // Step 2: Multi-part upload photo
                val fileBody = file.asRequestBody("image/jpeg".toMediaType())
                val requestBody2 = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("fb_dtsg", fbDtsg1)
                    .addFormDataPart("jazoest", jazoest1)
                    .addFormDataPart("pic", file.name, fileBody)
                    .addFormDataPart("submit", "Preview")
                    .build()

                val request2 = Request.Builder()
                    .url(action1)
                    .post(requestBody2)
                    .header("Cookie", cookies)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L) AppleWebKit/537.36")
                    .build()

                httpClient.newCall(request2).execute().use { response2 ->
                    val html2 = response2.body?.string() ?: ""
                    
                    // Check if redirect has confirmation
                    val action2 = extractFormAction(html2)
                    val fbDtsg2 = extractInputVal(html2, "fb_dtsg") ?: fbDtsg1
                    val jazoest2 = extractInputVal(html2, "jazoest") ?: jazoest1

                    if (action2 != null && (action2.contains("confirm") || action2.contains("set") || action2.contains("profile_picture") || action2.contains("photo"))) {
                        // Step 3: Post confirm picture set
                        val confirmBody = FormBody.Builder()
                            .add("fb_dtsg", fbDtsg2)
                            .add("jazoest", jazoest2)
                            .build()

                        val request3 = Request.Builder()
                            .url(action2)
                            .post(confirmBody)
                            .header("Cookie", cookies)
                            .header("User-Agent", "Mozilla/5.0 (Linux; Android 12; itel S665L) AppleWebKit/537.36")
                            .build()

                        httpClient.newCall(request3).execute().use { response3 ->
                            return@withContext "Success"
                        }
                    } else {
                        // Check if direct success
                        if (html2.contains("profile_picture") || response2.isSuccessful) {
                            return@withContext "Success"
                        }
                        return@withContext "Error: ছবি আপলোড সম্পূর্ণ হয়নি।"
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Error: ${e.localizedMessage ?: "সংযোগ বিচ্যুতি"}"
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDialog(
    webViewUrl: String,
    lastCreatedCookies: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE) }

    // State for selected slots
    var slotPaths by remember { mutableStateOf<List<String?>>(listOf(null, null, null, null, null)) }
    var activeSlot by remember { mutableStateOf(-1) }
    var isUploading by remember { mutableStateOf(false) }

    // Loader helper
    fun loadSlots() {
        val paths = mutableListOf<String?>()
        for (i in 0..4) {
            val path = prefs.getString("saved_pp_slot_$i", null)
            if (path != null && File(path).exists()) {
                paths.add(path)
            } else {
                paths.add(null)
            }
        }
        slotPaths = paths
    }

    // Load slots on entering dialog
    LaunchedEffect(Unit) {
        loadSlots()
    }

    // File picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && activeSlot != -1) {
            // Save inside local storage
            val dir = File(context.filesDir, "profile_pics")
            if (!dir.exists()) dir.mkdirs()
            val destFile = File(dir, "pp_slot_$activeSlot.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                prefs.edit().putString("saved_pp_slot_$activeSlot", destFile.absolutePath).apply()
                Toast.makeText(context, "ছবি সেভ করা হয়েছে!", Toast.LENGTH_SHORT).show()
                loadSlots()
            } catch (e: Exception) {
                Toast.makeText(context, "ছবি সেভ করতে ব্যর্থ: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Dialog(
        onDismissRequest = { if (!isUploading) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF0F172A) // Sleek slate-900 background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "মেম্বারশিপ মেনু (PP Autopilot)",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        enabled = !isUploading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.LightGray
                        )
                    }
                }

                Divider(color = Color(0xFF334155), modifier = Modifier.padding(vertical = 12.dp))

                // Heading for Upload PP section
                Text(
                    text = "Upload PP (গ্যালারি থেকে ৪/৫ টি ছবি সেভ রাখুন):",
                    color = Color.LightGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Start
                )

                // 5 Slots Row Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                ) {
                    items(5) { index ->
                        val path = slotPaths.getOrNull(index)
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E293B))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(10.dp))
                                .clickable(enabled = !isUploading) {
                                    activeSlot = index
                                    pickerLauncher.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (path != null) {
                                // Try decode bitmap and display
                                val bitmap = try {
                                    android.graphics.BitmapFactory.decodeFile(path)
                                } catch (e: Exception) {
                                    null
                                }
                                if (bitmap != null) {
                                    Box(modifier = Modifier.fillMaxSize()) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "PP Slot $index",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Small remove button
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(16.dp)
                                                .background(Color.Red.copy(alpha = 0.8f))
                                                .clickable {
                                                    try {
                                                        File(path).delete()
                                                    } catch (e: Exception) {}
                                                    prefs.edit().remove("saved_pp_slot_$index").apply()
                                                    loadSlots()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "Delete",
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }
                                    }
                                } else {
                                    Icon(Icons.Default.Add, contentDescription = "Add Picture", tint = Color.LightGray)
                                }
                            } else {
                                Icon(Icons.Default.Add, contentDescription = "Add Picture", tint = Color.LightGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Auto PP Button section
                Button(
                    onClick = {
                        scope.launch {
                            // Extract valid image paths
                            val validPaths = slotPaths.filterNotNull().filter { File(it).exists() }
                            if (validPaths.isEmpty()) {
                                Toast.makeText(context, "দয়া করে প্রথমে Upload PP দিয়ে অন্তত একটি ছবি সেভ করুন!", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            // Try getting cookies from active WebView or lastCreatedCookies
                            var activeCookies = CookieManager.getInstance().getCookie(webViewUrl)
                            if (activeCookies.isNullOrBlank()) {
                                activeCookies = lastCreatedCookies
                            }

                            if (activeCookies.isNullOrBlank()) {
                                Toast.makeText(context, "ত্রুটি: লগইন করা ফেসবুক আইডি বা কুকিজ খুঁজে পাওয়া যায়নি!", Toast.LENGTH_LONG).show()
                                return@launch
                            }

                            isUploading = true
                            
                            // Select random picture
                            val randomPath = validPaths[Random.nextInt(validPaths.size)]
                            Toast.makeText(context, "র্যান্ডম ছবি আপলোড হচ্ছে, দয়া করে অপেক্ষা করুন...", Toast.LENGTH_SHORT).show()

                            val result = ProfilePicUploader.uploadProfilePic(context, activeCookies, randomPath)
                            isUploading = false

                            if (result == "Success") {
                                Toast.makeText(context, "অভিনন্দন! অটো প্রোফাইল পিকচার সেট সম্পন্ন হয়েছে।", Toast.LENGTH_LONG).show()
                                onDismiss()
                            } else {
                                Toast.makeText(context, result, Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    enabled = !isUploading
                ) {
                    if (isUploading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("প্রোফাইল পিকচার সেট হচ্ছে...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = "Auto PP", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("AUTO PP", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
