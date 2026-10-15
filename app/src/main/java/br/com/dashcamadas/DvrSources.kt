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

/** Bounded marker search. Never follows links outside the chosen removable volume or creates files. */
object DvrMarkerSearch {
    fun find(root:File,packageName:String):List<File>{
        val base=try{root.canonicalFile}catch(_:Exception){return emptyList()}
        val found=linkedSetOf<File>()
        fun inspect(dir:File){
            val canonical=try{dir.canonicalFile}catch(_:Exception){return}
            if(canonical!=base && !canonical.path.startsWith(base.path+File.separator))return
            val items=canonical.listFiles() ?: return
            for(file in items.take(64))try{
                val actual=file.canonicalFile
                if(actual.path.startsWith(base.path+File.separator) && actual.isFile && actual.length()>0 && DvrProtocol.type(actual.name)!=null)found+=actual
            }catch(_:Exception){}
        }
        inspect(base)
        inspect(File(base,"Android/data/com.ankai.cardvr"));inspect(File(base,"Android/data/$packageName"))
        base.listFiles()?.take(64)?.filter{it.isDirectory && !it.name.equals("Android",true)}?.forEach{inspect(it)}
        return found.toList()
    }
    fun inside(file:File,root:File)=try{file.canonicalPath.startsWith(root.canonicalPath+File.separator)}catch(_:Exception){false}
}
