package br.com.dashcamadas.vision

import kotlin.math.*

/** Experimental calibrated risk evaluation; confidence loss stops critical output immediately. */
class RiskEngine(private val tuning:RiskTuning=RiskTuning()) {
    private data class Gate(var phase:Phase=Phase.UNAVAILABLE,var since:Long=0,var lastAlert:Long=Long.MIN_VALUE/2)
    private val gates=Risk.entries.associateWith{Gate()}
    private var target:Vehicle?=null
    private var targetTime=0L
    private var pedestrianTarget:Pedestrian?=null
    private var lastUpdate=-1L
    private val history=ArrayDeque<Pair<Long,Double>>()
    fun reset() { lastUpdate=-1;target=null;pedestrianTarget=null;history.clear();gates.values.forEach{it.phase=Phase.UNAVAILABLE;it.since=0} }
    fun update(now:Long,lanes:Lanes,vehicles:List<Vehicle>,speedMs:Float?,calibration:Calibration,pedestrians:List<Pedestrian> = emptyList()):RiskResult {
        if(lastUpdate>=0 && (now<=lastUpdate || now-lastUpdate>800))reset()
        lastUpdate=now
        val alerts=mutableSetOf<Risk>()
        fun gate(kind:Risk,available:Boolean,danger:Boolean) {
            val g=gates.getValue(kind)
            if(!available) {g.phase=Phase.UNAVAILABLE;g.since=now;return}
            if(danger) {
                if(g.phase!=Phase.CANDIDATE && g.phase!=Phase.RISK) {g.phase=Phase.CANDIDATE;g.since=now}
                if(g.phase==Phase.CANDIDATE && now-g.since>=tuning.confirmationMs) {
                    g.phase=Phase.RISK
                    if(now-g.lastAlert>=tuning.cooldownMs) {alerts.add(kind);g.lastAlert=now}
                }
            } else {
                if(g.phase==Phase.RISK) {g.phase=Phase.RECOVERY;g.since=now}
                else if(g.phase!=Phase.RECOVERY || now-g.since>=800) g.phase=Phase.IDLE
            }
        }
        val l=lanes.left;val r=lanes.right
        val laneAvailable=calibration.valid && speedMs!=null && speedMs>=tuning.departureMinSpeedMs && l!=null && r!=null && min(l.confidence,r.confidence)>=.65f && l.yStart<=.93f && r.yStart<=.93f && l.yEnd>=.93f && r.yEnd>=.93f
        val lx=l?.x(.93f) ?: 0f;val rx=r?.x(.93f) ?: 1f;val width=rx-lx
        val half=calibration.vehicleWidthM/calibration.laneWidthM/2+(tuning.departureSensitivity-50)/50f*.06f
        val valid=laneAvailable && width in .16f.. .92f
        gate(Risk.LEFT_DEPARTURE,valid,valid && (calibration.egoX-lx)/width<=half)
        gate(Risk.RIGHT_DEPARTURE,valid,valid && (rx-calibration.egoX)/width<=half)
        val candidates=vehicles.filter {
            val corridor=.08f+.28f*it.box.bottom
            val pathCenter=if(l!=null && r!=null && min(l.confidence,r.confidence)>=.65f && it.box.bottom in max(l.yStart,r.yStart)..min(l.yEnd,r.yEnd))(l.x(it.box.bottom)+r.x(it.box.bottom))/2 else calibration.egoX
            it.confidence>=.6f && it.box.width>=.04f && it.box.bottom>.4f && abs(it.box.centerX-pathCenter)<corridor
        }
        val old=target
        val matched=if(old!=null && now-targetTime<800) candidates.filter{it.category==old.category && it.box.iou(old.box)>.25f}.maxByOrNull{it.box.iou(old.box)} else null
        val selected=matched ?: candidates.maxByOrNull{it.box.area}
        if(matched==null || now<=targetTime || now-targetTime>800) history.clear()
        target=selected;targetTime=now
        var ttc:Float?=null
        if(selected!=null) {
            history.addLast(now to ln(sqrt(selected.box.area.toDouble().coerceAtLeast(1e-8))))
            while(history.isNotEmpty() && now-history.first().first>2500) history.removeFirst()
            if(history.size>=5 && now-history.first().first>=1000) {
                val base=history.first().first
                val times=history.map{(it.first-base)/1000.0}
                val mt=times.average();val mv=history.map{it.second}.average()
                val variance=times.sumOf{(it-mt)*(it-mt)}
                val covariance=history.mapIndexed{i,p->(times[i]-mt)*(p.second-mv)}.sum()
                val slope=if(variance>0) covariance/variance else 0.0
                val predicted=times.map{slope*(it-mt)+mv}
                val total=history.sumOf{(it.second-mv).pow(2)}
                val error=history.mapIndexed{i,p->(p.second-predicted[i]).pow(2)}.sum()
                val quality=if(total>1e-6) 1-error/total else 0.0
                if(slope>.08 && quality>.75) ttc=(1/slope).toFloat()
            }
        } else history.clear()
        val collisionAvailable=calibration.valid && speedMs!=null && speedMs>=3f && selected!=null && history.size>=5 && now-history.first().first>=1000
        val approaching=history.size>=2 && history.last().second>history.elementAt(history.size-2).second
        gate(Risk.COLLISION,collisionAvailable,collisionAvailable && approaching && ttc!=null && ttc<=tuning.collisionTtcSeconds && selected!!.box.width>.07f)
        // Inputs here have already passed the road mask. No assumed pedestrian distance in metres.
        val pedestrian=pedestrians.filter{person->
            val center=if(l!=null && r!=null && min(l.confidence,r.confidence)>=.65f && person.box.bottom in max(l.yStart,r.yStart)..min(l.yEnd,r.yEnd))(l.x(person.box.bottom)+r.x(person.box.bottom))/2 else calibration.egoX
            person.confidence>=.6f && person.box.height>=.1f && person.box.width>=.025f && person.box.bottom>=.6f && abs(person.box.centerX-center)<.08f+.28f*person.box.bottom
        }.maxByOrNull{it.box.area}
        val samePerson=pedestrian!=null && pedestrianTarget?.box?.iou(pedestrian.box)?.let{it>.25f}==true
        if(pedestrian!=null && !samePerson){gates.getValue(Risk.PEDESTRIAN).apply{phase=Phase.UNAVAILABLE;since=now}}
        pedestrianTarget=pedestrian
        val pedestrianAvailable=calibration.valid && speedMs!=null && speedMs>=3f && pedestrian!=null
        gate(Risk.PEDESTRIAN,pedestrianAvailable,pedestrianAvailable)
        return RiskResult(gates.filterValues{it.phase==Phase.RISK}.keys,gates.mapValues{it.value.phase},selected,ttc,alerts,pedestrian)
    }
}
