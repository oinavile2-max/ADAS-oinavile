package br.com.dashcamadas

import android.app.Activity
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Android 15 draws under bars by default; keep buttons away from bars and display cutouts. */
object SafeInsets {
    fun apply(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window,false)
        val root=activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0) ?: return
        val left=root.paddingLeft;val top=root.paddingTop;val right=root.paddingRight;val bottom=root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view,insets ->
            val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.setPadding(left+safe.left,top+safe.top,right+safe.right,bottom+safe.bottom)
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }
}
