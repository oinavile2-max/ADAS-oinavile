package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class NativeUsbPolicyTest {
    @Test fun videoAndVendorInterfacesAreCandidates(){assertTrue(NativeUsbPolicy.cameraCandidate(0,listOf(14)));assertTrue(NativeUsbPolicy.cameraCandidate(0,listOf(255)));assertTrue(NativeUsbPolicy.cameraCandidate(14,emptyList()))}
    @Test fun storageKeyboardAndHubAreNotCameraCandidates(){assertFalse(NativeUsbPolicy.cameraCandidate(0,listOf(8)));assertFalse(NativeUsbPolicy.cameraCandidate(3,listOf(3)));assertFalse(NativeUsbPolicy.cameraCandidate(9,listOf(9)))}
}
