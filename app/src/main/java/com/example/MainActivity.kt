package com.example

import com.example.ui.theme.font.LiveCkDialog
import com.example.ui.theme.font.MenuDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.tasks.await
import android.app.Activity
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import android.net.Uri
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.theme.MyApplicationTheme
import com.example.z
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

data class HistoryItem(
  val phone: String,
  val uid: String,
  val cookies: String,
  val password: String,
  val otp: String,
  val timestamp: Long
)

data class ButtonNames(
  val btn_bot_creator: String = "Bot Creator",
  val btn_range: String = "Range",
  val btn_copy_cookie: String = "Copy Cookie",
  val btn_copy_uid: String = "Copy UID",
  val btn_history: String = "History",
  val btn_cookie_login: String = "Cookie Login",
  val btn_gmail_copy: String = "Gmail Copy",
  val btn_fb_login: String = "FB Login",
  val btn_whoer_ip: String = "Whoer IP",
  val btn_proxy_config: String = "Proxy Config",
  val btn_admin_ws: String = "Admin WS",
  val btn_admin_tg: String = "Admin TG",
  val btn_tg_channel: String = "TG Channel",
  val btn_verify: String = "ভেরিফাই করুন",
  val btn_live_console: String = "Live Console",
  val btn_settings: String = "Settings"
)

private fun generateRandomGmail(): String {
  val chars = "abcdefghijklmnopqrstuvwxyz"
  val prefix = (1..8).map { chars.random() }.joinToString("")
  val number = (100..999).random()
  return "$prefix$number@gmail.com"
}

// OkHttp client setup
private val okHttpClient = OkHttpClient.Builder()
  .connectTimeout(15, TimeUnit.SECONDS)
  .readTimeout(15, TimeUnit.SECONDS)
  .build()

private fun getTodayOtpCount(context: Context): Int {
  val prefs = context.getSharedPreferences("otp_counter_prefs", Context.MODE_PRIVATE)
  return prefs.getInt("otp_count", 0)
}

private fun incrementTodayOtpCount(context: Context) {
  val prefs = context.getSharedPreferences("otp_counter_prefs", Context.MODE_PRIVATE)
  val currentCount = prefs.getInt("otp_count", 0)
  prefs.edit().putInt("otp_count", currentCount + 1).apply()
}

fun applyWebViewProxy(context: Context, enabled: Boolean, host: String, port: String) {
  if (WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
    if (enabled && host.isNotBlank() && port.isNotBlank()) {
      val proxyConfig = ProxyConfig.Builder()
        .addProxyRule("http://$host:$port", "http")
        .addProxyRule("http://$host:$port", "https")
        .addDirect()
        .build()
      try {
        val executor = java.util.concurrent.Executor { command -> 
          android.os.Handler(android.os.Looper.getMainLooper()).post(command) 
        }
        ProxyController.getInstance().setProxyOverride(proxyConfig, executor, Runnable {
          // Success callback
        })
      } catch (e: Exception) {
        e.printStackTrace()
      }
    } else {
      try {
        val executor = java.util.concurrent.Executor { command -> 
          android.os.Handler(android.os.Looper.getMainLooper()).post(command) 
        }
        ProxyController.getInstance().clearProxyOverride(executor, Runnable {
          // Cleared callback
        })
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }
}

// History storage via JSON in SharedPreferences
private const val HISTORY_KEY_JSON = "creation_history_json"

private fun saveHistoryList(prefs: SharedPreferences, list: List<HistoryItem>) {
  try {
    val jsonArray = JSONArray()
    for (item in list) {
      val obj = JSONObject()
      obj.put("phone", item.phone)
      obj.put("uid", item.uid)
      obj.put("cookies", item.cookies)
      obj.put("password", item.password)
      obj.put("otp", item.otp)
      obj.put("timestamp", item.timestamp)
      jsonArray.put(obj)
    }
    prefs.edit().putString(HISTORY_KEY_JSON, jsonArray.toString()).apply()
  } catch (e: Exception) {
    e.printStackTrace()
  }
}

// Save to SharedPreferences history
private fun saveAccountToHistory(context: Context, phone: String, uid: String, cookies: String, password: String, otp: String) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val items = getHistory(context).toMutableList()
  val timestamp = System.currentTimeMillis()
  items.add(0, HistoryItem(phone, uid, cookies, password, otp, timestamp))
  val trimmed = if (items.size > 2000) items.take(2000) else items
  saveHistoryList(prefs, trimmed)
}

// Get SharedPreferences history
private fun getHistory(context: Context): List<HistoryItem> {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val jsonStr = prefs.getString(HISTORY_KEY_JSON, null)
  if (!jsonStr.isNullOrEmpty()) {
    try {
      val jsonArray = JSONArray(jsonStr)
      val list = mutableListOf<HistoryItem>()
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          HistoryItem(
            phone = obj.optString("phone", ""),
            uid = obj.optString("uid", ""),
            cookies = obj.optString("cookies", ""),
            password = obj.optString("password", ""),
            otp = obj.optString("otp", ""),
            timestamp = obj.optLong("timestamp", 0L)
          )
        )
      }
      return list
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // Legacy fallback migration from old stringSet if exists
  val oldSet = prefs.getStringSet("creation_history", null)
  if (oldSet != null && oldSet.isNotEmpty()) {
    val list = oldSet.mapNotNull { entry ->
      val parts = entry.split("|")
      if (parts.size >= 5) {
        val phone = parts[0]
        val uid = parts[1]
        val rawCookies = try {
          String(Base64.decode(parts[2], Base64.NO_WRAP))
        } catch (e: Exception) {
          parts[2]
        }
        val password = parts[3]
        val otp = parts[4]
        val timestamp = parts.getOrNull(5)?.toLongOrNull() ?: 0L
        HistoryItem(phone, uid, rawCookies, password, otp, timestamp)
      } else null
    }.sortedByDescending { it.timestamp }
    
    saveHistoryList(prefs, list)
    return list
  }

  return emptyList()
}

// Update OTP in history
private fun updateOtpInHistory(context: Context, phone: String, otp: String) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val cleanTargetPhone = phone.replace("+", "").replace(" ", "").replace("-", "").trim()
  val items = getHistory(context).toMutableList()
  var updated = false
  for (i in 0 until items.size) {
    val item = items[i]
    val itemClean = item.phone.replace("+", "").replace(" ", "").replace("-", "").trim()
    if (itemClean == cleanTargetPhone || (itemClean.length >= 8 && cleanTargetPhone.length >= 8 && itemClean.takeLast(8) == cleanTargetPhone.takeLast(8))) {
      items[i] = item.copy(otp = otp)
      updated = true
      break
    }
  }
  if (updated) {
    saveHistoryList(prefs, items)
  }
}

// Clear all history
private fun clearHistory(context: Context) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  prefs.edit().remove(HISTORY_KEY_JSON).remove("creation_history").apply()
  val otpPrefs = context.getSharedPreferences("otp_counter_prefs", Context.MODE_PRIVATE)
  otpPrefs.edit().clear().apply()
}

// Delete individual item from history
private fun deleteItemFromHistory(context: Context, item: HistoryItem) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val items = getHistory(context).filterNot { it.phone == item.phone && it.timestamp == item.timestamp }
  saveHistoryList(prefs, items)
}

private fun hasValidOtpInHistory(context: Context, phone: String): Boolean {
  val cleanTargetPhone = phone.replace("+", "").replace(" ", "").replace("-", "").trim()
  if (cleanTargetPhone.isEmpty()) return false
  val items = getHistory(context)
  for (item in items) {
    val itemClean = item.phone.replace("+", "").replace(" ", "").replace("-", "").trim()
    if (itemClean == cleanTargetPhone || (itemClean.length >= 8 && cleanTargetPhone.length >= 8 && itemClean.takeLast(8) == cleanTargetPhone.takeLast(8))) {
      if (isValidOtpCode(item.otp)) {
        return true
      }
    }
  }
  return false
}

// Fetch Dynamic Button Names
fun fetchDynamicButtonNames(onSuccess: (ButtonNames) -> Unit) {
  val dbUrl = "https://fb-lite-pro-vr-default-rtdb.firebaseio.com/app_config/buttons.json"
  val request = okhttp3.Request.Builder().url(dbUrl).build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      // do nothing on failure, keep existing
    }
    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        val bodyStr = res.body?.string() ?: return
        if (bodyStr == "null") return
        try {
          val json = JSONObject(bodyStr)
          val names = ButtonNames(
            btn_bot_creator = json.optString("btn_bot_creator", "Bot Creator"),
            btn_range = json.optString("btn_range", "Range"),
            btn_copy_cookie = json.optString("btn_copy_cookie", "Copy Cookie"),
            btn_copy_uid = json.optString("btn_copy_uid", "Copy UID"),
            btn_history = json.optString("btn_history", "History"),
            btn_cookie_login = json.optString("btn_cookie_login", "Cookie Login"),
            btn_gmail_copy = json.optString("btn_gmail_copy", "Gmail Copy"),
            btn_fb_login = json.optString("btn_fb_login", "FB Login"),
            btn_whoer_ip = json.optString("btn_whoer_ip", "Whoer IP"),
            btn_proxy_config = json.optString("btn_proxy_config", "Proxy Config"),
            btn_admin_ws = json.optString("btn_admin_ws", "Admin WS"),
            btn_admin_tg = json.optString("btn_admin_tg", "Admin TG"),
            btn_tg_channel = json.optString("btn_tg_channel", "TG Channel"),
            btn_verify = json.optString("btn_verify", "ভেরিফাই করুন"),
            btn_live_console = json.optString("btn_live_console", "Live Console"),
            btn_settings = json.optString("btn_settings", "Settings")
          )
          onSuccess(names)
        } catch (e: Exception) {
          // ignore
        }
      }
    }
  })
}

