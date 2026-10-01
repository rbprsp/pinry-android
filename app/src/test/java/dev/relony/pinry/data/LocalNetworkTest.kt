package dev.relony.pinry.data

import dev.relony.pinry.data.net.mayBeLocal
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalNetworkTest {
    @Test
    fun privateLinkLocalAndUniqueLocalAddressesMayBeLocal() {
        val local = listOf("192.168.1.16", "10.0.2.2", "172.16.0.1", "172.31.255.254", "169.254.1.1", "fe80::1", "fd12:3456::1", "fc00::1")
        val public = listOf("8.8.8.8", "172.32.0.1", "100.64.0.1", "127.0.0.1", "2001:db8::1", "::1")
        assertEquals(local, local.filter { InetAddress.getByName(it).mayBeLocal() })
        assertEquals(emptyList<String>(), public.filter { InetAddress.getByName(it).mayBeLocal() })
    }
}
