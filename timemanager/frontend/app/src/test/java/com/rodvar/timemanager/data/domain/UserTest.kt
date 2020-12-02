package com.rodvar.timemanager.data.domain

import org.junit.Assert
import org.junit.Before
import org.junit.Test

class UserTest {

    lateinit var user: User

    @Before
    fun setUp() {
        this.user = User(email = "test@test.com")
    }

    @Test
    fun testAdmin() {
        this.user.role = Roles.ADMIN
        Assert.assertTrue(this.user.isAdmin())
        Assert.assertFalse(this.user.isManager())
        Assert.assertTrue(this.user.isUserAdmin())
    }

    @Test
    fun testManager() {
        this.user.role = Roles.MANAGER
        Assert.assertFalse(this.user.isAdmin())
        Assert.assertTrue(this.user.isManager())
        Assert.assertTrue(this.user.isUserAdmin())
    }

    @Test
    fun testRegular() {
        Assert.assertFalse(this.user.isAdmin())
        Assert.assertFalse(this.user.isManager())
        Assert.assertFalse(this.user.isUserAdmin())
    }
}