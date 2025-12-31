package org.example.app.ui.settings

import org.example.app.domain.City

object CityCatalog {
    fun cities(): List<City> {
        return listOf(
            City("Makkah", "Saudi Arabia", 21.3891, 39.8579),
            City("Madinah", "Saudi Arabia", 24.5247, 39.5692),
            City("Riyadh", "Saudi Arabia", 24.7136, 46.6753),
            City("Dubai", "United Arab Emirates", 25.2048, 55.2708),
            City("Cairo", "Egypt", 30.0444, 31.2357),
            City("Istanbul", "Turkey", 41.0082, 28.9784),
            City("London", "United Kingdom", 51.5072, -0.1276),
            City("New York", "United States", 40.7128, -74.0060),
            City("Toronto", "Canada", 43.6532, -79.3832),
            City("Kuala Lumpur", "Malaysia", 3.1390, 101.6869)
        )
    }
}