// Country lookup for live ranges
// Comprehensive Country lookup for live ranges
fun getCountryForRange(range: String): String {
  val clean = range.replace("XXX", "").replace("X", "").replace("+", "").replace(" ", "").trim()
  if (clean.isEmpty()) return ""

  // 4-digit prefixes (Caribbean & NANP special codes)
  if (clean.length >= 4) {
    val four = clean.substring(0, 4)
    val c = when (four) {
      "1242" -> "Bahamas 👉BS"
      "1246" -> "Barbados 👉BB"
      "1264" -> "Anguilla 👉AI"
      "1268" -> "Antigua 👉AG"
      "1284" -> "British Virgin Is 👉VG"
      "1340" -> "US Virgin Is 👉VI"
      "1345" -> "Cayman Islands 👉KY"
      "1441" -> "Bermuda 👉BM"
      "1473" -> "Grenada 👉GD"
      "1649" -> "Turks & Caicos 👉TC"
      "1664" -> "Montserrat 👉MS"
      "1670" -> "Northern Mariana Is 👉MP"
      "1671" -> "Guam 👉GU"
      "1684" -> "American Samoa 👉AS"
      "1721" -> "Sint Maarten 👉SX"
      "1758" -> "Saint Lucia 👉LC"
      "1767" -> "Dominica 👉DM"
      "1784" -> "St Vincent 👉VC"
      "1787", "1939" -> "Puerto Rico 👉PR"
      "1809", "1829", "1849" -> "Dominican Rep 👉DO"
      "1868" -> "Trinidad & Tobago 👉TT"
      "1869" -> "Saint Kitts & Nevis 👉KN"
      "1876" -> "Jamaica 👉JM"
      else -> null
    }
    if (c != null) return c
  }

  // 3-digit prefixes
  if (clean.length >= 3) {
    val three = clean.substring(0, 3)
    val c = when (three) {
      // Africa
      "211" -> "South Sudan 👉SS"
      "212" -> "Morocco 👉MA"
      "213" -> "Algeria 👉DZ"
      "216" -> "Tunisia 👉TN"
      "218" -> "Libya 👉LY"
      "220" -> "Gambia 👉GM"
      "221" -> "Senegal 👉SN"
      "222" -> "Mauritania 👉MR"
      "223" -> "Mali 👉ML"
      "224" -> "Guinea 👉GN"
      "225" -> "Ivory Coast 👉CI"
      "226" -> "Burkina Faso 👉BF"
      "227" -> "Niger 👉NE"
      "228" -> "Togo 👉TG"
      "229" -> "Benin 👉BJ"
      "230" -> "Mauritius 👉MU"
      "231" -> "Liberia 👉LR"
      "232" -> "Sierra Leone 👉SL"
      "233" -> "Ghana 👉GH"
      "234" -> "Nigeria 👉NG"
      "235" -> "Chad 👉TD"
      "236" -> "Central African Rep 👉CF"
      "237" -> "Cameroon 👉CM"
      "238" -> "Cape Verde 👉CV"
      "239" -> "Sao Tome 👉ST"
      "240" -> "Equatorial Guinea 👉GQ"
      "241" -> "Gabon 👉GA"
      "242" -> "Congo 👉CG"
      "243" -> "DR Congo 👉CD"
      "244" -> "Angola 👉AO"
      "245" -> "Guinea-Bissau 👉GW"
      "246" -> "Diego Garcia 👉IO"
      "247" -> "Ascension Island 👉AC"
      "248" -> "Seychelles 👉SC"
      "249" -> "Sudan 👉SD"
      "250" -> "Rwanda 👉RW"
      "251" -> "Ethiopia 👉ET"
      "252" -> "Somalia 👉SO"
      "253" -> "Djibouti 👉DJ"
      "254" -> "Kenya 👉KE"
      "255" -> "Tanzania 👉TZ"
      "256" -> "Uganda 👉UG"
      "257" -> "Burundi 👉BI"
      "258" -> "Mozambique 👉MZ"
      "260" -> "Zambia 👉ZM"
      "261" -> "Madagascar 👉MG"
      "262" -> "Reunion 👉RE"
      "263" -> "Zimbabwe 👉ZW"
      "264" -> "Namibia 👉NA"
      "265" -> "Malawi 👉MW"
      "266" -> "Lesotho 👉LS"
      "267" -> "Botswana 👉BW"
      "268" -> "Eswatini 👉SZ"
      "269" -> "Comoros 👉KM"
      "290" -> "Saint Helena 👉SH"
      "291" -> "Eritrea 👉ER"
      "297" -> "Aruba 👉AW"
      "298" -> "Faroe Islands 👉FO"
      "299" -> "Greenland 👉GL"

      // Europe
      "350" -> "Gibraltar 👉GI"
      "351" -> "Portugal 👉PT"
      "352" -> "Luxembourg 👉LU"
      "353" -> "Ireland 👉IE"
      "354" -> "Iceland 👉IS"
      "355" -> "Albania 👉AL"
      "356" -> "Malta 👉MT"
      "357" -> "Cyprus 👉CY"
      "358" -> "Finland 👉FI"
      "359" -> "Bulgaria 👉BG"
      "370" -> "Lithuania 👉LT"
      "371" -> "Latvia 👉LV"
      "372" -> "Estonia 👉EE"
      "373" -> "Moldova 👉MD"
      "374" -> "Armenia 👉AM"
      "375" -> "Belarus 👉BY"
      "376" -> "Andorra 👉AD"
      "377" -> "Monaco 👉MC"
      "378" -> "San Marino 👉SM"
      "379" -> "Vatican City 👉VA"
      "380" -> "Ukraine 👉UA"
      "381" -> "Serbia 👉RS"
      "382" -> "Montenegro 👉ME"
      "383" -> "Kosovo 👉XK"
      "385" -> "Croatia 👉HR"
      "386" -> "Slovenia 👉SI"
      "387" -> "Bosnia 👉BA"
      "389" -> "North Macedonia 👉MK"
      "420" -> "Czech Republic 👉CZ"
      "421" -> "Slovakia 👉SK"
      "423" -> "Liechtenstein 👉LI"

      // Americas
      "500" -> "Falkland Islands 👉FK"
      "501" -> "Belize 👉BZ"
      "502" -> "Guatemala 👉GT"
      "503" -> "El Salvador 👉SV"
      "504" -> "Honduras 👉HN"
      "505" -> "Nicaragua 👉NI"
      "506" -> "Costa Rica 👉CR"
      "507" -> "Panama 👉PA"
      "508" -> "St Pierre & Miquelon 👉PM"
      "509" -> "Haiti 👉HT"
      "590" -> "Guadeloupe 👉GP"
      "591" -> "Bolivia 👉BO"
      "592" -> "Guyana 👉GY"
      "593" -> "Ecuador 👉EC"
      "594" -> "French Guiana 👉GF"
      "595" -> "Paraguay 👉PY"
      "596" -> "Martinique 👉MQ"
      "597" -> "Suriname 👉SR"
      "598" -> "Uruguay 👉UY"
      "599" -> "Curacao 👉CW"

      // Asia & Middle East
      "850" -> "North Korea 👉KP"
      "852" -> "Hong Kong 👉HK"
      "853" -> "Macau 👉MO"
      "855" -> "Cambodia 👉KH"
      "856" -> "Laos 👉LA"
      "880" -> "Bangladesh 👉BD"
      "886" -> "Taiwan 👉TW"
      "960" -> "Maldives 👉MV"
      "961" -> "Lebanon 👉LB"
      "962" -> "Jordan 👉JO"
      "963" -> "Syria 👉SY"
      "964" -> "Iraq 👉IQ"
      "965" -> "Kuwait 👉KW"
      "966" -> "Saudi Arabia 👉SA"
      "967" -> "Yemen 👉YE"
      "968" -> "Oman 👉OM"
      "970" -> "Palestine 👉PS"
      "971" -> "UAE 👉AE"
      "972" -> "Israel 👉IL"
      "973" -> "Bahrain 👉BH"
      "974" -> "Qatar 👉QA"
      "975" -> "Bhutan 👉BT"
      "976" -> "Mongolia 👉MN"
      "977" -> "Nepal 👉NP"
      "992" -> "Tajikistan 👉TJ"
      "993" -> "Turkmenistan 👉TM"
      "994" -> "Azerbaijan 👉AZ"
      "995" -> "Georgia 👉GE"
      "996" -> "Kyrgyzstan 👉KG"
      "998" -> "Uzbekistan 👉UZ"

      // Oceania
      "670" -> "East Timor 👉TL"
      "672" -> "Norfolk Island 👉NF"
      "673" -> "Brunei 👉BN"
      "674" -> "Nauru 👉NR"
      "675" -> "Papua New Guinea 👉PG"
      "676" -> "Tonga 👉TO"
      "677" -> "Solomon Islands 👉SB"
      "678" -> "Vanuatu 👉VU"
      "679" -> "Fiji 👉FJ"
      "680" -> "Palau 👉PW"
      "681" -> "Wallis & Futuna 👉WF"
      "682" -> "Cook Islands 👉CK"
      "683" -> "Niue 👉NU"
      "685" -> "Samoa 👉WS"
      "686" -> "Kiribati 👉KI"
      "687" -> "New Caledonia 👉NC"
      "688" -> "Tuvalu 👉TV"
      "689" -> "French Polynesia 👉PF"
      "690" -> "Tokelau 👉TK"
      "691" -> "Micronesia 👉FM"
      "692" -> "Marshall Islands 👉MH"
      else -> null
    }
    if (c != null) return c
  }

  // 2-digit prefixes
  if (clean.length >= 2) {
    val two = clean.substring(0, 2)
    val c = when (two) {
      "20" -> "Egypt 👉EG"
      "27" -> "South Africa 👉ZA"
      "30" -> "Greece 👉GR"
      "31" -> "Netherlands 👉NL"
      "32" -> "Belgium 👉BE"
      "33" -> "France 👉FR"
      "34" -> "Spain 👉ES"
      "36" -> "Hungary 👉HU"
      "39" -> "Italy 👉IT"
      "40" -> "Romania 👉RO"
      "41" -> "Switzerland 👉CH"
      "43" -> "Austria 👉AT"
      "44" -> "United Kingdom 👉GB"
      "45" -> "Denmark 👉DK"
      "46" -> "Sweden 👉SE"
      "47" -> "Norway 👉NO"
      "48" -> "Poland 👉PL"
      "49" -> "Germany 👉DE"
      "51" -> "Peru 👉PE"
      "52" -> "Mexico 👉MX"
      "53" -> "Cuba 👉CU"
      "54" -> "Argentina 👉AR"
      "55" -> "Brazil 👉BR"
      "56" -> "Chile 👉CL"
      "57" -> "Colombia 👉CO"
      "58" -> "Venezuela 👉VE"
      "60" -> "Malaysia 👉MY"
      "61" -> "Australia 👉AU"
      "62" -> "Indonesia 👉ID"
      "63" -> "Philippines 👉PH"
      "64" -> "New Zealand 👉NZ"
      "65" -> "Singapore 👉SG"
      "66" -> "Thailand 👉TH"
      "81" -> "Japan 👉JP"
      "82" -> "South Korea 👉KR"
      "84" -> "Vietnam 👉VN"
      "86" -> "China 👉CN"
      "90" -> "Turkey 👉TR"
      "91" -> "India 👉IN"
      "92" -> "Pakistan 👉PK"
      "93" -> "Afghanistan 👉AF"
      "94" -> "Sri Lanka 👉LK"
      "95" -> "Myanmar 👉MM"
      "98" -> "Iran 👉IR"
      else -> null
    }
    if (c != null) return c
  }

  // 1-digit prefixes
  if (clean.length >= 1) {
    val one = clean.substring(0, 1)
    if (one == "1") return "USA/Canada 👉US"
    if (one == "7") return "Russia 👉RU"
  }

  return ""
}

// Fetch live Facebook ranges
fun fetchFacebookRanges(onSuccess: (List<String>) -> Unit, onFailure: (String) -> Unit) {
  val request = Request.Builder()
    .url("${z.API_BASE_URL}/liveaccess")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Unknown network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure("HTTP error ${res.code}")
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            val services = dataObj?.optJSONArray("services")
            if (services != null) {
              for (i in 0 until services.length()) {
                val sObj = services.getJSONObject(i)
                val sid = sObj.optString("sid", "")
                if (sid.equals("Facebook", ignoreCase = true)) {
                  val rangesArray = sObj.optJSONArray("ranges")
                  val ranges = mutableListOf<String>()
                  if (rangesArray != null) {
                    for (j in 0 until rangesArray.length()) {
                      ranges.add(rangesArray.getString(j))
                    }
                  }
                  onSuccess(ranges)
                  return
                }
              }
            }
          }
          onFailure("Facebook service not found in API")
        } catch (e: Exception) {
          onFailure("Parsing error: ${e.message}")
        }
      }
    }
  })
}

// Request phone number for selected range
fun fetchNumber(rangeCode: String, onSuccess: (String) -> Unit, onFailure: (String) -> Unit) {
  val rid = rangeCode.replace("XXX", "").replace("X", "").replace(" ", "").trim()
  val cleanRid = if (rid.isEmpty()) "8801" else rid

  val payload = JSONObject().put("rid", cleanRid).toString()
  val requestBody = payload.toRequestBody("application/json".toMediaTypeOrNull())

  val request = Request.Builder()
    .url("${z.API_BASE_URL}/getnum")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .post(requestBody)
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Unknown network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure("HTTP error ${res.code}")
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            if (dataObj != null) {
              val fullNumber = dataObj.optString("full_number", "")
                .ifEmpty { dataObj.optString("no_plus_number", "") }
              val cleanNumber = fullNumber.replace("+", "").replace(" ", "").trim()
              if (cleanNumber.isNotEmpty()) {
                onSuccess(cleanNumber)
                return
              }
            }
          }
          onFailure("রানিং কোনো নাম্বার পাওয়া যায়নি!")
        } catch (e: Exception) {
          onFailure("Parsing error: ${e.message}")
        }
      }
    }
  })
}

