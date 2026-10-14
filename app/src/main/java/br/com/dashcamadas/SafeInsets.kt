package br.com.dashcamadas

import android.app.Activity
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Android 15 draws under bars by default; keep buttons away from bars and display cutouts. */
object SafeInsets {
    fun apply(activity: Activity, immersive: Boolean = false) {
        WindowCompat.setDecorFitsSystemWindows(activity.window,false)
        val root=activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        val left=root.paddingLeft;val top=root.paddingTop;val right=root.paddingRight;val bottom=root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
            val safe=if(immersive) {
                val bars=insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val cutout=insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.displayCutout())
                androidx.core.graphics.Insets.of(maxOf(bars.left,cutout.left),maxOf(bars.top,cutout.top),maxOf(bars.right,cutout.right),maxOf(bars.bottom,cutout.bottom))
            } else insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime=if(immersive)0 else insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            view.setPadding(left+safe.left,top+safe.top,right+safe.right,bottom+maxOf(safe.bottom,ime))
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
