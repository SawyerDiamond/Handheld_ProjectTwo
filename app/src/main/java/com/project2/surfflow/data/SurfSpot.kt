package com.project2.surfflow.data

data class SurfSpot(
    val name: String,
    val latitude: Double,
    val longitude: Double
)

object LongIslandSurfSpots {
    val spots = listOf(
        SurfSpot("Montauk Point", 41.0706, -71.8564),
        SurfSpot("Ditch Plains", 41.0431, -71.8564),
        SurfSpot("Lighthouse Beach", 41.0706, -71.8564),
        SurfSpot("Gurney's", 41.0431, -71.8564),
        SurfSpot("Hither Hills", 41.0100, -71.8500),
        SurfSpot("Amagansett", 40.9767, -72.1431),
        SurfSpot("East Hampton", 40.9634, -72.1848),
        SurfSpot("Wainscott", 40.9365, -72.2431),
        SurfSpot("Bridgehampton", 40.9379, -72.3008),
        SurfSpot("Southampton", 40.8843, -72.3892),
        SurfSpot("Shinnecock Inlet", 40.8500, -72.4667),
        SurfSpot("Westhampton", 40.8081, -72.6447),
        SurfSpot("Fire Island", 40.6500, -73.2000),
        SurfSpot("Jones Beach", 40.5833, -73.5167),
        SurfSpot("Long Beach", 40.5884, -73.6581),
        SurfSpot("Rockaway Beach", 40.5795, -73.8142)
    )
}

