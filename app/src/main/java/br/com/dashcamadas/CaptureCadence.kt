package br.com.dashcamadas

/** Capture freshness, preview pacing and neural duty cycle are independent. */
class CaptureCadence {
    private var previewDeadline = -1.0
    private var previewRate = 0
    private var analysisAt = -1L
    @Volatile private var lastAnalysisDuration = 0L
    fun claimPreview(now: Long, fps: Int): Boolean {
        val rate = fps.coerceIn(1, 30)
        val interval = 1000.0 / rate
        if (rate != previewRate) { previewRate = rate; previewDeadline = -1.0 }
        if (previewDeadline >= 0 && now + 1.0 < previewDeadline) return false
        previewDeadline = if (previewDeadline < 0 || now - previewDeadline > interval) now + interval else previewDeadline + interval
        return true
    }
    fun analysisDue(now: Long): Boolean = analysisAt < 0 || now - analysisAt >= analysisIntervalMs()
    fun analysisAccepted(now: Long) { analysisAt = now }
    fun analysisFinished(durationMs: Long) { lastAnalysisDuration = durationMs.coerceIn(0, 10000) }
    fun analysisIntervalMs(): Long = if (lastAnalysisDuration <= 500) (lastAnalysisDuration + 150).coerceIn(250, 700)
        else (lastAnalysisDuration * 2).coerceIn(1000, 20000)
    fun reset() { previewDeadline = -1.0; previewRate = 0; analysisAt = -1; lastAnalysisDuration = 0 }
}

/** One replaceable item and one posted drain; a blocked UI receives the freshest frame. */
class LatestFrameMailbox<T> {
    private var value: T? = null
    private var posted = false
    @Synchronized fun offer(item: T): Boolean { value = item; if (posted) return false; posted = true; return true }
    @Synchronized fun take(): T? { val next = value; value = null; posted = false; return next }
}

/** Analysis can annotate an exact live frame; it can never rewind or stall the live clock. */
class PreviewTimeline {
    var timestamp = -1L; private set
    fun accept(timestampMs: Long): Boolean {
        if (timestampMs <= timestamp) return false
        timestamp = timestampMs; return true
    }
    fun matches(timestampMs: Long) = timestampMs == timestamp
    fun reset() { timestamp = -1L }
}

/** Counts distinct draw events in a rolling two-second window, not inference callbacks. */
class PreviewFrameMeter {
    private val draws = java.util.ArrayDeque<Long>()
    private var lastTimestamp = -1L
    private var previousDraw = -1L
    private var lastGap = 0L
    private var maximumGap = 0L
    private var count = 0L
    @Synchronized fun drawn(timestamp: Long, now: Long) {
        if (timestamp <= lastTimestamp) return
        lastTimestamp = timestamp
        if (previousDraw >= 0 && now - previousDraw <= 2000) { lastGap = now - previousDraw; maximumGap = maxOf(maximumGap, lastGap) }
        previousDraw = now; count++; draws.addLast(now); trim(now)
    }
    private fun trim(now: Long) { while (draws.isNotEmpty() && now - draws.peekFirst() > 2000) draws.removeFirst() }
    data class Snapshot(val frames: Long, val fps: Double, val lastGapMs: Long, val maxGapMs: Long)
    @Synchronized fun snapshot(now: Long): Snapshot {
        trim(now)
        val duration = if (draws.size > 1) now - draws.peekFirst() else 0
        return Snapshot(count, if (duration > 0) (draws.size - 1) * 1000.0 / duration else 0.0, lastGap, maximumGap)
    }
    @Synchronized fun reset() { draws.clear(); lastTimestamp = -1; previousDraw = -1; lastGap = 0; maximumGap = 0; count = 0 }
}
