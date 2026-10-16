package br.com.dashcamadas

import java.io.File

object DvrSources {
    const val FILE="dvrfile:"
    fun native(id:String)=id.startsWith("usb:") || id.startsWith(FILE)
    fun mounted(id:String)=id.startsWith(FILE)
}
enum class DvrSearchMode(val label:String){
    USB_FIRST("USB primeiro"),VOLUME_FIRST("Volumes primeiro"),USB_ONLY("Somente USB"),VOLUME_ONLY("Somente volumes");
    fun order(usb:List<String>,volumes:List<String>)=when(this){USB_FIRST->usb+volumes;VOLUME_FIRST->volumes+usb;USB_ONLY->usb;VOLUME_ONLY->volumes}.distinct()
    companion object{fun read(value:String?)=entries.firstOrNull{it.name==value} ?: USB_FIRST}
}

/** Shallow marker search. Never follows links outside the chosen removable volume or creates files. */
object DvrMarkerSearch {
    fun find(root:File,packageName:String):List<File>{
        val base=try{root.canonicalFile}catch(_:Exception){return emptyList()}
        val found=linkedSetOf<File>()
        fun inspect(dir:File){
            val canonical=try{dir.canonicalFile}catch(_:Exception){return}
            if(canonical!=base && !canonical.path.startsWith(base.path+File.separator))return
            // Filter names before limiting directory traversal: recordings must not hide a marker.
            val items=canonical.listFiles{_,name->DvrProtocol.type(name)!=null} ?: return
            for(file in items)try{
                val actual=file.canonicalFile
                if(actual.path.startsWith(base.path+File.separator) && actual.isFile && actual.length()>0 && DvrProtocol.type(actual.name)!=null)found+=actual
            }catch(_:Exception){}
        }
        fun inspectContainer(dir:File){
            inspect(dir)
            inspect(File(dir,"Android/data/com.ankai.cardvr"));inspect(File(dir,"Android/data/$packageName"))
        }
        inspectContainer(base)
        base.listFiles{file->file.isDirectory && !file.name.equals("Android",true)}?.take(64)?.forEach{inspectContainer(it)}
        return found.toList()
    }
    fun inside(file:File,root:File)=try{file.canonicalPath.startsWith(root.canonicalPath+File.separator)}catch(_:Exception){false}
}
