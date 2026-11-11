package org.slf4j

import android.util.Base64
import kotlin.system.exitProcess

object z {
    // Base64 encoded owner email
    private const val E_B64 = "c2l5YW1taWEzMjBAZ21haWwuY29t"
    
    fun getAdmin(): String {
        return try {
            val decoded = String(Base64.decode(E_B64, Base64.DEFAULT)).trim()
            // Anti-tamper check: Verify length and domain.
            // If someone changes the base64 to their own email but length doesn't match 21, it will crash.
            if (decoded.length != 21 || !decoded.endsWith("@gmail.com")) {
                exitProcess(1) // Crash the app if tampered
            }
            decoded
        } catch (e: Exception) {
            exitProcess(1) // Crash if decoding fails
        }
    }
}
