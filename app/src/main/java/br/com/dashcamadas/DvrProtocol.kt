package br.com.dashcamadas

/** Communication filenames and protocol numbers from the reference APK's filenames.json. */
object DvrProtocol {
    fun type(name:String):Int?=when(name.lowercase(java.util.Locale.ROOT)){
        "akfile","elene"->0
        "kkfile","eefile"->1
        "jrfile"->2
        else->null
    }
}
