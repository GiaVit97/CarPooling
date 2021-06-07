package it.polito.mad.carpooling_09.data

/**
 * This class model the user preferences for the trip
 */
data class TripPreferences(
    var smoke: PreferencesType = PreferencesType.NoPreferences,
    var animals: PreferencesType = PreferencesType.NoPreferences,
    var music: PreferencesType = PreferencesType.NoPreferences
) {
    companion object {
        const val FIELD_SMOKE = "smoke"
        const val FIELD_ANIMALS = "animals"
        const val FIELD_MUSIC = "music"
    }
}

enum class PreferencesType(val choice : String){
    NoPreferences("No preferences"),
    Yes("Yes"),
    No("No")
}