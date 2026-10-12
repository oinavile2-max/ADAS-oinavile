package br.com.dashcamadas.vision

/** COCO uses zero-based IDs: person=0; car=2, bus=5, truck=7. Keeps inference outputs separate. */
object SceneOutputDecoder {
    fun decode(locations:Array<FloatArray>,classes:FloatArray,scores:FloatArray,count:Float):SceneDetections {
        val vehicles=ArrayList<Vehicle>();val pedestrians=ArrayList<Pedestrian>()
        if(!count.isFinite())return SceneDetections(vehicles,pedestrians)
        val size=count.toInt().coerceIn(0,minOf(locations.size,classes.size,scores.size))
        for(index in 0 until size){
            val category=classes[index];val score=scores[index];val b=locations[index]
            if(!category.isFinite() || category!=category.toInt().toFloat() || !score.isFinite() || score !in .5f..1f || b.size!=4 || b.any{!it.isFinite()})continue
            val box=Box(b[1].coerceIn(0f,1f),b[0].coerceIn(0f,1f),b[3].coerceIn(0f,1f),b[2].coerceIn(0f,1f))
            if(box.width<=0 || box.height<=0)continue
            when(category.toInt()){
                0->pedestrians.add(Pedestrian(box,score))
                2,5,7->vehicles.add(Vehicle(box,score,category.toInt()))
            }
        }
        return SceneDetections(vehicles,pedestrians)
    }
}