// Create actual Facebook account via submit reg
fun createFacebookAccount(
  context: Context,
  phone: String,
  passwordInput: String,
  country: String,
  onSuccess: (uid: String, name: String, cookies: String) -> Unit,
  onFailure: (String) -> Unit
) {
  val prefs = context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
  val proxyHost = prefs.getString("proxy_host", "") ?: ""
  val proxyPort = prefs.getString("proxy_port", "") ?: ""
  val proxyUser = prefs.getString("proxy_user", "") ?: ""
  val proxyPass = prefs.getString("proxy_pass", "") ?: ""

  val clientBuilder = okHttpClient.newBuilder()
  
  // Automatically use proxy if host/port are provided
  if (proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
    try {
      val proxy = java.net.Proxy(
        java.net.Proxy.Type.HTTP,
        java.net.InetSocketAddress(proxyHost, proxyPort.toInt())
      )
      clientBuilder.proxy(proxy)
      
      // Direct Authentication for Proxy if credentials exist
      if (proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
          val credential = okhttp3.Credentials.basic(proxyUser, proxyPass)
          clientBuilder.proxyAuthenticator { _, response ->
              response.request.newBuilder()
                  .header("Proxy-Authorization", credential)
                  .build()
          }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  val activeClient = clientBuilder.build()

  val firstNames = when (country) {
    "Bangladesh" -> listOf("রাহিম", "করিম", "সাকিব", "শফিকুল", "আব্দুর", "মাসুদ", "আরিফ", "নাজমুল", "তারেক", "ফারুক", "হাসান", "কামরুল", "মেহেদী", "রনি", "সুমন", "রুবেল", "জসিম", "সোহেল", "ইমরান", "আকবর", "রাশেদ", "রিয়াজ", "মিজান", "আনোয়ার", "মাহমুদ", "হাবিব", "শাহিন", "তৌহিদ", "ফয়সাল", "সাগর", "শরীফ", "জাকির", "আলমগীর", "নূর", "তাজুল", "মহিউদ্দিন", "সেলিম", "রুহুল", "জালাল", "বাপ্পি", "শামিম", "সাইফুল", "তন্ময়", "পলাশ", "আশরাফ", "মুকুল", "লিটন", "শাহাদাত", "নাসির", "সাজ্জাদ")
    "United States" -> listOf("James", "John", "Robert", "Michael", "William", "David", "Richard", "Joseph", "Thomas", "Charles", "Christopher", "Daniel", "Matthew", "Anthony", "Donald", "Mark", "Paul", "Steven", "Andrew", "Kenneth", "Joshua", "Kevin", "Brian", "George", "Edward", "Ronald", "Timothy", "Jason", "Jeffrey", "Ryan", "Jacob", "Gary", "Nicholas", "Eric", "Jonathan", "Stephen", "Larry", "Justin", "Scott", "Brandon", "Benjamin", "Samuel", "Frank", "Gregory", "Raymond", "Alexander", "Patrick", "Jack", "Dennis", "Jerry")
    "China" -> listOf("雅婷", "雅君", "雅涵", "雅萱", "雅妤", "雅甄", "雅玲", "雅芬", "雅儀", "雅柔", "雅蓉", "雅瑄", "雅穎", "雅琪", "雅晴", "雅雯", "雅潔", "雅瑜", "雅蓁", "雅芸", "雅珊", "雅慈", "雅茹", "雅嫻", "雅薇", "雅彤", "雅恩", "雅榕", "雅媛", "雅寧", "怡婷", "怡君", "怡涵", "怡萱", "怡妤", "怡甄", "怡玲", "怡芬", "怡儀", "怡柔", "怡蓉", "怡瑄", "怡穎", "怡琪", "怡晴", "怡雯", "怡潔", "怡瑜", "怡蓁", "怡芸", "怡珊", "怡慈", "怡茹", "怡嫻", "怡薇", "怡彤", "怡恩", "怡榕", "怡媛", "怡寧", "欣婷", "欣君", "欣涵", "欣萱", "欣妤", "欣甄", "欣玲", "欣芬", "欣儀", "欣柔", "欣蓉", "欣瑄", "欣穎", "欣琪", "欣晴", "欣雯", "欣潔", "欣瑜")
    "Madagascar" -> listOf("Rakoto", "Rasoa", "Rabe", "Jean", "Marie", "Nirina", "Fanja", "Mialy", "Tiana", "Tahina", "Lova", "Andry", "Hery", "Rivo", "Tolotra", "Vola", "Bodo", "Mamy", "Njaka", "Mihaja", "Lanto", "Naivo", "Solo", "Rija", "Mahefa", "Tojo", "Kanto", "Aina", "Sitraka", "Zo", "Faly", "Haja", "Riana", "Tafita", "Mino", "Dina", "Ndriana", "Malala", "Hasina", "Ony", "Noro", "Tovo", "Zoly", "Ihary", "Lina", "Bako", "Nary", "Fidy", "Rado")
    else -> listOf("John", "Emma", "Michael", "Sophia", "David")
  }
  
  val lastNames = when (country) {
    "Bangladesh" -> listOf("তালুকদার", "রহমান", "খান", "চৌধুরী", "আহমেদ", "শেখ", "ইসলাম", "মোল্লা", "মন্ডল", "হাওলাদার", "সরকার", "শিকদার", "ভূঁইয়া", "মির্জা", "খন্দকার", "কাজী", "মজুমদার", "খাঁন", "সৈয়দ", "বিশ্বাস", "মিয়া", "মিয়াঁ", "বেপারী", "খলিফা", "মাহী", "হোসেন", "আকন্দ", "ফকির", "হক", "সানা", "মৃধা", "সৈয়াল", "ফরায়েজী", "মালী", "মুন্সী", "তপাদার", "পাটোয়ারী", "তরফদার", "মহাজন", "দাস", "সাহা", "কৰ্মকার", "দেবনাথ", "ভট্টাচার্য", "শীল", "পাল", "বনিক", "রায়", "মিত্র", "ঘোষ")
    "United States" -> listOf("Smith", "Johnson", "Williams", "Brown", "Jones", "Garcia", "Miller", "Davis", "Rodriguez", "Martinez", "Hernandez", "Lopez", "Gonzalez", "Wilson", "Anderson", "Thomas", "Taylor", "Moore", "Jackson", "Martin", "Lee", "Perez", "Thompson", "White", "Harris", "Sanchez", "Clark", "Ramirez", "Lewis", "Robinson", "Walker", "Young", "Allen", "King", "Wright", "Scott", "Torres", "Nguyen", "Hill", "Flores", "Green", "Adams", "Nelson", "Baker", "Hall", "Rivera", "Campbell", "Mitchell", "Carter", "Roberts")
    "China" -> listOf("池", "曾", "張", "熊", "謝", "馮", "彭", "鄧", "馬", "葉", "武", "向", "孫", "柯", "黎", "康", "陳", "林", "黃", "李", "王", "吳", "劉", "蔡", "楊", "許", "鄭", "郭", "洪", "邱", "廖", "賴", "徐", "周", "蘇", "莊", "呂", "江", "何", "蕭", "羅", "高", "潘", "簡", "朱", "鍾", "游", "詹", "胡", "施", "沉", "余", "盧", "趙", "梁", "顏", "翁", "魏", "戴", "方", "宋", "范", "杜", "傅", "侯", "曹", "薛", "丁", "卓", "董", "唐", "藍", "蔣", "石", "紀", "姚", "古", "連")
    "Madagascar" -> listOf("Andrianampy", "Ramanana", "Ravalomanana", "Andriamanjato", "Rakotomanga", "Randria", "Razafy", "Rasamimanana", "Ratsiraka", "Razafindrakoto", "Andriamandimby", "Rasoamanana", "Rabearivelo", "Razafimahatratra", "Rajoelina", "Rakotondrazaka", "Randrianasolo", "Ravelomanantsoa", "Rakotomalala", "Rasolofondraibe", "Rakotonirina", "Rahajanirina", "Rakotoniaina", "Rabemananjara", "Rakotondrabe", "Rakotoarisoa", "Andrianirina", "Rasoanaivo", "Razafindrabe", "Andriantsilavo", "Rakotovao", "Razafindratandra", "Randriamampionona", "Rakotondramanana", "Ramaroson", "Rakotoarivelo", "Randriamanantena", "Rasolomanana", "Razafindrakondro", "Andriamasinoro", "Rakotomandimby", "Razafimanantsoa", "Rakotoarimanana", "Rakotondrasoa", "Randriamanjaka", "Rasoarimalala", "Rakotozandriny", "Razafindramamba", "Rakotondravony", "Andriantsalama")
    else -> listOf("Smith", "Johnson", "Williams", "Jones", "Brown")
  }
  
  val fname = firstNames.random()
  val lname = lastNames.random()
  val day = (1..28).random().toString()
  val month = (1..12).random().toString()
  val year = (1985..2003).random().toString()
  
  val uuid1 = UUID.randomUUID().toString()
  val uuid2 = UUID.randomUUID().toString()
  
  val cookieDatr = "3XA5at-YBOFaGHi2xPrg-wka"
  val androidUa = "Mozilla/5.0 (Linux; Android 12; itel S665L Build/SP1A.210812.016) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.7827.91 Mobile Safari/537.36"

  val formBody = FormBody.Builder()
    .add("ccp", "2")
    .add("reg_instance", cookieDatr)
    .add("submission_request", "true")
    .add("helper", "")
    .add("reg_impression_id", uuid1)
    .add("ns", "1")
    .add("zero_header_af_client", "")
    .add("app_id", "103")
    .add("logger_id", uuid2)
    .add("field_names[0]", "firstname")
    .add("firstname", fname)
    .add("lastname", lname)
    .add("field_names[1]", "birthday_wrapper")
    .add("birthday_day", day)
    .add("birthday_month", month)
    .add("birthday_year", year)
    .add("age_step_input", "")
    .add("did_use_age", "false")
    .add("field_names[2]", "reg_email__")
    .add("reg_email__", phone)
    .add("field_names[3]", "sex")
    .add("sex", "2")
    .add("preferred_pronoun", "")
    .add("custom_gender", "")
    .add("reg_passwd__", passwordInput)
    .add("name_suggest_elig", "false")
    .add("was_shown_name_suggestions", "false")
    .add("did_use_suggested_name", "false")
    .add("use_custom_gender", "false")
    .add("guid", "")
    .add("pre_form_step", "")
    .add("submit", "Sign up")
    .add("fb_dtsg", "NAfx5UxG44eai86HC1iwiixBs1mUDFhn3ccN1fj3-SJJc64TeUsEAEg:0:0")
    .add("jazoest", "24748")
    .add("lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
    .add("__dyn", "1Z3pawlEnwm8_Bg9ppoW5UdE4a2i5U4e0C86u7E39x60zU3ex608ewk9E4W0pKq0FE6S0x81vohw73wGwcq1GwqU2YwbK0oi0zE1jU1soG0hi0Lo6-0Co1kU1UU3jwea")
    .add("__csr", "")
    .add("__hsdp", "")
    .add("__hblp", "")
    .add("__sjsp", "")
    .add("__req", "g")
    .add("__fmt", "1")
    .add("__a", "AYzJ_41FhHOHmeaJtz_y-NZ41BrpCkk8MZbenM7ATpRLY9c4d3QLNQW9sph6SN5jNJBH5tH1yvE_P-EybRqM6tZ_nqLEaV4b3ZU")
    .add("__user", "0")
    .build()

  val url = "${a.b1()}reg/submit/?privacy_mutation_token=eyJ0eXBlIjowLCJjcmVhdGlvbl90aW1lIjoxNzgyMTQ5MzY4LCJjYWxsc2l0ZV9pZCI6OTA3OTI0NDAyOTQ4MDU4fQ%3D%3D&app_id=103&multi_step_form=1&skip_suma=0&shouldForceMTouch=1"

  val request = Request.Builder()
    .url(url)
    .header("User-Agent", androidUa)
    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
    .header("Accept-Language", "fr-FR,fr;q=0.9,en;q=0.8")
    .header("Accept-Encoding", "gzip, deflate, br, zstd")
    .header("Connection", "keep-alive")
    .header("Upgrade-Insecure-Requests", "1")
    .header("sec-ch-ua-platform", "\"Android\"")
    .header("sec-ch-ua", "\"Android WebView\";v=\"149\", \"Chromium\";v=\"149\", \"Not)A;Brand\";v=\"24\"")
    .header("x-response-format", "JSONStream")
    .header("sec-ch-ua-mobile", "?1")
    .header("x-asbd-id", "359341")
    .header("x-fb-lsd", "AdRCh7SdER7Za5PotUuics5fFt0")
    .header("x-requested-with", "XMLHttpRequest")
    .header("origin", a.b1().dropLast(1))
    .header("sec-fetch-site", "same-origin")
    .header("sec-fetch-mode", "cors")
    .header("sec-fetch-dest", "empty")
    .header("referer", "${a.b1()}reg/?is_two_steps_login=0&cid=103&refsrc=deprecated&soft=hjk")
    .header("priority", "u=1, i")
    .header("Cookie", "datr=$cookieDatr")
    .post(formBody)
    .build()

  activeClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure(e.message ?: "Account creation failed due to network error")
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        val headers = res.headers
        val cookiesList = headers.values("Set-Cookie")
        
        val cookieMap = mutableMapOf<String, String>()
        cookieMap["datr"] = cookieDatr

        for (cookieStr in cookiesList) {
          val cookieParts = cookieStr.split(";").firstOrNull()?.split("=")
          if (cookieParts != null && cookieParts.size == 2) {
            val key = cookieParts[0].trim()
            val value = cookieParts[1].trim()
            cookieMap[key] = value
          }
        }

        val cUser = cookieMap["c_user"]
        if (cUser != null) {
          val requiredKeys = listOf("datr", "sb", "ps_l", "ps_n", "m_pixel_ratio", "wd", "c_user", "fr", "xs")
          val cookiePartsList = mutableListOf<String>()
          for (k in requiredKeys) {
            val v = cookieMap[k]
            if (v != null) {
              cookiePartsList.add("$k=$v")
            }
          }
          val formattedCookies = cookiePartsList.joinToString("; ")
          onSuccess(cUser, "$fname $lname", formattedCookies)
        } else {
          onFailure("অ্যাকাউন্ট তৈরি করা যায়নি! ফেসবুক লিমিটেড থেকে c_user পাওয়া যায়নি।")
        }
      }
    }
  })
}

// Polling OTP list from API
fun checkOtpForPhone(phone: String, onSuccess: (String, String) -> Unit, onFailure: () -> Unit) {
  val request = Request.Builder()
    .url("${z.API_BASE_URL}/success-otp")
    .addHeader("mauthapi", z.API_KEY)
    .addHeader("Content-Type", "application/json")
    .build()

  okHttpClient.newCall(request).enqueue(object : okhttp3.Callback {
    override fun onFailure(call: okhttp3.Call, e: IOException) {
      onFailure()
    }

    override fun onResponse(call: okhttp3.Call, response: Response) {
      response.use { res ->
        if (!res.isSuccessful) {
          onFailure()
          return
        }
        val bodyString = res.body?.string() ?: ""
        try {
          val json = JSONObject(bodyString)
          val meta = json.optJSONObject("meta")
          if (meta?.optInt("code") == 200) {
            val dataObj = json.optJSONObject("data")
            val otps = dataObj?.optJSONArray("otps")
            if (otps != null) {
              val cleanTargetPhone = phone.replace("+", "").replace(" ", "").replace("-", "").trim()
              for (i in 0 until otps.length()) {
                val otpItem = otps.getJSONObject(i)
                val otpNumber = otpItem.optString("number", "").replace("+", "").replace(" ", "").replace("-", "").trim()
                val isPhoneMatch = otpNumber.isNotEmpty() && cleanTargetPhone.isNotEmpty() && (
                  otpNumber == cleanTargetPhone ||
                  otpNumber.endsWith(cleanTargetPhone) ||
                  cleanTargetPhone.endsWith(otpNumber) ||
                  (otpNumber.length >= 8 && cleanTargetPhone.length >= 8 && otpNumber.takeLast(8) == cleanTargetPhone.takeLast(8))
                )
                if (isPhoneMatch) {
                  val message = otpItem.optString("message", "")
                  if (message.isNotEmpty()) {
                    val otpCode = extractOtpFromText(message)
                    if (isValidOtpCode(otpCode)) {
                      onSuccess(otpCode, message)
                      return
                    }
                  }
                }
              }
            }
          }
          onFailure()
        } catch (e: Exception) {
          onFailure()
        }
      }
    }
  })
}

fun isValidOtpCode(otp: String?): Boolean {
  if (otp.isNullOrBlank()) return false
  val clean = otp.trim()
  if (clean == "N/A" || clean.contains("ব্যর্থ") || clean.contains("failed", ignoreCase = true) || clean.contains("error", ignoreCase = true) || clean.contains("expired", ignoreCase = true) || clean.contains("অপেক্ষারত")) {
    return false
  }
  return (clean.length == 5 || clean.length == 6 || clean.length == 8) && clean.all { it.isDigit() }
}

private fun extractOtpFromText(text: String): String {
  if (text.isBlank()) return "N/A"
  if (text.contains("ব্যর্থ") || text.contains("failed", ignoreCase = true) || text.contains("error", ignoreCase = true)) {
    return "N/A"
  }

  // 1. Check FB-XXXXX pattern (strictly 5, 6, or 8 digits)
  val fbPattern = Regex("""FB[- ]?(\d{5,8})""", RegexOption.IGNORE_CASE)
  val fbMatch = fbPattern.find(text)
  if (fbMatch != null) {
    val code = fbMatch.groupValues[1]
    if (isValidOtpCode(code)) return code
  }

  // 2. Check "code is XXXXX" or "code: XXXXX" or "কোড XXXXX"
  val codePattern = Regex("""(?:code|OTP|কোড|verification|confirmation)[:\sisহলো]*(\d{5,8})""", RegexOption.IGNORE_CASE)
  val codeMatch = codePattern.find(text)
  if (codeMatch != null) {
    val code = codeMatch.groupValues[1]
    if (isValidOtpCode(code)) return code
  }

  // 3. Check "XXXXX is your [Facebook] code"
  val prefixPattern = Regex("""^(\d{5,8})\s+(?:is|is your|হলো|Facebook|FB)""", RegexOption.IGNORE_CASE)
  val prefixMatch = prefixPattern.find(text.trim())
  if (prefixMatch != null) {
    val code = prefixMatch.groupValues[1]
    if (isValidOtpCode(code)) return code
  }

  // 4. Standalone 5, 6, or 8 digit numbers surrounded by word boundaries
  val standaloneNumbers = Regex("""\b(\d{5}|\d{6}|\d{8})\b""").findAll(text).map { it.value }.toList()
  for (num in standaloneNumbers) {
    if (isValidOtpCode(num)) {
      return num
    }
  }

  return "N/A"
}

private class AndroidIdInterface(val isEnabled: () -> Boolean, val context: Context) {
    @android.webkit.JavascriptInterface
    fun getAndroidId(): String {
        return if (isEnabled()) {
             Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "null"
        } else {
             "fake-android-id-12345"
        }
    }
}

