package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class AutoRecordingGateTest {
    @Test fun authorizationOrConnectionWithoutImageNeverStartsRecording(){
        val gate=AutoRecordingGate();gate.opened()
        assertNull(gate.claim(false,true,false));assertNull(gate.claim(true,false,false))
        assertNull(gate.claim(true,true,true));assertNotNull(gate.claim(true,true,false))
        assertNull(gate.claim(true,true,false))
    }
    @Test fun manualStopInvalidatesQueuedAutomaticStartAndPreventsRestart(){
        val gate=AutoRecordingGate();gate.opened()
        val queued=gate.claim(true,true,false)!!;gate.manual()
        assertFalse(gate.valid(queued))
        repeat(100){assertNull(gate.claim(true,true,false))}
    }
    @Test fun explicitNewConnectionRearmsAndInvalidatesOldCommands(){
        val gate=AutoRecordingGate();gate.opened()
        val old=gate.claim(true,true,false)!!
        gate.opened();assertFalse(gate.valid(old))
        val current=gate.claim(true,true,false)!!;assertTrue(gate.valid(current))
        gate.manual();assertFalse(gate.valid(current))
    }
}
