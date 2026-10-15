package br.com.dashcamadas

/** Vendor Android 7 ROMs may publish USB mounts outside StorageManager's public volume list. */
object DvrMountPaths {
    fun parse(mounts:String):List<String> = buildList {
        for(line in mounts.lineSequence().take(512)){
            val fields=line.trim().split(Regex("\\s+"));if(fields.size<3)continue
            val path=fields[1].replace("\\040"," ").replace("\\011","\t").replace("\\134","\\")
            if(path.split('/').any{it==".." || it=="."})continue
            val media=path.startsWith("/mnt/media_rw/")
            val storage=path.startsWith("/storage/") && !path.startsWith("/storage/emulated") && !path.startsWith("/storage/self")
            val vendor=path=="/mnt/usb_storage" || path.startsWith("/mnt/usb_storage/") || path.matches(Regex("/mnt/usbhost[0-9]+(?:/[^/]+)?"))
            if(media)add("/storage/"+path.removePrefix("/mnt/media_rw/"))
            if(media || storage || vendor)add(path)
        }
    }.distinct()
}