@Composable
fun TargetedNoticeOverlay() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE) }
    var noticeText by remember { mutableStateOf<String?>(null) }
    var isVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Get the admin email from SharedPreferences (set in TypeResolver)
    val adminEmail = prefs.getString("my_admin_email", "owner") ?: "owner"
    val adminId = if (adminEmail == "owner") "owner" else adminEmail.lowercase().replace(".", ",")
    
    var lastNoticeTimestamp by remember { mutableStateOf(prefs.getLong("last_notice_ts_$adminId", 0L)) }

    LaunchedEffect(adminId) {
        while (true) {
            try {
                val auth = FirebaseAuth.getInstance()
                val token = try { auth.currentUser?.getIdToken(false)?.await()?.token } catch(e: Exception) { null }
                
                val url = "https://all-admin-pnal-default-rtdb.firebaseio.com/notices/$adminId.json" + 
                    if (token != null) "?auth=$token" else ""
                
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { okHttpClient.newCall(request).execute() }
                val body = response.body?.string() ?: ""
                response.close()
                
                if (body.isNotEmpty() && body != "null") {
                    val json = JSONObject(body)
                    val text = json.optString("text", "")
                    val ts = json.optLong("timestamp", 0L)
                    
                    if (ts > lastNoticeTimestamp && text.isNotEmpty()) {
                        noticeText = text
                        isVisible = true
                        lastNoticeTimestamp = ts
                        prefs.edit().putLong("last_notice_ts_$adminId", ts).apply()
                        
                        // Hide after 5 seconds
                        kotlinx.coroutines.delay(5000)
                        isVisible = false
                    }
                }
            } catch (e: Exception) { }
            kotlinx.coroutines.delay(10000) // Check every 10 seconds
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(16.dp).statusBarsPadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B)),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier.fillMaxWidth().clickable { isVisible = false }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Notice from Admin", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(noticeText ?: "", color = Color.Black, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
  companion object {
    var savedWebViewBundle: Bundle? = null
    var activeWebView: WebView? = null
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    if (savedInstanceState != null) {
      savedWebViewBundle = savedInstanceState.getBundle("saved_webview_state")
    }
    try {
      if (FirebaseApp.getApps(this).isEmpty()) {
        try {
          FirebaseApp.initializeApp(this)
        } catch (innerEx: Exception) {
          val options = com.google.firebase.FirebaseOptions.Builder()
            .setApplicationId("1:171803187901:android:06f962b35043d369bcf5d4")
            .setApiKey("AIzaSyDoegs-mez3YrhxM_uzx6Q6vKqifR5FXYQ")
            .setDatabaseUrl("https://all-admin-pnal-default-rtdb.firebaseio.com")
            .setProjectId("my-original-apk")
            .setStorageBucket("my-original-apk.firebasestorage.app")
            .build()
          FirebaseApp.initializeApp(this, options)
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    a.c(this)

    // Set up global authenticator for authenticated proxies
    val prefs = getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE)
    java.net.Authenticator.setDefault(object : java.net.Authenticator() {
      override fun getPasswordAuthentication(): java.net.PasswordAuthentication? {
        val proxyUser = prefs.getString("proxy_user", "") ?: ""
        val proxyPass = prefs.getString("proxy_pass", "") ?: ""
        val proxyHost = prefs.getString("proxy_host", "") ?: ""
        val proxyPort = prefs.getString("proxy_port", "") ?: ""
        
        // Auto-authenticate if proxy details are set
        if (proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
          return java.net.PasswordAuthentication(proxyUser, proxyPass.toCharArray())
        }
        return null
      }
    })

    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        TargetedNoticeOverlay()
        MainScreen()
      }
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    val webBundle = Bundle()
    activeWebView?.saveState(webBundle)
    outState.putBundle("saved_webview_state", webBundle)
    savedWebViewBundle = webBundle
  }
}

private fun getCookieValue(cookieString: String?, key: String): String? {
  if (cookieString.isNullOrBlank()) return null
  val cookies = cookieString.split(";")
  for (cookie in cookies) {
    val parts = cookie.trim().split("=", limit = 2)
    if (parts.size == 2 && parts[0].trim() == key) {
      return parts[1].trim()
    }
  }
  return null
}


