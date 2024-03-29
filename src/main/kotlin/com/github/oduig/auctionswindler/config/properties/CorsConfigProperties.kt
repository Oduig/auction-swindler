package com.github.oduig.auctionswindler.config.properties

data class CorsConfigProperties(
    // These need to be nullable so Spring Boot can load them
    var methods: List<String> = emptyList(),
    var origins: List<String> = emptyList(),
    var routes: List<String> = emptyList()
)
