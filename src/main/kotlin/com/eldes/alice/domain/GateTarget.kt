package com.eldes.alice.domain

enum class GateZone(val title: String) {
    YARD("Двор"),
    PARKING("Парковка"),
}

enum class GateDirection(val title: String) {
    IN("въезд"),
    OUT("выезд"),
}

data class GateTarget(
    val key: String,
    val deviceId: String,
    val zone: GateZone,
    val direction: GateDirection,
) {
    val title: String = "${zone.title} - ${direction.title}"
    val actionTitle: String = when (key) {
        "yard_in" -> "Въезд во двор"
        "yard_out" -> "Выезд из двора"
        "parking_in" -> "Въезд в паркинг"
        "parking_out" -> "Выезд из паркинга"
        else -> title
    }

    companion object {
        val all = listOf(
            GateTarget("yard_in", "38344", GateZone.YARD, GateDirection.IN),
            GateTarget("yard_out", "38356", GateZone.YARD, GateDirection.OUT),
            GateTarget("parking_in", "44925", GateZone.PARKING, GateDirection.IN),
            GateTarget("parking_out", "42668", GateZone.PARKING, GateDirection.OUT),
        )

        fun byKey(key: String): GateTarget? = all.firstOrNull { it.key == key }
    }
}
