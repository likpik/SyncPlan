package com.example.syncplan

import java.util.regex.Pattern

object EmailValidator {
    private val EMAIL_PATTERN = Pattern.compile(
        "[a-zA-Z0-9+._%\\-]{1,256}@[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}(\\.[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25})+"
    )

    fun isValid(email: String): Boolean {
        return EMAIL_PATTERN.matcher(email).matches()
    }
}