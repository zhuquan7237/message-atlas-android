package com.messageatlas.app.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateClientTest {
    @Test
    fun `detects a newer patch version`() {
        assertTrue(UpdateClient.isRemoteNewer("v0.3.2", "0.3.1"))
    }

    @Test
    fun `detects a newer major version`() {
        assertTrue(UpdateClient.isRemoteNewer("2.0.0", "1.99.99"))
    }

    @Test
    fun `treats missing version segments as zero`() {
        assertFalse(UpdateClient.isRemoteNewer("1.2", "1.2.0"))
        assertTrue(UpdateClient.isRemoteNewer("1.2.1", "1.2"))
    }

    @Test
    fun `ignores prerelease suffix for update comparison`() {
        assertFalse(UpdateClient.isRemoteNewer("v1.2.0-beta", "1.2.0"))
        assertTrue(UpdateClient.isRemoteNewer("v1.3.0-rc1", "1.2.9"))
    }

    @Test
    fun `does not downgrade to older remote version`() {
        assertFalse(UpdateClient.isRemoteNewer("0.3.0", "0.3.1"))
    }
}

