package br.com.dashcamadas

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import java.io.File

object MountedDvrDiscovery {
    @Volatile var cached:List<String> = emptyList();private set
    fun allowed(context:Context)=BuildConfig.K2000_NATIVE && Build.VERSION.SDK_INT in 24..27 &&
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED &&
        context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)==PackageManager.PERMISSION_GRANTED
    fun roots(context:Context):List<File>{
        if(!allowed(context))return emptyList()
        val roots=linkedSetOf<File>()
        try{
            val volumes=context.getSystemService(android.os.storage.StorageManager::class.java).storageVolumes
            volumes.filter{it.isRemovable && !it.isPrimary}.mapNotNull{it.uuid}.forEach{uuid->
                if(uuid.matches(Regex("[A-Za-z0-9_-]{1,80}")))roots+=File("/storage/$uuid")
            }
        }catch(_:Exception){}
        try{DvrMountPaths.parse(File("/proc/mounts").readText()).forEach{roots+=File(it)}}catch(_:Exception){}
        File("/storage").listFiles()?.filter{it.name !in setOf("emulated","self") && it.isDirectory}?.take(32)?.forEach{dir->
            try{val real=dir.canonicalFile;if(real.path.startsWith("/storage/") && !real.path.startsWith("/storage/emulated/"))roots+=real}catch(_:Exception){}
        }
        val primary=try{Environment.getExternalStorageDirectory().canonicalFile}catch(_:Exception){null}
        return roots.filter{it.isDirectory && it.canRead() && it.canonicalFile!=primary && (primary==null || !DvrMarkerSearch.inside(it,primary))}.toList()
    }
    fun scan(context:Context):List<String>{
        cached=roots(context).flatMap{DvrMarkerSearch.find(it,context.packageName)}.distinct().map{DvrSources.FILE+it.path}
        return cached
    }
    fun validate(context:Context,source:String):File{
        check(allowed(context)){"Autorize a leitura e escrita de volumes no Android 7/8"}
        require(DvrSources.mounted(source))
        val file=File(source.removePrefix(DvrSources.FILE)).canonicalFile
        require(DvrProtocol.type(file.name)!=null && file.isFile && file.length()>0 && file.canRead() && file.canWrite()){"Marcador DVR ausente ou sem acesso de leitura/escrita"}
        require(roots(context).any{DvrMarkerSearch.inside(file,it)}){"Marcador fora dos volumes removíveis permitidos"}
        return file
    }
}
