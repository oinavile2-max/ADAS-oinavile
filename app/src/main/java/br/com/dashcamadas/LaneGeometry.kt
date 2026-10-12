package br.com.dashcamadas

/** Original V4.8 straight-line candidate detector, shared by Android and offline tests.
 * Candidate output is NOT a calibrated lane-departure warning. */
object LaneGeometry {
    data class Result(val left: FloatArray?, val right: FloatArray?)
    fun detect(gray: IntArray, w: Int, h: Int): Result? {
        require(gray.size == w*h)
        if (w < 80 || h < 60) return null
        val leftPts = ArrayList<Pair<Float,Float>>()
        val rightPts = ArrayList<Pair<Float,Float>>()
        val y0 = (h * .56f).toInt()
        for (y in y0 until h step 3) {
            val t = (y - y0).toFloat() / (h - y0)
            val cx = w / 2
            val span = (w * (.11f + .35f * t)).toInt()
            val gap = (w * (.025f + .03f * t)).toInt()
            fun best(a: Int, b: Int): Int {
                var bx = -1; var score = 0
                for (x in a.coerceAtLeast(2) until b.coerceAtMost(w - 2)) {
                    val v = gray[y * w + x]
                    val edge = kotlin.math.abs(v - gray[y * w + x - 2]) + kotlin.math.abs(v - gray[y * w + x + 2])
                    val strength = if (v > 135) edge else 0
                    if (strength > score) { score = strength; bx = x }
                }
                return if (score > 95) bx else -1
            }
            val l = best(cx - span, cx - gap)
            val r = best(cx + gap, cx + span)
            if (l >= 0) leftPts.add(l.toFloat() to y.toFloat())
            if (r >= 0) rightPts.add(r.toFloat() to y.toFloat())
        }
        fun fit(points: List<Pair<Float,Float>>): FloatArray? {
            if (points.size < 8) return null
            val n = points.size.toFloat()
            val mx = points.sumOf { it.first.toDouble() }.toFloat() / n
            val my = points.sumOf { it.second.toDouble() }.toFloat() / n
            var cov = 0f; var variance = 0f
            for ((x,y) in points) { cov += (x-mx)*(y-my); variance += (y-my)*(y-my) }
            if (variance < 1f) return null
            val slope = cov / variance
            val intercept = mx - slope * my
            val error = points.count { kotlin.math.abs(it.first - (slope*it.second + intercept)) < w*.055f }
            if (error < points.size * .7f) return null
            val top = h * .57f; val bottom = h * .96f
            val xt = (slope * top + intercept) / w; val xb = (slope * bottom + intercept) / w
            if (xt !in 0f..1f || xb !in 0f..1f) return null
            return floatArrayOf(xt, top/h, xb, bottom/h)
        }
        // Conservative perspective sanity check: reject vertical contrast streaks.
        // This straight-road baseline can also reject real curves; it is not operational LDW.
        val l = fit(leftPts)?.takeIf { it[2] < it[0] - .02f }
        val r = fit(rightPts)?.takeIf { it[2] > it[0] + .02f }
        if (l == null && r == null) return null
        if (l != null && r != null && (l[2] >= r[2] || r[2] - l[2] < .12f)) return null
        return Result(l,r)
    }
}
