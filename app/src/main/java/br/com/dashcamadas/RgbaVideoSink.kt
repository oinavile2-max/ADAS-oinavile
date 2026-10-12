package br.com.dashcamadas

import android.media.Image
import android.opengl.*
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Own EGL encoder surface: records actual RGBA frames without undocumented SDK encoder flags. */
class RgbaVideoSink(surface:Surface,private val width:Int,private val height:Int):AutoCloseable {
    private val display=EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private var context:EGLContext=EGL14.EGL_NO_CONTEXT
    private var window:EGLSurface=EGL14.EGL_NO_SURFACE
    private var program=0
    private var texture=0
    private var upload=ByteBuffer.allocateDirect(1)
    private val vertices=ByteBuffer.allocateDirect(16*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply{
        put(floatArrayOf(-1f,-1f,0f,1f,1f,-1f,1f,1f,-1f,1f,0f,0f,1f,1f,1f,0f));rewind()
    }
    init {
        try{
            check(display!=EGL14.EGL_NO_DISPLAY && EGL14.eglInitialize(display,IntArray(2),0,IntArray(2),0)){"EGL indisponível"}
            val configs=arrayOfNulls<EGLConfig>(1);val count=IntArray(1)
            val attributes=intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,
                EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,0x3142,1,EGL14.EGL_NONE)
            check(EGL14.eglChooseConfig(display,attributes,0,configs,0,1,count,0) && count[0]>0){"EGL recordable indisponível"}
            context=EGL14.eglCreateContext(display,configs[0],EGL14.EGL_NO_CONTEXT,intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,2,EGL14.EGL_NONE),0)
            check(context!=EGL14.EGL_NO_CONTEXT)
            window=EGL14.eglCreateWindowSurface(display,configs[0],surface,intArrayOf(EGL14.EGL_NONE),0)
            check(window!=EGL14.EGL_NO_SURFACE);current()
            fun shader(type:Int,source:String):Int {
                val value=GLES20.glCreateShader(type);GLES20.glShaderSource(value,source);GLES20.glCompileShader(value)
                val status=IntArray(1);GLES20.glGetShaderiv(value,GLES20.GL_COMPILE_STATUS,status,0)
                check(status[0]!=0){"Shader: "+GLES20.glGetShaderInfoLog(value)};return value
            }
            val a=shader(GLES20.GL_VERTEX_SHADER,"attribute vec2 pos;attribute vec2 uv;varying vec2 tex;void main(){gl_Position=vec4(pos,0.0,1.0);tex=uv;}")
            val b=shader(GLES20.GL_FRAGMENT_SHADER,"precision mediump float;uniform sampler2D image;varying vec2 tex;void main(){gl_FragColor=texture2D(image,tex);}")
            program=GLES20.glCreateProgram();GLES20.glAttachShader(program,a);GLES20.glAttachShader(program,b);GLES20.glLinkProgram(program)
            GLES20.glDeleteShader(a);GLES20.glDeleteShader(b)
            val status=IntArray(1);GLES20.glGetProgramiv(program,GLES20.GL_LINK_STATUS,status,0);check(status[0]!=0){"EGL shader link falhou"}
            val ids=IntArray(1);GLES20.glGenTextures(1,ids,0);texture=ids[0];GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_CLAMP_TO_EDGE)
        }catch(e:Exception){close();throw e}
    }
    private fun current(){check(EGL14.eglMakeCurrent(display,window,window,context)){"EGL encoder não ativou"}}
    fun render(image:Image,presentationNs:Long){
        current();val crop=image.cropRect;val p=image.planes[0];val w=crop.width();val h=crop.height()
        check(p.pixelStride>=4)
        if(upload.capacity()!=w*h*4)upload=ByteBuffer.allocateDirect(w*h*4)
        upload.clear();val source=p.buffer.duplicate();val offset=source.position();val validLimit=source.limit()
        for(y in 0 until h){
            val row=offset+(crop.top+y)*p.rowStride+crop.left*p.pixelStride
            if(p.pixelStride==4){check(row+w*4<=validLimit);source.limit(row+w*4);source.position(row);upload.put(source);source.limit(validLimit)}
            else for(x in 0 until w)for(c in 0..3)upload.put(source.get(row+x*p.pixelStride+c))
        }
        upload.flip();GLES20.glViewport(0,0,width,height);GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT,1)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D,0,GLES20.GL_RGBA,w,h,0,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,upload)
        GLES20.glUniform1i(GLES20.glGetUniformLocation(program,"image"),0)
        val pos=GLES20.glGetAttribLocation(program,"pos");val uv=GLES20.glGetAttribLocation(program,"uv")
        vertices.position(0);GLES20.glVertexAttribPointer(pos,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(pos)
        vertices.position(2);GLES20.glVertexAttribPointer(uv,2,GLES20.GL_FLOAT,false,16,vertices);GLES20.glEnableVertexAttribArray(uv)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP,0,4)
        check(GLES20.glGetError()==GLES20.GL_NO_ERROR){"Upload RGBA para encoder falhou"}
        check(EGLExt.eglPresentationTimeANDROID(display,window,presentationNs))
        check(EGL14.eglSwapBuffers(display,window)){"Encoder não recebeu quadro"}
    }
    override fun close(){
        if(context!=EGL14.EGL_NO_CONTEXT && window!=EGL14.EGL_NO_SURFACE){
            if(EGL14.eglMakeCurrent(display,window,window,context)){
                if(texture!=0)GLES20.glDeleteTextures(1,intArrayOf(texture),0)
                if(program!=0)GLES20.glDeleteProgram(program)
            }
        }
        EGL14.eglMakeCurrent(display,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_SURFACE,EGL14.EGL_NO_CONTEXT)
        if(window!=EGL14.EGL_NO_SURFACE)EGL14.eglDestroySurface(display,window)
        if(context!=EGL14.EGL_NO_CONTEXT)EGL14.eglDestroyContext(display,context)
        window=EGL14.EGL_NO_SURFACE;context=EGL14.EGL_NO_CONTEXT
        // Do not terminate the shared default display used by the vendor's preview renderer.
    }
}
