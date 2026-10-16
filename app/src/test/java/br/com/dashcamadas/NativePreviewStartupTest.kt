package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test

class NativePreviewStartupTest {
    private class Driver:NativePreviewStartup.Driver {
        val calls=mutableListOf<String>()
        var initialized=false;var connected=false;var result=0
        private var previewExists=false
        override fun connect(){calls+="connect";previewExists=true}
        override fun initialized()=initialized
        override fun connected()=connected
        override fun attachPreview(){check(previewExists);calls+="attach"}
        override fun startPreview():Int {check(calls.last()=="attach");calls+="start";return result}
    }
    @Test fun deferredConnectionAttachesOnlyAfterInitializationAndStartsOnce(){
        val d=Driver();val startup=NativePreviewStartup(d)
        startup.begin(1000)
        assertFalse(startup.poll(1000));assertEquals(listOf("connect"),d.calls)
        d.connected=true;assertFalse(startup.poll(1250))
        d.initialized=true;assertTrue(startup.poll(1500));assertTrue(startup.poll(1750))
        assertEquals(listOf("connect","attach","start"),d.calls)
        assertEquals(NativePreviewStartup.Phase.READY,startup.phase)
    }
    @Test fun initializedWithoutConnectionCannotAttachOrStart(){
        val d=Driver().apply{initialized=true};val startup=NativePreviewStartup(d)
        startup.begin(0);assertFalse(startup.poll(4999))
        val error=assertThrows(IllegalStateException::class.java){startup.poll(5000)}
        assertTrue(error.message!!.contains("conectado=false, inicializado=true"))
        assertEquals(listOf("connect"),d.calls)
        assertEquals(NativePreviewStartup.Phase.FAILED,startup.phase)
        assertFalse(startup.poll(5001))
    }
    @Test fun closeCancelsPendingAttachmentAndLatePoll(){
        val d=Driver();val startup=NativePreviewStartup(d)
        startup.begin(0);startup.close();d.connected=true;d.initialized=true
        assertFalse(startup.poll(100));assertEquals(listOf("connect"),d.calls)
    }
    @Test fun rejectedStartDoesNotBecomeReadyOrRetry(){
        val d=Driver().apply{connected=true;initialized=true;result=-1}
        val startup=NativePreviewStartup(d);startup.begin(0)
        assertThrows(IllegalStateException::class.java){startup.poll(1)}
        assertFalse(startup.poll(2));assertEquals(listOf("connect","attach","start"),d.calls)
        assertEquals(NativePreviewStartup.Phase.FAILED,startup.phase)
    }
}
