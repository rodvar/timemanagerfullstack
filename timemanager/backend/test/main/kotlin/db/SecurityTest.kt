package main.kotlin.db

import kotlin.test.Test
import kotlin.test.assertEquals

class SecurityTest {

    @Test
    fun hashEquals() {
        assertEquals(Security.encrypt("pepe"), Security.encrypt("pepe"))
    }
}