package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class NativeUsbPolicyTest {
    @Test fun videoAndVendorInterfacesAreCandidates(){assertTrue(NativeUsbPolicy.cameraCandidate(0,listOf(14)));assertTrue(NativeUsbPolicy.cameraCandidate(0,listOf(255)));assertTrue(NativeUsbPolicy.cameraCandidate(14,emptyList()))}
    @Test fun storageKeyboardAndHubAreNotCameraCandidates(){assertFalse(NativeUsbPolicy.cameraCandidate(0,listOf(8)));assertFalse(NativeUsbPolicy.cameraCandidate(3,listOf(3)));assertFalse(NativeUsbPolicy.cameraCandidate(9,listOf(9)))}
    @Test fun reportedNwdDvrIsOfferedDespiteStorageInterface(){
        assertTrue(NativeUsbPolicy.cameraCandidate(0,listOf(8),0x3322,0x6655,0))
        assertTrue(NativeUsbPolicy.referenceDvr(0x3322,0x6655,0,0))
    }
    @Test fun otherUsbInHardwareReportRemainsExcluded(){
        assertFalse(NativeUsbPolicy.cameraCandidate(0,listOf(8),0x058f,0x6387,0))
        assertFalse(NativeUsbPolicy.referenceDvr(0x058f,0x6387,0,0))
    }
    @Test fun referenceFiltersRequireAllFourDescriptors(){
        val filters=listOf(Triple(0x3322,0x6655,0),Triple(0x2298,0x6698,0),Triple(0x0098,0x1198,0),
            Triple(0x0198,0x1298,0),Triple(0x0eff,0x0001,8),Triple(0x1b3f,0x8301,0))
        filters.forEach{(vid,pid,type)->
            assertTrue(NativeUsbPolicy.cameraCandidate(type,listOf(8),vid,pid,0))
            assertFalse(NativeUsbPolicy.referenceDvr(vid,pid+1,type,0))
            assertFalse(NativeUsbPolicy.referenceDvr(vid,pid,type,1))
            assertFalse(NativeUsbPolicy.referenceDvr(vid,pid,3,0))
        }
    }
}