@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MainScreen() {
  val context = LocalContext.current
  val activity = context as? Activity
  val haptic = LocalHapticFeedback.current
  
  var dynamicButtons by remember { mutableStateOf(ButtonNames()) }

  LaunchedEffect(Unit) {
    while(true) {
      fetchDynamicButtonNames { names ->
        dynamicButtons = names
      }
      kotlinx.coroutines.delay(2000)
    }
  }

  val prefs = remember { context.getSharedPreferences("fb_creator_prefs", Context.MODE_PRIVATE) }
  var isAppAuthorized by remember { mutableStateOf<Boolean?>(prefs.getBoolean("is_app_authorized_cached", true)) }

  val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
  var deviceNotApprovedDialog by remember { mutableStateOf(false) }
  var blockReason by remember { mutableStateOf("") }
  var expiryDateText by remember { mutableStateOf("") }
  var userName by remember { mutableStateOf("Unknown") }

  LaunchedEffect(Unit) {
    while(true) {
      val rawResult = a.checkStatusAndDevice(deviceId)
      if (rawResult == "GLOBAL_OFF") {
        activity?.finishAffinity()
        return@LaunchedEffect
      }
      if (rawResult == "NETWORK_ERROR") {
        kotlinx.coroutines.delay(4000)
        continue
      }
      
      val parts = rawResult.split("|")
      val result = parts[0]
      if (parts.size > 1) {
        userName = parts[1]
      }

      when {
        result == "BANNED" -> {
          blockReason = "BANNED"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
          prefs.edit().putBoolean("is_app_authorized_cached", false).apply()
        }
        result == "NOT_FOUND" -> {
          blockReason = "NOT_FOUND"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
          prefs.edit().putBoolean("is_app_authorized_cached", false).apply()
        }
        result.startsWith("EXPIRED_") -> {
          blockReason = "EXPIRED"
          val timestamp = result.removePrefix("EXPIRED_").toLongOrNull() ?: 0L
          expiryDateText = if (timestamp > 0) java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp)) else "N/A"
          deviceNotApprovedDialog = true
          isAppAuthorized = false
          prefs.edit().putBoolean("is_app_authorized_cached", false).apply()
        }
        result.startsWith("APPROVED_") -> {
          isAppAuthorized = true
          deviceNotApprovedDialog = false
          prefs.edit().putBoolean("is_app_authorized_cached", true).apply()
        }
      }
      kotlinx.coroutines.delay(3000)
    }
  }

  if (deviceNotApprovedDialog) {
    val mainTitle = when (blockReason) {
      "BANNED" -> "অ্যাকাউন্ট ব্যান করা হয়েছে"
      "EXPIRED" -> "ডিভাইসের মেয়াদ শেষ"
      else -> "ডিভাইস ভেরিফিকেশন প্রয়োজন"
    }
    val descText = when (blockReason) {
      "BANNED" -> "অ্যাডমিন আপনার ডিভাইসটি ব্লক করে দিয়েছেন।"
      "EXPIRED" -> "আপনার ডিভাইসের মেয়াদ শেষ হয়ে গেছে! অনুগ্রহ করে অ্যাডমিনের সাথে যোগাযোগ করুন।"
      else -> "আপনার ডিভাইসটি এখনো অনুমোদিত নয়। অ্যাডমিনের সাথে যোগাযোগ করুন।"
    }

    val infiniteTransition = rememberInfiniteTransition(label = "loginCyberGlow")
    val pulseScale by infiniteTransition.animateFloat(
      initialValue = 0.96f,
      targetValue = 1.04f,
      animationSpec = infiniteRepeatable(
        animation = tween(1300, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "pulseScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
      initialValue = 0.45f,
      targetValue = 1.0f,
      animationSpec = infiniteRepeatable(
        animation = tween(1500, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "glowAlpha"
    )

    // Button 1 Specific: WhatsApp Chat Icon Wiggle/Rock Animation (-8 to +8 degrees)
    val wsWiggle by infiniteTransition.animateFloat(
      initialValue = -8f,
      targetValue = 8f,
      animationSpec = infiniteRepeatable(
        animation = tween(650, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "wsWiggle"
    )

    // Button 2 Specific: Telegram Admin Paper Plane Flight Gliding Tilt & Float
    val tgPlaneTilt by infiniteTransition.animateFloat(
      initialValue = -10f,
      targetValue = 10f,
      animationSpec = infiniteRepeatable(
        animation = tween(850, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "tgPlaneTilt"
    )
    val tgPlaneFly by infiniteTransition.animateFloat(
      initialValue = -2.5f,
      targetValue = 2.5f,
      animationSpec = infiniteRepeatable(
        animation = tween(850, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "tgPlaneFly"
    )

    // Button 3 Specific: Telegram Channel Megaphone Broadcast Pulse / Soundwave Flash
    val channelScale by infiniteTransition.animateFloat(
      initialValue = 0.88f,
      targetValue = 1.20f,
      animationSpec = infiniteRepeatable(
        animation = tween(550, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "channelScale"
    )

    // Button 4 Specific: Verify Continuous 360-degree Smooth Radar Scanner Rotation
    val verifyRotation by infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(
        animation = tween(2000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
      ),
      label = "verifyRotation"
    )

    // Device ID Copy Button Specific: Breathing Scale Animation
    val copyIconScale by infiniteTransition.animateFloat(
      initialValue = 0.85f,
      targetValue = 1.15f,
      animationSpec = infiniteRepeatable(
        animation = tween(750, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse
      ),
      label = "copyIconScale"
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF070B14))
        .padding(16.dp),
      contentAlignment = Alignment.Center
    ) {
      Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1120)),
        border = BorderStroke(
          1.5.dp,
          Brush.horizontalGradient(
            listOf(
              Color(0xFF00E5FF),
              Color(0xFFA855F7),
              Color(0xFFEC4899),
              Color(0xFF00E5FF)
            )
          )
        ),
        modifier = Modifier
          .fillMaxWidth()
          .padding(4.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp)
        ) {
          // Top Animated Header Badge: 👑🅾🅁🄽🄾🄱- ⟨🆉🅾🅽🅴⟩💭
          Surface(
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFF0F172A),
            border = BorderStroke(
              1.5.dp,
              Brush.horizontalGradient(
                listOf(
                  Color(0xFFF59E0B),
                  Color(0xFF00E5FF),
                  Color(0xFFA855F7),
                  Color(0xFFF59E0B)
                )
              )
            ),
            modifier = Modifier
              .scale(pulseScale)
              .padding(bottom = 12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .background(Color(0xFF10B981), shape = androidx.compose.foundation.shape.CircleShape)
              )
              Spacer(Modifier.width(8.dp))
              Text(
                text = "👑🅾🅁🄽🄾🄱- ⟨🆉🅾🅽🅴⟩💭",
                color = Color(0xFFFCD34D),
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp
              )
            }
          }

          // Lock Icon in animated glowing Circle
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(74.dp)
              .scale(pulseScale)
          ) {
            Surface(
              shape = androidx.compose.foundation.shape.CircleShape,
              color = Color(0xFF00E5FF).copy(alpha = 0.12f * glowAlpha),
              modifier = Modifier.size(74.dp)
            ) {}
            Surface(
              shape = androidx.compose.foundation.shape.CircleShape,
              color = Color(0xFF07142A),
              border = BorderStroke(2.dp, Color(0xFF00E5FF)),
              modifier = Modifier.size(64.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  tint = Color(0xFF00E5FF),
                  modifier = Modifier.size(32.dp)
                )
              }
            }
          }
          
          Spacer(Modifier.height(12.dp))
          
          Text(
            text = mainTitle,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
          )
          
          Spacer(Modifier.height(12.dp))
          
          // Red-bordered Status Box with animated glow
          Surface(
            color = Color(0xFF180A0E),
            border = BorderStroke(1.2.dp, Color(0xFFEF4444).copy(alpha = 0.6f + 0.4f * glowAlpha)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = "ব্যবহারকারী: $userName",
                color = Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
              )
              Spacer(Modifier.height(5.dp))
              Text(
                text = descText,
                color = Color(0xFFF87171),
                textAlign = TextAlign.Center,
                fontSize = 13.sp
              )
              if (blockReason == "EXPIRED" && expiryDateText.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                  "(মেয়াদ উত্তীর্ণ: $expiryDateText)",
                  color = Color(0xFFFDA4AF),
                  fontSize = 11.sp
                )
              }
            }
          }

          Spacer(Modifier.height(12.dp))

          // Device ID Box
          Surface(
            color = Color(0xFF070B14),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(
              modifier = Modifier.padding(12.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text("আপনার ডিভাইস আইডি (Device ID):", color = Color(0xFFE2E8F0), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
              Spacer(Modifier.height(8.dp))
              
              Surface(
                color = Color(0xFF0B1322),
                border = BorderStroke(1.2.dp, Color(0xFF2563EB).copy(alpha = 0.6f + 0.4f * glowAlpha)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
              ) {
                Text(
                  text = deviceId,
                  color = Color(0xFF00E5FF),
                  textAlign = TextAlign.Center,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  modifier = Modifier.padding(10.dp)
                )
              }
              
              Spacer(Modifier.height(8.dp))
              
              Button(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("Device ID", deviceId)
                  clipboard.setPrimaryClip(clip)
                  Toast.makeText(context, "ডিভাইস আইডি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0F172A),
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f + 0.5f * glowAlpha)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth().height(38.dp)
              ) {
                Row(
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(15.dp).scale(copyIconScale)
                  )
                  Spacer(Modifier.width(8.dp))
                  Text("ডিভাইস আইডি কপি করুন", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFE0F2FE))
                }
              }
            }
          }

          Spacer(Modifier.height(14.dp))

          // 2x2 Action Buttons with Vibrant Cyber Color Grading & Specific Animated Icons
          // Row 1: WhatsApp (Emerald Green + Wiggle) + Telegram Admin (Electric Blue + Flying Plane)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://wa.me/8801300349649"))
                context.startActivity(intent)
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(
                  Brush.horizontalGradient(listOf(Color(0xFF047857), Color(0xFF10B981), Color(0xFF059669))),
                  shape = RoundedCornerShape(12.dp)
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
              ),
              border = BorderStroke(1.2.dp, Color(0xFF34D399)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Chat,
                  contentDescription = null,
                  modifier = Modifier
                    .size(17.dp)
                    .rotate(wsWiggle),
                  tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = dynamicButtons.btn_admin_ws,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 2
                )
              }
            }

            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/Ornob_vai"))
                context.startActivity(intent)
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(
                  Brush.horizontalGradient(listOf(Color(0xFF1D4ED8), Color(0xFF2563EB), Color(0xFF0284C7))),
                  shape = RoundedCornerShape(12.dp)
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
              ),
              border = BorderStroke(1.2.dp, Color(0xFF38BDF8)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Send,
                  contentDescription = null,
                  modifier = Modifier
                    .size(17.dp)
                    .offset(x = tgPlaneFly.dp, y = (-tgPlaneFly).dp)
                    .rotate(tgPlaneTilt),
                  tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = dynamicButtons.btn_admin_tg,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 2
                )
              }
            }
          }

          Spacer(Modifier.height(8.dp))

          // Row 2: Telegram Channel (Cyber Crimson + Beacon Pulse) + Verify (Dark Cyber Slate + 360 Radar Spin)
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://t.me/+sjTCu5CFkscxNWU1"))
                context.startActivity(intent)
              },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(
                  Brush.horizontalGradient(listOf(Color(0xFF9F1239), Color(0xFFE11D48), Color(0xFFDC2626))),
                  shape = RoundedCornerShape(12.dp)
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
              ),
              border = BorderStroke(1.2.dp, Color(0xFFFB7185)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Campaign,
                  contentDescription = null,
                  modifier = Modifier
                    .size(17.dp)
                    .scale(channelScale),
                  tint = Color.White
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = dynamicButtons.btn_tg_channel,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 2
                )
              }
            }

            Button(
              onClick = { activity?.recreate() },
              modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .background(
                  Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF312E81))),
                  shape = RoundedCornerShape(12.dp)
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
              ),
              border = BorderStroke(1.2.dp, Color(0xFF00E5FF)),
              shape = RoundedCornerShape(12.dp),
              contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = null,
                  modifier = Modifier
                    .size(17.dp)
                    .rotate(verifyRotation),
                  tint = Color(0xFF00E5FF)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                  text = dynamicButtons.btn_verify,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  textAlign = TextAlign.Center,
                  maxLines = 2
                )
              }
            }
          }
        }
      }
    }
    return
  }

  if (isAppAuthorized == null) {
      Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
      }
      return
  }

  val clipboardManager = LocalClipboardManager.current
  val scope = rememberCoroutineScope()
  val snackbarHostState = remember { SnackbarHostState() }
  
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }
  var webView: WebView? by remember { mutableStateOf(null) }
  var webViewKey by remember { mutableIntStateOf(0) }
  var isLoading by remember { mutableStateOf(false) }
  var progress by remember { mutableStateOf(0) }
  var currentUrl by remember { mutableStateOf(a.b1()) }
  var canGoBack by remember { mutableStateOf(false) }
  var canGoForward by remember { mutableStateOf(false) }

  // State variables for auto creation dialog - persisted across backgrounding
  val savedActiveDialog = remember { prefs.getString("active_dialog", "none") ?: "none" }
  var showCreatorDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "creator") }
  var showHistoryDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "history") }
  var showCookieLoginDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "cookie_login") }
  var showSetRangeDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "range") }
  var showSettingsDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "settings") }
  var showLiveConsoleDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "console") }
  var showMusicDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "music") }
  val isMusicPlaying by MusicManager.isPlaying.collectAsState()

  val musicInfiniteTrans = rememberInfiniteTransition(label = "music_global_anim")
  val musicIconRotate by musicInfiniteTrans.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(2800, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "music_rot"
  )
  val musicPulseScale by musicInfiniteTrans.animateFloat(
    initialValue = 0.93f,
    targetValue = 1.07f,
    animationSpec = infiniteRepeatable(
      animation = tween(650, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "music_scale"
  )

  var isDesktopMode by remember { mutableStateOf(prefs.getBoolean("is_desktop_mode", false)) }
  var isProxyEnabled by remember { mutableStateOf(prefs.getBoolean("is_proxy_enabled", false)) }
  var dnsServer by remember { mutableStateOf("8.8.8.8") }
  
  var cookieLoginInput by remember { mutableStateOf("") }
  var rangesList by remember { mutableStateOf<List<String>>(emptyList()) }
  var selectedRange by remember { mutableStateOf(prefs.getString("saved_range", "") ?: "") }
  var isFetchingRanges by remember { mutableStateOf(false) }
  var activePhoneChecking by remember { mutableStateOf("") }
  var currentCreationStatus by remember { mutableStateOf("") }
  var isCreatingAccount by remember { mutableStateOf(false) }
  var lastCreatedPhone by remember { mutableStateOf("") }
  var lastCreatedUid by remember { mutableStateOf("") }
  var lastCreatedCookies by remember { mutableStateOf("") }
  var lastCreatedOtp by remember { mutableStateOf("") }
  var lastSuccessCookies by remember { mutableStateOf(prefs.getString("last_success_cookies", "") ?: "") }

  // Proxy state variables
  var proxyHost by remember { mutableStateOf(prefs.getString("proxy_host", "") ?: "") }
  var proxyPort by remember { mutableStateOf(prefs.getString("proxy_port", "") ?: "") }
  var proxyUser by remember { mutableStateOf(prefs.getString("proxy_user", "") ?: "") }
  var proxyPass by remember { mutableStateOf(prefs.getString("proxy_pass", "") ?: "") }
  var showProxyConfigDialog by rememberSaveable { mutableStateOf(savedActiveDialog == "proxy") }
  var customPassword by remember { mutableStateOf(prefs.getString("saved_password", "") ?: "Pass@" + (1000..9999).random().toString()) }
  var selectedCountry by remember { mutableStateOf(prefs.getString("selected_country", "Bangladesh") ?: "Bangladesh") }

  // Sync active dialog state so returning from background keeps user in current section
  LaunchedEffect(showCreatorDialog, showHistoryDialog, showCookieLoginDialog, showSetRangeDialog, showSettingsDialog, showLiveConsoleDialog, showProxyConfigDialog, showMusicDialog) {
    val active = when {
      showCreatorDialog -> "creator"
      showHistoryDialog -> "history"
      showCookieLoginDialog -> "cookie_login"
      showSetRangeDialog -> "range"
      showSettingsDialog -> "settings"
      showLiveConsoleDialog -> "console"
      showProxyConfigDialog -> "proxy"
      showMusicDialog -> "music"
      else -> "none"
    }
    prefs.edit().putString("active_dialog", active).apply()
  }

  // Loop for continuous background OTP checking: runs from startup without needing to open the tool dialog!
  LaunchedEffect(Unit) {
    while (true) {
      val now = System.currentTimeMillis()
      val history = getHistory(context)
      val pendingItems = history.filter { 
        !isValidOtpCode(it.otp) && (now - it.timestamp < 30 * 60 * 1000L)
      }

      val targetPhones = mutableSetOf<String>()
      if (activePhoneChecking.isNotEmpty() && !hasValidOtpInHistory(context, activePhoneChecking)) {
        targetPhones.add(activePhoneChecking)
      }
      if (lastCreatedPhone.isNotEmpty() && !hasValidOtpInHistory(context, lastCreatedPhone)) {
        targetPhones.add(lastCreatedPhone)
      }
      pendingItems.forEach { 
        if (!hasValidOtpInHistory(context, it.phone)) {
          targetPhones.add(it.phone)
        }
      }

      if (targetPhones.isNotEmpty()) {
        for (targetPhone in targetPhones) {
          checkOtpForPhone(
            phone = targetPhone,
            onSuccess = { otp, _ ->
              if (isValidOtpCode(otp)) {
                // Copy directly to Android OS system clipboard so Gboard and system clipboard immediately registers it!
                val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("OTP", otp)
                sysClipboard.setPrimaryClip(clip)
                clipboardManager.setText(AnnotatedString(otp))

                val alreadyHasOtp = hasValidOtpInHistory(context, targetPhone)
                updateOtpInHistory(context, targetPhone, otp)
                if (!alreadyHasOtp) {
                  incrementTodayOtpCount(context)
                }

                if (targetPhone == activePhoneChecking) activePhoneChecking = ""
                if (targetPhone == lastCreatedPhone) lastCreatedPhone = ""
                lastCreatedOtp = otp

                // System Toast for instant feedback outside tool
                Toast.makeText(context, "OTP ($otp) ক্লিপবোর্ডে কপি হয়েছে!", Toast.LENGTH_LONG).show()
                scope.launch {
                  snackbarHostState.showSnackbar("OTP স্বয়ংক্রিয়ভাবে ক্লিপবোর্ডে কপি হয়েছে: $otp")
                }
              }
            },
            onFailure = {}
          )
        }
      }

      kotlinx.coroutines.delay(2000)
    }
  }

  // Loop for fetching ranges - updates every 10 seconds automatically from startup
  LaunchedEffect(Unit) {
/*
    isFetchingRanges = true
    while (true) {
      // ... (code)
      kotlinx.coroutines.delay(10000)
    }
*/
  }

  // Support system back press navigation inside WebView
  BackHandler(enabled = canGoBack) {
    webView?.goBack()
  }

  if (showLiveConsoleDialog) {
    var liveRanges by remember { mutableStateOf<List<String>>(emptyList()) }
    var isFetchingLiveRanges by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    
    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
    
    fun fetchLive() {
       isFetchingLiveRanges = true
       errorMessage = ""
       fetchFacebookRanges(
         onSuccess = { r -> 
            liveRanges = r
            isFetchingLiveRanges = false
         },
         onFailure = { e ->
            errorMessage = e
            isFetchingLiveRanges = false
         }
       )
    }

    LaunchedEffect(Unit) {
       fetchLive()
    }

    AlertDialog(
      onDismissRequest = { showLiveConsoleDialog = false },
      containerColor = Color(0xFF070B14),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = {
         Row(
           modifier = Modifier.fillMaxWidth(),
           horizontalArrangement = Arrangement.SpaceBetween,
           verticalAlignment = Alignment.CenterVertically
         ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Terminal,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(20.dp)
              )
              Text(
                dynamicButtons.btn_live_console,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color.White
              )
            }
            IconButton(onClick = { fetchLive() }) {
               Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF00E5FF))
            }
         }
      },
      text = {
         Column(modifier = Modifier.fillMaxWidth()) {
            if (isFetchingLiveRanges) {
               Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                 CircularProgressIndicator(color = Color(0xFF00E5FF))
               }
            } else if (errorMessage.isNotEmpty()) {
               Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
            } else if (liveRanges.isEmpty()) {
               Text("কোনো রেঞ্জ পাওয়া যায়নি।", color = Color(0xFF94A3B8))
            } else {
               Text(
                 "Facebook Live Ranges (Click to auto setup):",
                 fontSize = 12.sp,
                 color = Color(0xFF00E5FF),
                 fontWeight = FontWeight.SemiBold
               )
               Spacer(Modifier.height(8.dp))
               LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                  items(liveRanges.size) { index ->
                     val range = liveRanges[index]
                     val countryInfo = getCountryForRange(range)
                     val displayText = if (countryInfo.isNotEmpty()) "$range [$countryInfo]" else range
                     Card(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 4.dp)
                          .clickable {
                             val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                             val clip = android.content.ClipData.newPlainText("Range", range)
                             sysClipboard.setPrimaryClip(clip)
                             clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(range))
                             selectedRange = range
                             prefs.edit().putString("saved_range", range).apply()
                             Toast.makeText(context, "$displayText Copied & Selected!", Toast.LENGTH_SHORT).show()
                             showLiveConsoleDialog = false
                          },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1527))
                     ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                           Text(
                              text = displayText,
                              fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                              fontWeight = FontWeight.Bold,
                              fontSize = 13.sp,
                              color = Color.White
                           )
                           Icon(
                             imageVector = Icons.Default.ContentCopy,
                             contentDescription = "Copy Range",
                             tint = Color(0xFF00E5FF),
                             modifier = Modifier.size(16.dp)
                           )
                        }
                     }
                  }
               }
            }
         }
      },
      confirmButton = {
         TextButton(onClick = { showLiveConsoleDialog = false }) {
            Text("Close", color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
         }
      }
    )
  }

  if (showSettingsDialog) {
    AlertDialog(
      onDismissRequest = { showSettingsDialog = false },
      title = { Text("Settings") },
      text = {
        Column {
            OutlinedTextField(
                value = dnsServer,
                onValueChange = { 
                    dnsServer = it 
                    prefs.edit().putString("saved_dns", it).apply()
                },
                label = { Text("DNS Server (Internal Networking)") }
            )
        }
      },
      confirmButton = {
        TextButton(onClick = { showSettingsDialog = false }) { Text("OK") }
      }
    )
  }

  if (showMusicDialog) {
    MusicDialog(
      onDismiss = { showMusicDialog = false }
    )
  }
  
  Scaffold(
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
          Column {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Secure Connection",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
              )
              Text(
                text = "FB Limited",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
            Text(
              text = if (isLoading) "লোড হচ্ছে... $progress%" else "নিরাপদ সংযোগ",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = if (isLoading) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        actions = {
          TextButton(onClick = { showLiveConsoleDialog = true }) {
            Icon(Icons.Default.Terminal, contentDescription = "Live Console", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(dynamicButtons.btn_live_console, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
          }
          IconButton(onClick = { showSettingsDialog = true }) {
            Icon(Icons.Default.Settings, contentDescription = "Settings")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        )
      )
    },
    bottomBar = {
      Surface(
        tonalElevation = 4.dp,
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 3.dp),
          verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          // Top Row: Navigation on left, Refresh and Clear Data on right
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            // Navigation Group (Back, Forward, Home)
            Row(
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              IconButton(
                onClick = { webView?.goBack() },
                enabled = canGoBack,
                modifier = Modifier
                  .size(28.dp)
                  .testTag("back_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "পেছনে",
                  modifier = Modifier.size(16.dp)
                )
              }
              IconButton(
                onClick = { webView?.goForward() },
                enabled = canGoForward,
                modifier = Modifier
                  .size(28.dp)
                  .testTag("forward_button")
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = "সামনে",
                  modifier = Modifier.size(16.dp)
                )
              }
              IconButton(
                onClick = {
                  webView?.loadUrl(a.b1())
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Home,
                  contentDescription = "হোম",
                  modifier = Modifier.size(16.dp),
                  tint = MaterialTheme.colorScheme.onSurface
                )
              }
            }

            // Compact Actions Group (Reload, Clear Data)
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Small Reload Button
              Button(
                onClick = { webView?.reload() },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondaryContainer,
                  contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .height(25.dp)
                  .testTag("reload_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "রিলোড",
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "রিলোড",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  )
                }
              }

              // Small Clear Data Button
              Button(
                onClick = {
                  webView?.let { wv ->
                    // 1. Clear cookies
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.removeAllCookies { success ->
                      cookieManager.flush()
                    }
                    cookieManager.removeSessionCookies(null)

                    // 2. Clear storage & cache
                    WebStorage.getInstance().deleteAllData()
                    wv.clearCache(true)
                    wv.clearFormData()
                    wv.clearHistory()

                    // 3. Change useragent based on desktop mode setting
                    if (isDesktopMode) {
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                    }
                    wv.loadUrl(a.b1())

                    // 5. Show success info
                    scope.launch {
                      snackbarHostState.showSnackbar(
                        message = "সমস্ত ডাটা ও কুকি মুছে ফেলা হয়েছে!",
                        actionLabel = "ঠিক আছে",
                        duration = SnackbarDuration.Short
                      )
                    }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .height(25.dp)
                  .testTag("clear_data_button")
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = "ডাটা মুছুন",
                    modifier = Modifier.size(12.dp)
                  )
                  Text(
                    text = "ডাটা মুছুন",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold)
                  )
                }
              }
            }
          }

          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 2.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
          ) {
            // Quick Access Thumb-Level Row: Live Console, Music & Settings
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              // Live Console Button (Prominent Cyber Cyan Card)
              Button(
                onClick = { showLiveConsoleDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0B192C),
                  contentColor = Color(0xFF00E5FF)
                ),
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .weight(1.1f)
                  .height(30.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Live Console",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = dynamicButtons.btn_live_console,
                    color = Color(0xFF00E5FF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                  )
                }
              }

              // Music Player Button (Cyber Neon Rose Card with Animation & Haptic)
              Button(
                onClick = {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  showMusicDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isMusicPlaying) Color(0xFF35082A) else Color(0xFF1E0A1A),
                  contentColor = if (isMusicPlaying) Color(0xFFFF2A85) else Color(0xFFFB7185)
                ),
                border = BorderStroke(1.dp, if (isMusicPlaying) Color(0xFFFF2A85) else Color(0xFFE11D48)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .weight(1.1f)
                  .height(30.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Music Player",
                    tint = if (isMusicPlaying) Color(0xFFFF2A85) else Color(0xFFFB7185),
                    modifier = Modifier
                      .size(14.dp)
                      .then(
                        if (isMusicPlaying) Modifier.rotate(musicIconRotate).scale(musicPulseScale)
                        else Modifier
                      )
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = if (isMusicPlaying) "Playing" else "Music",
                    color = if (isMusicPlaying) Color(0xFFFF2A85) else Color(0xFFFB7185),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                  )
                }
              }

              // Settings Button (Prominent Cyber Purple Card)
              Button(
                onClick = { showSettingsDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF1E1035),
                  contentColor = Color(0xFFA855F7)
                ),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                modifier = Modifier
                  .weight(0.9f)
                  .height(30.dp)
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = Color(0xFFA855F7),
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(
                    text = "Settings",
                    color = Color(0xFFA855F7),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    maxLines = 1
                  )
                }
              }
            }

            // Bottom Rows: Ultra Compact Button Sizes
            val compactBtnPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)
            val compactTextStyle = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontSize = 9.sp
            )
            val compactIconSize = 12.dp
            val compactBtnHeight = 28.dp

            // Row 0: Start (Main Feature)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  scope.launch {
                    // 1. Clear Data & Initial Load
                    webView?.let { wv ->
                      val cookieManager = CookieManager.getInstance()
                      cookieManager.removeAllCookies { }
                      cookieManager.flush()
                      WebStorage.getInstance().deleteAllData()
                      wv.clearCache(true)
                      wv.loadUrl(a.b1())
                    }
                    
                    kotlinx.coroutines.delay(2000)

                    // 2. Auto Login with last success cookies
                    if (lastSuccessCookies.isNotBlank()) {
                      val cookieManager = CookieManager.getInstance()
                      cookieManager.setAcceptCookie(true)
                      val domains = listOf(
                        "https://.facebook.com", "https://facebook.com",
                        "https://m.facebook.com", "https://limited.facebook.com"
                      )
                      val trimmed = lastSuccessCookies.trim()
                      if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                        try {
                          val jsonArray = JSONArray(trimmed)
                          for (i in 0 until jsonArray.length()) {
                            val obj = jsonArray.getJSONObject(i)
                            val name = obj.optString("name")
                            val value = obj.optString("value")
                            val domain = obj.optString("domain", ".facebook.com")
                            val path = obj.optString("path", "/")
                            if (name.isNotEmpty()) {
                              val cookieString = "$name=$value; Domain=$domain; Path=$path"
                              domains.forEach { d -> cookieManager.setCookie(d, cookieString) }
                            }
                          }
                        } catch (e: Exception) {}
                      } else {
                        val parts = trimmed.split(";")
                        for (part in parts) {
                          val cleanPart = part.trim()
                          if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                            domains.forEach { d -> cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/") }
                          }
                        }
                      }
                      cookieManager.flush()
                    }

                    // 3. Forced Desktop Mode ON -> Load
                    webView?.let { wv ->
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                      wv.loadUrl(a.b1())
                    }
                    kotlinx.coroutines.delay(3000)

                    // 4. Reload to final state (Mobile Mode)
                    webView?.let { wv ->
                      isDesktopMode = false
                      prefs.edit().putBoolean("is_desktop_mode", false).apply()
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                      wv.reload()
                    }
                    
                    snackbarHostState.showSnackbar("Process Completed!")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color.Transparent,
                  contentColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp),
                border = BorderStroke(
                  width = 1.dp,
                  brush = Brush.horizontalGradient(
                    listOf(Color(0xFF00E5FF), Color(0xFF818CF8), Color(0xFFA855F7))
                  )
                ),
                contentPadding = compactBtnPadding,
                modifier = Modifier
                  .fillMaxWidth()
                  .height(compactBtnHeight)
                  .background(
                    brush = Brush.horizontalGradient(
                      listOf(
                        Color(0xFF0B192C),
                        Color(0xFF1E1B4B),
                        Color(0xFF1E1035)
                      )
                    ),
                    shape = RoundedCornerShape(4.dp)
                  )
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Start",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(compactIconSize).padding(end = 4.dp)
                  )
                  Text(
                    text = "Start (Clear, Login, Mode Fix)",
                    style = compactTextStyle.copy(
                      color = Color(0xFFF8FAFC),
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 0.3.sp
                    )
                  )
                }
              }
            }

            // Row 1: Tools
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { showCreatorDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0369A1), // Electric Sky Blue
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF00E5FF)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_bot_creator, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showSetRangeDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF6D28D9), // Vivid Purple/Violet
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFA855F7)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (selectedRange.isEmpty()) dynamicButtons.btn_range else selectedRange, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                  if (!cookies.isNullOrEmpty()) {
                    clipboardManager.setText(AnnotatedString(cookies))
                    scope.launch { snackbarHostState.showSnackbar("Cookies Copied!") }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF047857), // Neon Emerald
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF10B981)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_copy_cookie, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 2: Account
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  val cookies = CookieManager.getInstance().getCookie(webView?.url ?: a.b1())
                  val uid = cookies?.split(";")?.firstOrNull { it.trim().startsWith("c_user=") }?.substringAfter("=")?.trim()
                  if (!uid.isNullOrEmpty()) {
                    clipboardManager.setText(AnnotatedString(uid))
                    scope.launch { snackbarHostState.showSnackbar("UID ($uid) Copied!") }
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF1D4ED8), // Royal Sapphire Blue
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF38BDF8)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_copy_uid, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showHistoryDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFBE185D), // Cyber Pink/Magenta
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFF43F5E)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_history, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { showCookieLoginDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFFB45309), // Luxury Amber/Gold
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_cookie_login, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 3: Others
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = {
                  val randomGmail = generateRandomGmail()
                  clipboardManager.setText(AnnotatedString(randomGmail))
                  scope.launch { snackbarHostState.showSnackbar("Gmail Copied!") }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF0F766E), // Deep Cyber Teal
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF14B8A6)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_gmail_copy, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { webView?.loadUrl("https://m.facebook.com/login/") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF1E40AF), // Official FB Dark Blue
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF60A5FA)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_fb_login, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = { webView?.loadUrl("https://whoer.net/") },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF86198F), // Neon Fuchsia
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFFE879F9)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_whoer_ip, style = compactTextStyle, maxLines = 1)
              }
            }

            // Row 4: Proxy & Desktop Mode
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(2.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Button(
                onClick = { showProxyConfigDialog = true },
                colors = ButtonDefaults.buttonColors(
                  containerColor = Color(0xFF334155), // Dark Slate
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = dynamicButtons.btn_proxy_config, style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  isProxyEnabled = !isProxyEnabled
                  prefs.edit().putBoolean("is_proxy_enabled", isProxyEnabled).apply()
                  applyWebViewProxy(context, isProxyEnabled, proxyHost, proxyPort)
                  scope.launch {
                    snackbarHostState.showSnackbar(if (isProxyEnabled) "Proxy Enabled" else "Proxy Disabled")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isProxyEnabled) Color(0xFF15803D) else Color(0xFF991B1B), // Cyber Green or Red
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, if (isProxyEnabled) Color(0xFF22C55E) else Color(0xFFEF4444)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (isProxyEnabled) "Proxy: ON" else "Proxy: OFF", style = compactTextStyle, maxLines = 1)
              }
              Button(
                onClick = {
                  isDesktopMode = !isDesktopMode
                  prefs.edit().putBoolean("is_desktop_mode", isDesktopMode).apply()
                  webView?.let { wv ->
                    if (isDesktopMode) {
                      wv.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                    } else {
                      wv.settings.userAgentString = getRandomMobileUserAgent()
                    }
                    wv.reload()
                  }
                  scope.launch {
                    snackbarHostState.showSnackbar(if (isDesktopMode) "Desktop Mode ON" else "Desktop Mode OFF")
                  }
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isDesktopMode) Color(0xFFC2410C) else Color(0xFF1E293B), // Cyber Amber/Orange or Deep Dark Slate
                  contentColor = Color.White
                ),
                border = BorderStroke(1.dp, if (isDesktopMode) Color(0xFFFB923C) else Color(0xFF64748B)),
                shape = RoundedCornerShape(4.dp),
                contentPadding = compactBtnPadding,
                modifier = Modifier.weight(1f).height(compactBtnHeight)
              ) {
                Text(text = if (isDesktopMode) "Desktop: ON" else "Desktop: OFF", style = compactTextStyle, maxLines = 1)
              }
            }
          }
        }
      }
    }
  ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {

      // Elegant slim loading indicator
      AnimatedVisibility(
        visible = isLoading,
        enter = fadeIn(),
        exit = fadeOut()
      ) {
        LinearProgressIndicator(
          progress = { progress / 100f },
          modifier = Modifier.fillMaxWidth(),
          color = MaterialTheme.colorScheme.primary,
          trackColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .weight(1f)
          .background(MaterialTheme.colorScheme.background)
      ) {
        androidx.compose.runtime.key(webViewKey) {
          AndroidView(
            factory = { context ->
              webViewInstance ?: WebView(context).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(
                  android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                  android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
                
                // Essential settings for facebook and standard web apps
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.databaseEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.setSupportZoom(true)
                settings.builtInZoomControls = true
                settings.displayZoomControls = false
                settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                settings.safeBrowsingEnabled = false
                
                // Set Initial User Agent (Desktop or Mobile based on configuration)
                if (isDesktopMode) {
                  settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                } else {
                  settings.userAgentString = getRandomMobileUserAgent()
                }
                
                // Android ID Interface
                addJavascriptInterface(AndroidIdInterface({ true }, context), "AndroidIDInterface")

                // Accept Third-Party Cookies
                val cookieManager = CookieManager.getInstance()
                cookieManager.setAcceptCookie(true)
                cookieManager.setAcceptThirdPartyCookies(this, true)

                webViewClient = object : WebViewClient() {
                  override fun onRenderProcessGone(
                    view: WebView?,
                    detail: android.webkit.RenderProcessGoneDetail?
                  ): Boolean {
                    val didCrash = detail?.didCrash() ?: true
                    android.util.Log.e("WebView", "Chromium render process gone. Crashed: $didCrash")
                    try {
                      view?.let { wv ->
                        (wv.parent as? android.view.ViewGroup)?.removeView(wv)
                        wv.destroy()
                      }
                    } catch (e: Exception) {
                      e.printStackTrace()
                    }
                    webViewInstance = null
                    webView = null
                    scope.launch {
                      kotlinx.coroutines.delay(1500)
                      if (webViewInstance == null) {
                        webViewKey++
                      }
                    }
                    return true
                  }

                  override fun onReceivedSslError(
                    view: WebView?,
                    handler: android.webkit.SslErrorHandler?,
                    error: android.net.http.SslError?
                  ) {
                    handler?.proceed()
                  }

                  override fun onReceivedHttpAuthRequest(
                    view: WebView?,
                    handler: android.webkit.HttpAuthHandler?,
                    host: String?,
                    realm: String?
                  ) {
                    if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank() && proxyUser.isNotBlank() && proxyPass.isNotBlank()) {
                      handler?.proceed(proxyUser, proxyPass)
                    } else {
                      super.onReceivedHttpAuthRequest(view, handler, host, realm)
                    }
                  }

                  override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    isLoading = true
                    currentUrl = url ?: ""
                    canGoBack = view?.canGoBack() ?: false
                    canGoForward = view?.canGoForward() ?: false
                  }

                  override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    isLoading = false
                    currentUrl = url ?: ""
                    canGoBack = view?.canGoBack() ?: false
                    canGoForward = view?.canGoForward() ?: false
                  }

                  private fun handleCustomUri(view: WebView?, urlString: String): Boolean {
                    if (urlString.startsWith("http://") || urlString.startsWith("https://")) {
                      return false
                    }
                    try {
                      val intent = android.content.Intent.parseUri(urlString, android.content.Intent.URI_INTENT_SCHEME)
                      if (intent != null) {
                        val packageManager = context.packageManager
                        val info = packageManager.resolveActivity(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
                        if (info != null) {
                          context.startActivity(intent)
                        } else {
                          val fallbackUrl = intent.getStringExtra("browser_fallback_url")
                          if (fallbackUrl != null) {
                            view?.loadUrl(fallbackUrl)
                          }
                        }
                        return true
                      }
                    } catch (e: Exception) {
                      try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(urlString))
                        context.startActivity(intent)
                        return true
                      } catch (ex: Exception) {
                        ex.printStackTrace()
                      }
                    }
                    return true
                  }

                  override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                    val urlString = request?.url?.toString() ?: return false
                    return handleCustomUri(view, urlString)
                  }

                  @Deprecated("Deprecated in Java")
                  override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) return false
                    return handleCustomUri(view, url)
                  }
                }

                webChromeClient = object : WebChromeClient() {
                  override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    progress = newProgress
                    canGoBack = view?.canGoBack() ?: false
                    canGoForward = view?.canGoForward() ?: false
                  }
                }

                if (MainActivity.savedWebViewBundle != null) {
                  restoreState(MainActivity.savedWebViewBundle!!)
                } else {
                  loadUrl(a.b1())
                }
                webViewInstance = this
                webView = this
                MainActivity.activeWebView = this
              }
            },
            update = { view ->
              webView = view
              MainActivity.activeWebView = view
            },
            onRelease = { view ->
              try {
                view.stopLoading()
                view.destroy()
              } catch (e: Exception) {
                e.printStackTrace()
              }
            },
            modifier = Modifier
              .fillMaxSize()
              .testTag("webview")
          )
        }

        // Floating Animated Circular Music Button on the Left Side of the Screen (User requested round icon on left)
        Box(
          modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 10.dp)
        ) {
          // Radiant pulsing outer glow aura when playing
          if (isMusicPlaying) {
            Box(
              modifier = Modifier
                .size(64.dp)
                .align(Alignment.Center)
                .scale(musicPulseScale)
                .background(
                  brush = Brush.radialGradient(
                    colors = listOf(
                      Color(0xFFFF2A85).copy(alpha = 0.6f),
                      Color(0xFFA855F7).copy(alpha = 0.35f),
                      Color.Transparent
                    )
                  ),
                  shape = CircleShape
                )
            )
          }

          // Main Round Button with Cyber Neon Gradient Border
          Surface(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              showMusicDialog = true
            },
            shape = CircleShape,
            color = if (isMusicPlaying) Color(0xFF1E0720) else Color(0xFF0F1420),
            border = BorderStroke(
              width = if (isMusicPlaying) 2.5.dp else 1.5.dp,
              brush = if (isMusicPlaying) {
                Brush.sweepGradient(
                  listOf(
                    Color(0xFFFF2A85),
                    Color(0xFFA855F7),
                    Color(0xFF00E5FF),
                    Color(0xFFFF2A85)
                  )
                )
              } else {
                Brush.linearGradient(
                  listOf(
                    Color(0xFFF43F5E),
                    Color(0xFF8B5CF6)
                  )
                )
              }
            ),
            shadowElevation = 10.dp,
            modifier = Modifier
              .size(52.dp)
              .testTag("floating_round_music_button")
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier.fillMaxSize()
            ) {
              // Glowing background disc ring
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .background(
                    brush = if (isMusicPlaying) Brush.radialGradient(
                      listOf(Color(0xFF3B0B32), Color(0xFF130718))
                    ) else Brush.radialGradient(
                      listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                    ),
                    shape = CircleShape
                  )
              )

              // Vinyl / Music Note Animated Rotating Icon
              Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Music Player",
                tint = if (isMusicPlaying) Color(0xFFFF2A85) else Color(0xFFFB7185),
                modifier = Modifier
                  .size(26.dp)
                  .then(
                    if (isMusicPlaying) Modifier
                      .rotate(musicIconRotate)
                      .scale(musicPulseScale)
                    else Modifier
                  )
              )

              // Small Active Equalizer / Notification Badge dot on top-right
              if (isMusicPlaying) {
                Box(
                  modifier = Modifier
                    .size(8.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = (-8).dp, y = 8.dp)
                    .background(Color(0xFF00E5FF), CircleShape)
                )
              }
            }
          }
        }
      }
    }
  }

  // Creator Dialog
  if (showCreatorDialog) {
    AlertDialog(
      onDismissRequest = { showCreatorDialog = false },
      containerColor = Color(0xFF070B14),
      titleContentColor = Color.White,
      textContentColor = Color.White,
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = Color(0xFF0F1A30),
              border = BorderStroke(1.dp, Color(0xFF00E5FF)),
              modifier = Modifier.size(28.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text("⚡", fontSize = 14.sp)
              }
            }
            Text(
              text = "FB Tool Auto Creator",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
          }

          IconButton(onClick = { showCreatorDialog = false }, modifier = Modifier.size(28.dp)) {
            Text("✕", color = Color(0xFF64748B), fontWeight = FontWeight.Bold, fontSize = 16.sp)
          }
        }
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Card 1: Number Range
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(22.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Text("#", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                  }
                  Text("Number Range", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                TextButton(
                  onClick = { showLiveConsoleDialog = true },
                  contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                  modifier = Modifier.height(24.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(12.dp))
                    Text("Refresh", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }

              Surface(
                color = Color(0xFF070B14),
                border = BorderStroke(1.dp, Color(0xFF1E293B)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().clickable { showLiveConsoleDialog = true }
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                      color = Color(0xFF1877F2),
                      shape = androidx.compose.foundation.shape.CircleShape,
                      modifier = Modifier.size(18.dp)
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text("f", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                      }
                    }
                    Text(
                      text = if (selectedRange.isNotEmpty()) selectedRange else "রেঞ্জ নির্বাচন করুন...",
                      color = if (selectedRange.isNotEmpty()) Color.White else Color(0xFF64748B),
                      fontWeight = FontWeight.Bold,
                      fontSize = 13.sp,
                      fontFamily = FontFamily.Monospace
                    )
                  }
                  Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color(0xFF64748B))
                }
              }
            }
          }

          // Card 2: Account Name
          var countryExpanded by remember { mutableStateOf(false) }
          val countries = listOf("Bangladesh", "United States", "China", "Madagascar")
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    color = Color(0xFF0284C7),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(22.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                  }
                  Text("Account Name", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Surface(
                  color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                  border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
                  shape = RoundedCornerShape(50.dp),
                  modifier = Modifier.height(22.dp)
                ) {
                  Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text("অটো নেম", color = Color(0xFF00E5FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                }
              }

              ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = !countryExpanded }
              ) {
                OutlinedTextField(
                  value = selectedCountry,
                  onValueChange = { },
                  readOnly = true,
                  label = { Text("Country Style", color = Color(0xFF00E5FF)) },
                  trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                  colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color(0xFF1E293B),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF070B14),
                    unfocusedContainerColor = Color(0xFF070B14)
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                  expanded = countryExpanded,
                  onDismissRequest = { countryExpanded = false },
                  modifier = Modifier.background(Color(0xFF0B1322))
                ) {
                  countries.forEach { selectionOption ->
                    DropdownMenuItem(
                      text = { Text(selectionOption, color = Color.White) },
                      onClick = {
                        selectedCountry = selectionOption
                        prefs.edit().putString("selected_country", selectionOption).apply()
                        countryExpanded = false
                      }
                    )
                  }
                }
              }
            }
          }

          // Card 3: Password
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                  color = Color(0xFF059669),
                  shape = RoundedCornerShape(6.dp),
                  modifier = Modifier.size(22.dp)
                ) {
                  Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                  }
                }
                Text("Password", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              OutlinedTextField(
                value = customPassword,
                onValueChange = { 
                  customPassword = it
                  prefs.edit().putString("saved_password", it).apply()
                },
                placeholder = { Text("Enter password...", color = Color(0xFF64748B)) },
                leadingIcon = { Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF10B981)) },
                trailingIcon = {
                  IconButton(onClick = { 
                    val newPass = "Pass@" + (1000..9999).random().toString()
                    customPassword = newPass
                    prefs.edit().putString("saved_password", newPass).apply()
                  }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Generate", tint = Color(0xFF10B981))
                  }
                },
                colors = OutlinedTextFieldDefaults.colors(
                  focusedBorderColor = Color(0xFF10B981),
                  unfocusedBorderColor = Color(0xFF1E293B),
                  focusedTextColor = Color.White,
                  unfocusedTextColor = Color.White,
                  focusedContainerColor = Color(0xFF070B14),
                  unfocusedContainerColor = Color(0xFF070B14)
                ),
                shape = RoundedCornerShape(10.dp),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
              )
            }
          }

          // Card 4: Proxy
          Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B1322)),
            border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.7f)),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  Surface(
                    color = Color(0xFF7C3AED),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.size(22.dp)
                  ) {
                    Box(contentAlignment = Alignment.Center) {
                      Icon(Icons.Default.Security, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                  }
                  Text("Proxy", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                TextButton(
                  onClick = { showProxyConfigDialog = true },
                  contentPadding = PaddingValues(0.dp),
                  modifier = Modifier.height(24.dp)
                ) {
                  Text("Set Proxy", color = Color(0xFFA855F7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                  color = if (!isProxyEnabled) Color(0xFF00E5FF) else Color(0xFF070B14),
                  border = BorderStroke(1.dp, if (!isProxyEnabled) Color(0xFF00E5FF) else Color(0xFF1E293B)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).clickable {
                    isProxyEnabled = false
                    prefs.edit().putBoolean("is_proxy_enabled", false).apply()
                    applyWebViewProxy(context, false, "", "")
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text(
                      "⚡ Direct Mode",
                      color = if (!isProxyEnabled) Color(0xFF070B14) else Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    )
                  }
                }

                Surface(
                  color = if (isProxyEnabled) Color(0xFFA855F7) else Color(0xFF070B14),
                  border = BorderStroke(1.dp, if (isProxyEnabled) Color(0xFFA855F7) else Color(0xFF1E293B)),
                  shape = RoundedCornerShape(8.dp),
                  modifier = Modifier.weight(1f).clickable {
                    isProxyEnabled = true
                    prefs.edit().putBoolean("is_proxy_enabled", true).apply()
                    applyWebViewProxy(context, true, proxyHost, proxyPort)
                  }
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                  ) {
                    Text(
                      "🌐 Worker Proxy",
                      color = Color.White,
                      fontWeight = FontWeight.Bold,
                      fontSize = 11.sp
                    )
                  }
                }
              }
            }
          }

          // Button 1: OTP CONFIRM (Cyan to Blue Gradient)
          Surface(
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .background(
                Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF0088FF))),
                shape = RoundedCornerShape(12.dp)
              )
              .clickable {
                if (lastCreatedOtp.isNotEmpty() && lastCreatedOtp != "Expired" && lastCreatedOtp != "number expired") {
                  val sysClipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  val clip = ClipData.newPlainText("OTP", lastCreatedOtp)
                  sysClipboard.setPrimaryClip(clip)
                  clipboardManager.setText(AnnotatedString(lastCreatedOtp))
                  Toast.makeText(context, "OTP: $lastCreatedOtp কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "কোনো নতুন ওটিপি আসেনি", Toast.LENGTH_SHORT).show()
                }
              },
            color = Color.Transparent,
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(Icons.Default.Key, contentDescription = null, tint = Color.Black, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text(
                text = if (lastCreatedOtp.isNotEmpty() && lastCreatedOtp != "Expired" && lastCreatedOtp != "number expired") "OTP: $lastCreatedOtp (কপি করুন)" else "ওটিপি কোড (OTP)",
                color = Color.Black,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp
              )
            }
          }

          // Button 2: Create Account Button (Cyan to Purple to Pink Gradient)
          Button(
            onClick = {
                if (selectedRange.isEmpty()) {
                  currentCreationStatus = "দয়া করে রেঞ্জ নির্বাচন করুন"
                  Toast.makeText(context, "দয়া করে রেঞ্জ নির্বাচন করুন", Toast.LENGTH_SHORT).show()
                  showLiveConsoleDialog = true
                  return@Button
                }
                isCreatingAccount = true
                
                currentCreationStatus = "account creating..."
                lastCreatedPhone = ""
                lastCreatedUid = ""
                lastCreatedCookies = ""
                lastCreatedOtp = ""

                fetchNumber(
                  rangeCode = selectedRange,
                  onSuccess = { phoneNumber ->
                    scope.launch {
                      lastCreatedPhone = phoneNumber
                      createFacebookAccount(
                        context = context,
                        phone = phoneNumber,
                        passwordInput = customPassword,
                        country = selectedCountry,
                        onSuccess = { uid, name, cookies ->
                          scope.launch {
                            lastCreatedUid = uid
                            lastCreatedCookies = cookies
                            currentCreationStatus = "create success"
                            saveAccountToHistory(context, phoneNumber, uid, cookies, customPassword, "")
                            lastSuccessCookies = cookies
                            prefs.edit().putString("last_success_cookies", cookies).apply()
                            activePhoneChecking = phoneNumber
                            isCreatingAccount = false

                            if (cookies.isNotEmpty()) {
                              try {
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.removeAllCookies(null)
                                
                                val domains = listOf(
                                  "https://.facebook.com",
                                  "https://facebook.com",
                                  "https://m.facebook.com",
                                  "https://limited.facebook.com"
                                )
                                
                                val trimmed = cookies.trim()
                                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                  val jsonArray = org.json.JSONArray(trimmed)
                                  for (i in 0 until jsonArray.length()) {
                                    val obj = jsonArray.getJSONObject(i)
                                    val cookieName = obj.optString("name")
                                    val cookieValue = obj.optString("value")
                                    val domain = obj.optString("domain", ".facebook.com")
                                    val path = obj.optString("path", "/")
                                    if (cookieName.isNotEmpty()) {
                                      val cookieString = "$cookieName=$cookieValue; Domain=$domain; Path=$path"
                                      domains.forEach { d ->
                                        cookieManager.setCookie(d, cookieString)
                                      }
                                    }
                                  }
                                } else {
                                  val parts = trimmed.split(";")
                                  for (part in parts) {
                                    val cleanPart = part.trim()
                                    if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                                      domains.forEach { d ->
                                        cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/")
                                      }
                                    }
                                  }
                                }
                                cookieManager.flush()
                                if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
                                  applyWebViewProxy(context, true, proxyHost, proxyPort)
                                }
                                webView?.loadUrl(a.b1())
                                snackbarHostState.showSnackbar("অ্যাকাউন্ট তৈরি সফল! অটোমেটিক লগইন করা হচ্ছে...")
                              } catch (e: Exception) {
                                e.printStackTrace()
                              }
                            }
                          }
                        },
                        onFailure = { errorMsg ->
                          scope.launch {
                            currentCreationStatus = "create failed"
                            saveAccountToHistory(context, phoneNumber, "N/A", "N/A", customPassword, "তৈরি ব্যর্থ")
                            isCreatingAccount = false
                          }
                        }
                      )
                    }
                  },
                  onFailure = { errorMsg ->
                    scope.launch {
                      currentCreationStatus = "create failed"
                      isCreatingAccount = false
                    }
                  }
                )
              },
              enabled = !isCreatingAccount,
              modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(
                  Brush.horizontalGradient(listOf(Color(0xFF00E5FF), Color(0xFF8B5CF6), Color(0xFFEC4899))),
                  shape = RoundedCornerShape(12.dp)
                ),
              colors = ButtonDefaults.buttonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(12.dp)
            ) {
              if (isCreatingAccount) {
                CircularProgressIndicator(
                  color = Color.White,
                  modifier = Modifier.size(20.dp),
                  strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(10.dp))
              }
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.White)
                Spacer(Modifier.width(8.dp))
                Text(
                  text = if (isCreatingAccount) "অ্যাকাউন্ট তৈরি হচ্ছে..." else "অ্যাকাউন্ট তৈরি করুন",
                  fontWeight = FontWeight.Bold,
                  fontSize = 15.sp,
                  color = Color.White
                )
              }
            }

            Spacer(Modifier.height(10.dp))

          // Creation Status / Logs
          if (currentCreationStatus.isNotEmpty()) {
            Surface(
              color = Color(0xFF0B1322),
              border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = currentCreationStatus,
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (currentCreationStatus == "create success") Color(0xFF10B981) else Color(0xFF00E5FF)
                )
              }
            }
          }

          // OTP received block
          if (lastCreatedPhone.isNotEmpty()) {
            Surface(
              color = Color(0xFF0B1322),
              border = BorderStroke(1.dp, if (lastCreatedOtp.isNotEmpty() && lastCreatedOtp != "Expired" && lastCreatedOtp != "number expired") Color(0xFF10B981) else Color(0xFF00E5FF)),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                if (lastCreatedOtp.isNotEmpty()) {
                  val isExpired = lastCreatedOtp == "Expired" || lastCreatedOtp == "number expired"
                  Text(
                    text = if (isExpired) "number expired" else "ওটিপি: $lastCreatedOtp",
                    style = MaterialTheme.typography.titleLarge.copy(
                      fontWeight = FontWeight.Bold,
                      color = if (isExpired) Color(0xFFEF4444) else Color(0xFF00E5FF)
                    )
                  )
                } else {
                  CircularProgressIndicator(color = Color(0xFF00E5FF), modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                }
              }
            }
          }

          // Display 3 copy buttons (only after successful account creation)
          if (lastCreatedUid.isNotEmpty() && lastCreatedUid != "N/A" && lastCreatedCookies.isNotEmpty() && lastCreatedCookies != "N/A") {
            Row(
              modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedPhone))
                  scope.launch { snackbarHostState.showSnackbar("নাম্বার কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("নাম্বার কপি", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedUid))
                  scope.launch { snackbarHostState.showSnackbar("UID কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C3AED)),
                modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("UID কপি", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }

              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(lastCreatedCookies))
                  scope.launch { snackbarHostState.showSnackbar("কুকিজ কপি করা হয়েছে!") }
                },
                contentPadding = PaddingValues(horizontal = 6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                modifier = Modifier.weight(1f).height(36.dp),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("কুকিজ কপি", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showCreatorDialog = false }) {
          Text("বন্ধ করুন")
        }
      }
    )
  }

  // Set Range Dialog
  if (showSetRangeDialog) {
    AlertDialog(
      onDismissRequest = { showSetRangeDialog = false },
      title = { Text("রেঞ্জ সেট করুন") },
      text = {
        OutlinedTextField(
          value = selectedRange,
          onValueChange = { 
            selectedRange = it
            prefs.edit().putString("saved_range", it).apply()
          },
          label = { Text("রেঞ্জ লিখুন (যেমন: 22508XXXX)") },
          modifier = Modifier.fillMaxWidth(),
          singleLine = true
        )
      },
      confirmButton = {
        Button(onClick = { 
          prefs.edit().putString("saved_range", selectedRange).apply()
          showSetRangeDialog = false 
        }) {
          Text("সেভ করুন")
        }
      }
    )
  }

  if (showCookieLoginDialog) {
    AlertDialog(
      onDismissRequest = { showCookieLoginDialog = false },
      title = { Text("Login with Cookies") },
      text = {
        OutlinedTextField(
          value = cookieLoginInput,
          onValueChange = { cookieLoginInput = it },
          label = { Text("Paste Cookies Here") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 3
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (cookieLoginInput.isNotEmpty()) {
              lastSuccessCookies = cookieLoginInput
              prefs.edit().putString("last_success_cookies", cookieLoginInput).apply()
              val cookieManager = CookieManager.getInstance()
              cookieManager.setAcceptCookie(true)
              cookieManager.removeAllCookies(null)
              
              val domains = listOf(
                "https://.facebook.com",
                "https://facebook.com",
                "https://m.facebook.com",
                "https://limited.facebook.com"
              )
              
              val trimmed = cookieLoginInput.trim()
              if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                try {
                  val jsonArray = org.json.JSONArray(trimmed)
                  for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val name = obj.optString("name")
                    val value = obj.optString("value")
                    val domain = obj.optString("domain", ".facebook.com")
                    val path = obj.optString("path", "/")
                    if (name.isNotEmpty()) {
                      val cookieString = "$name=$value; Domain=$domain; Path=$path"
                      domains.forEach { d ->
                        cookieManager.setCookie(d, cookieString)
                      }
                    }
                  }
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              } else {
                val parts = trimmed.split(";")
                for (part in parts) {
                  val cleanPart = part.trim()
                  if (cleanPart.isNotEmpty() && cleanPart.contains("=")) {
                    domains.forEach { d ->
                      cookieManager.setCookie(d, "$cleanPart; Domain=.facebook.com; Path=/")
                    }
                  }
                }
              }
              cookieManager.flush()
              webView?.loadUrl(a.b1())
              scope.launch {
                snackbarHostState.showSnackbar("Cookie set and reloading!")
              }
            }
            showCookieLoginDialog = false
            cookieLoginInput = ""
          }
        ) {
          Text("Login")
        }
      },
      dismissButton = {
        TextButton(onClick = { showCookieLoginDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // History Dialog
  if (showHistoryDialog) {
    val historyItems = remember(showHistoryDialog) { mutableStateOf(getHistory(context)) }

    AlertDialog(
      onDismissRequest = { showHistoryDialog = false },
      title = {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "ইতিহাস",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              maxLines = 1
            )
          }
          
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Copy button - copies only accounts with valid OTP in phone\totp format line by line
            Button(
              onClick = {
                val validOtpItems = historyItems.value.filter { 
                  isValidOtpCode(it.otp)
                }
                val textToCopy = validOtpItems.joinToString("\n") { 
                  val cleanPhone = it.phone.replace("+", "").replace(" ", "").trim()
                  "$cleanPhone\t${it.otp.trim()}" 
                }
                if (textToCopy.isNotEmpty()) {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                  val clip = android.content.ClipData.newPlainText("Copied OTPs", textToCopy)
                  clipboard.setPrimaryClip(clip)
                  clipboardManager.setText(AnnotatedString(textToCopy))
                  Toast.makeText(context, "${validOtpItems.size} টি সফল ওটিপি কপি করা হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "কপি করার মতো কোনো সফল ওটিপি পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF10B981), // Emerald/Green
                contentColor = Color.White
              ),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = "Copy OTP",
                  modifier = Modifier.size(12.dp)
                )
                Text(
                  text = "Copy",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            // All Clear button
            Button(
              onClick = {
                if (historyItems.value.isNotEmpty()) {
                  clearHistory(context)
                  historyItems.value = emptyList()
                  Toast.makeText(context, "সকল ইতিহাস মুছে ফেলা হয়েছে!", Toast.LENGTH_SHORT).show()
                } else {
                  Toast.makeText(context, "ইতিহাস খালি রয়েছে", Toast.LENGTH_SHORT).show()
                }
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
              ),
              shape = RoundedCornerShape(6.dp),
              contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
              modifier = Modifier.height(30.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteSweep,
                  contentDescription = "Clear All",
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "Clear All",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      },
      text = {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Total OTP display - persistently reflects strictly valid 5, 6, or 8 digit OTPs in history
          val totalOtpCount = historyItems.value.count {
            isValidOtpCode(it.otp)
          }
          Card(
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.padding(12.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Total otp $totalOtpCount",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
              )
            }
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 330.dp)
          ) {
          if (historyItems.value.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize(),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "কোনো অ্যাকাউন্ট বা ওটিপি রেকর্ড পাওয়া যায়নি।",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          } else {
            LazyColumn(
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              items(historyItems.value) { item ->
                Card(
                  colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                  ),
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      val formattedTime = try {
                        val sdf = SimpleDateFormat("dd MMM hh:mm a", Locale.getDefault())
                        sdf.format(Date(item.timestamp))
                      } catch (e: Exception) {
                        "N/A"
                      }
                      Text(
                        text = "সময়: $formattedTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                      )

                      // Single item delete button
                      IconButton(
                        onClick = {
                          deleteItemFromHistory(context, item)
                          historyItems.value = getHistory(context)
                          Toast.makeText(context, "আইটেমটি ডিলিট করা হয়েছে", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(24.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Delete,
                          contentDescription = "Delete item",
                          tint = MaterialTheme.colorScheme.error,
                          modifier = Modifier.size(16.dp)
                        )
                      }
                    }

                    Text(
                      text = "নাম্বার: " + item.phone,
                      style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                    )

                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                      Text(
                        text = "ওটিপি কোড: ",
                        style = MaterialTheme.typography.bodyMedium
                      )
                      if (item.otp.isNotEmpty()) {
                        val isExpired = item.otp == "Expired" || item.otp == "number expired"
                        Text(
                          text = if (isExpired) "number expired" else item.otp,
                          style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                          )
                        )
                        if (!isExpired) {
                          IconButton(
                            onClick = {
                              clipboardManager.setText(AnnotatedString(item.otp))
                              scope.launch { snackbarHostState.showSnackbar("OTP কপি করা হয়েছে!") }
                            },
                            modifier = Modifier.size(24.dp)
                          ) {
                            Icon(
                              imageVector = Icons.Default.ContentCopy,
                              contentDescription = "OTP কপি করুন",
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      } else {
                        Row(
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                          CircularProgressIndicator(modifier = Modifier.size(10.dp), strokeWidth = 1.dp)
                          Text(
                            text = "অপেক্ষারত / পাওয়া যায়নি",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                          )
                        }
                      }
                    }

                    // Copy action row
                    Row(
                      modifier = Modifier.fillMaxWidth(),
                      horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.phone))
                          scope.launch { snackbarHostState.showSnackbar("নাম্বার কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("নাম্বার কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }

                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.uid))
                          scope.launch { snackbarHostState.showSnackbar("UID কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("UID কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }

                      Button(
                        onClick = {
                          clipboardManager.setText(AnnotatedString(item.cookies))
                          scope.launch { snackbarHostState.showSnackbar("কুকি কপি করা হয়েছে!") }
                        },
                        contentPadding = PaddingValues(horizontal = 4.dp),
                        modifier = Modifier.weight(1f).height(30.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        shape = RoundedCornerShape(4.dp)
                      ) {
                        Text("কুকি কপি", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                      }
                    }
                  }
                }
              }
            }
          }
        }
        }
      },
      confirmButton = {
        TextButton(onClick = { showHistoryDialog = false }) {
          Text("বন্ধ করুন")
        }
      }
    )
  }

  // Proxy Config Dialog
  if (showProxyConfigDialog) {
    var hostInput by remember { mutableStateOf(proxyHost) }
    var portInput by remember { mutableStateOf(proxyPort) }
    var userInput by remember { mutableStateOf(proxyUser) }
    var passInput by remember { mutableStateOf(proxyPass) }
    AlertDialog(
      onDismissRequest = { showProxyConfigDialog = false },
      title = { Text("Proxy Settings") },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedTextField(
            value = hostInput,
            onValueChange = { hostInput = it },
            label = { Text("Server Change/Host") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = portInput,
            onValueChange = { portInput = it },
            label = { Text("Port") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = userInput,
            onValueChange = { userInput = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )

          OutlinedTextField(
            value = passInput,
            onValueChange = { passInput = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            proxyHost = hostInput.trim()
            proxyPort = portInput.trim()
            proxyUser = userInput.trim()
            proxyPass = passInput.trim()

            prefs.edit()
              .putString("proxy_host", proxyHost)
              .putString("proxy_port", proxyPort)
              .putString("proxy_user", proxyUser)
              .putString("proxy_pass", proxyPass)
              .apply()

            // Apply to WebView if enabled
            if (isProxyEnabled && proxyHost.isNotBlank() && proxyPort.isNotBlank()) {
              applyWebViewProxy(context, true, proxyHost, proxyPort)
            }

            showProxyConfigDialog = false
            scope.launch { snackbarHostState.showSnackbar("Proxy settings saved successfully!") }
          }
        ) {
          Text("সংরক্ষণ করুন (Save)")
        }
      },
      dismissButton = {
        TextButton(onClick = { showProxyConfigDialog = false }) {
          Text("বাতিল (Cancel)")
        }
      }
    )
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
  MyApplicationTheme {
    MainScreen()
  }
}

fun getRandomMobileUserAgent(): String {
    val androidVersions = listOf("10", "11", "12", "13", "14")
    val deviceModels = listOf(
        "SM-G998B", "SM-G991B", "SM-S901B", "SM-S908B", "SM-S918B", "SM-A546B", "SM-A536B", 
        "Pixel 6", "Pixel 6 Pro", "Pixel 7", "Pixel 7 Pro", "Pixel 8", "Pixel 8 Pro", 
        "2201117TY", "2201116PG", "2210132G", "23049PCD8G", 
        "CPH2305", "CPH2371", "CPH2437", 
        "V2130", "V2145", "V2227A"
    )
    val chromeVersions = listOf(
        "114.0.0.0", "115.0.0.0", "116.0.0.0", "117.0.0.0", 
        "118.0.0.0", "119.0.0.0", "120.0.0.0", "121.0.0.0", "122.0.0.0", "123.0.0.0"
    )
    
    val androidVersion = androidVersions.random()
    val deviceModel = deviceModels.random()
    val chromeVersion = chromeVersions.random()
    
    return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
}
