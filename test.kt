suspend fun fetchConversationsMetadata(): Map<String, Pair<String, String>> {
    return try {
        val auth = FirebaseAuth.getInstance()
        val token = auth.currentUser?.getIdToken(false)?.await()?.token ?: return emptyMap()
        val client = OkHttpClient()
        val url = "https://my-original-apk-default-rtdb.firebaseio.com/support_metadata.json?auth=$token"
        val request = Request.Builder().url(url).build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val body = response.body?.string() ?: ""
        response.close()
        
        val map = mutableMapOf<String, Pair<String, String>>()
        if (body.isNotEmpty() && body != "null") {
            val json = JSONObject(body)
            val keys = json.keys()
            while (keys.hasNext()) {
                val uid = keys.next()
                val obj = json.optJSONObject(uid)
                if (obj != null) {
                    val dName = obj.optString("displayName", "User")
                    val adminEmail = obj.optString("adminEmail", "")
                    map[uid] = Pair(dName, adminEmail)
                }
            }
        }
        map
    } catch (e: Exception) {
        emptyMap()
    }
}
