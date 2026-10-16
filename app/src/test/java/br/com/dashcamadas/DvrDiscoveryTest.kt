package br.com.dashcamadas

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File

class DvrDiscoveryTest {
    @Test fun unrelatedFilesCannotHideKnownMarkerNames(){
        val root=Files.createTempDirectory("dvr-full").toFile()
        try{
            repeat(160){File(root,"recording-$it.mp4").writeText("video")}
            val expected=mutableSetOf<File>()
            // More than 64 distinct recognized names, independent of filesystem enumeration order.
            for(name in listOf("akfile","kkfile"))repeat(64){mask->
                val variant=name.mapIndexed{index,c->if(mask and (1 shl index)!=0)c.uppercaseChar() else c}.joinToString("")
                expected+=File(root,variant).apply{writeText("endpoint")}.canonicalFile
            }
            assertEquals(expected,DvrMarkerSearch.find(root,"br.com.dashcamadas").toSet())
        }finally{root.deleteRecursively()}
    }
    @Test fun vendorMountsHavePublicAliasesAndPrimaryOrSystemMountsAreExcluded(){
        val entries="""
            /dev/block/vold/public:8,1 /mnt/media_rw/DVR vfat rw 0 0
            /dev/block/sda1 /mnt/usbhost1 vfat rw 0 0
            fuse /storage/emulated/0 fuse rw 0 0
            /dev/block/data /data ext4 rw 0 0
            /dev/block/sda2 /storage/USB\040DISK vfat rw 0 0
            /dev/block/sda3 /storage/../data vfat rw 0 0
        """.trimIndent()
        assertEquals(listOf("/storage/DVR","/mnt/media_rw/DVR","/mnt/usbhost1","/storage/USB DISK"),DvrMountPaths.parse(entries))
    }
    @Test fun mountedMarkersAreFoundInRootOriginalAppDirectoryAndImmediateChild(){
        val root=Files.createTempDirectory("dvr-root").toFile()
        try{
            val expected=listOf(File(root,"AKFILE"),File(root,"Android/data/com.ankai.cardvr/kkfile"),File(root,"DVR/jrfile"))
            expected.forEach{requireNotNull(it.parentFile).mkdirs();it.writeBytes(byteArrayOf(1))}
            File(root,"elene").writeBytes(byteArrayOf())
            File(root,"video.mp4").writeText("not a marker")
            assertEquals(expected.map{it.canonicalFile}.toSet(),DvrMarkerSearch.find(root,"br.com.dashcamadas").toSet())
            assertEquals(5,root.walkTopDown().filter{it.isFile}.count())
        }finally{root.deleteRecursively()}
    }
    @Test fun nestedVolumeContainersAlsoCheckOriginalAndCurrentAppDirectories(){
        val root=Files.createTempDirectory("dvr-container").toFile()
        try{
            repeat(100){File(root,"clip-$it.mp4").writeText("video")}
            val expected=listOf(File(root,"disk/Android/data/com.ankai.cardvr/akfile"),
                File(root,"disk/Android/data/br.com.dashcamadas/jrfile"))
            expected.forEach{requireNotNull(it.parentFile).mkdirs();it.writeText("endpoint")}
            assertEquals(expected.map{it.canonicalFile}.toSet(),DvrMarkerSearch.find(root,"br.com.dashcamadas").toSet())
        }finally{root.deleteRecursively()}
    }
    @Test fun discoveryDoesNotFollowAMarkerOrDirectoryLinkOutsideRemovableVolume(){
        val root=Files.createTempDirectory("dvr-volume").toFile();val other=Files.createTempDirectory("outside-volume").toFile()
        try{
            val file=File(other,"akfile").apply{writeText("external")}
            Files.createSymbolicLink(File(root,"akfile").toPath(),file.toPath())
            Files.createSymbolicLink(File(root,"camera").toPath(),other.toPath())
            assertTrue(DvrMarkerSearch.find(root,"br.com.dashcamadas").isEmpty())
            assertFalse(DvrMarkerSearch.inside(file,root))
        }finally{File(root,"camera").delete();File(root,"akfile").delete();root.deleteRecursively();other.deleteRecursively()}
    }
    @Test fun explicitSearchModesPreservePriorityAndExcludeOtherTransport(){
        val usb=listOf("usb:a");val file=listOf("dvrfile:/storage/x/akfile")
        assertEquals(usb+file,DvrSearchMode.USB_FIRST.order(usb,file))
        assertEquals(file+usb,DvrSearchMode.VOLUME_FIRST.order(usb,file))
        assertEquals(usb,DvrSearchMode.USB_ONLY.order(usb,file))
        assertEquals(file,DvrSearchMode.VOLUME_ONLY.order(usb,file))
        assertTrue(DvrSources.native(file.single()));assertTrue(DvrSources.native(usb.single()))
        assertFalse(DvrSources.native("0"));assertFalse(DvrSources.mounted(usb.single()))
    }
    @Test fun permissionMustBeCurrentAndConfirmedByAndroid(){
        val request=UsbPermissionRequest()
        request.begin("camera",1000)
        assertEquals(UsbPermissionRequest.Result.STALE,request.complete("other",true,true,2000))
        assertEquals("camera",request.device)
        assertEquals(UsbPermissionRequest.Result.DENIED,request.complete("camera",true,false,2000))
        request.begin("camera",3000)
        assertEquals(UsbPermissionRequest.Result.GRANTED,request.complete("camera",true,true,22999))
        assertNull(request.device)
    }
    @Test fun expiredOrReplacedRequestsCannotOpenACameraAfterResume(){
        val request=UsbPermissionRequest();request.begin("old",0);request.begin("new",1000)
        assertEquals(UsbPermissionRequest.Result.STALE,request.complete("old",true,true,1500))
        assertEquals(UsbPermissionRequest.Result.EXPIRED,request.complete("new",true,true,21000))
        assertNull(request.device)
        request.begin("camera",100000);assertFalse(request.expire(119999));assertTrue(request.expire(120000))
        assertEquals(UsbPermissionRequest.Result.STALE,request.complete("camera",true,true,120001))
    }
}
