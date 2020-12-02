package main.kotlin.db

import at.favre.lib.crypto.bcrypt.BCrypt
import java.security.MessageDigest

object Security {

    /**
     * @param string used as plaion password
     * @return encrypted string (password hash)
     */
    fun encrypt(string: String) : String = this.hashString("SHA-512", string)

    /**
     * the hash uses random salt
     * @param string used as plaion password
     * @return encrypted string (password hash)
     */
    fun encryptRandom(string: String) : String = BCrypt.withDefaults().hashToString(12, string.toCharArray())

    /**
     * Supported algorithms:
     *
     * Algorithm	Supported API Levels
     * MD5          1+
     * SHA-1	    1+
     * SHA-224	    1-8,22+
     * SHA-256	    1+
     * SHA-384	    1+
     * SHA-512	    1+
     */
    fun hashString(type: String, input: String): String {
        val HEX_CHARS = "0123456789ABCDEF"
        val bytes = MessageDigest
                .getInstance(type)
                .digest(input.toByteArray())
        val result = StringBuilder(bytes.size * 2)

        bytes.forEach {
            val i = it.toInt()
            result.append(HEX_CHARS[i shr 4 and 0x0f])
            result.append(HEX_CHARS[i and 0x0f])
        }

        return result.toString()
    }
}