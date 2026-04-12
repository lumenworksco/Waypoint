package com.florian.piste

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint

class PisteViewModel(application: Application) : AndroidViewModel(application) {
    val repository = PisteRepository(application)

    // Location state
    private val _userLocation = MutableStateFlow<GeoPoint?>(null)
    val userLocation: StateFlow<GeoPoint?> = _userLocation.asStateFlow()

    private val _locationEnabled = MutableStateFlow(false)
    val locationEnabled: StateFlow<Boolean> = _locationEnabled.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    private val _currentAltitude = MutableStateFlow<Double?>(null)
    val currentAltitude: StateFlow<Double?> = _currentAltitude.asStateFlow()

    // Weather state
    private val _weather = MutableStateFlow<WeatherData?>(null)
    val weather: StateFlow<WeatherData?> = _weather.asStateFlow()

    private val _forecast = MutableStateFlow<Forecast?>(null)
    val forecast: StateFlow<Forecast?> = _forecast.asStateFlow()

    private val _awareness = MutableStateFlow<AwarenessAssessment?>(null)
    val awareness: StateFlow<AwarenessAssessment?> = _awareness.asStateFlow()

    private val _minutesToSunset = MutableStateFlow<Int?>(null)
    val minutesToSunset: StateFlow<Int?> = _minutesToSunset.asStateFlow()

    // Place name
    private val _placeName = MutableStateFlow<String?>(null)
    val placeName: StateFlow<String?> = _placeName.asStateFlow()

    // Achievement banner
    private val _achievementBanner = MutableStateFlow<String?>(null)
    val achievementBanner: StateFlow<String?> = _achievementBanner.asStateFlow()

    // Settings
    val useImperial: Boolean get() = repository.loadSetting("distance_unit", "METRIC") == "IMPERIAL"
    val hasOnboarded: Boolean get() = repository.loadSetting("has_onboarded", "false") == "true"

    // Fetch timestamps (for rate limiting)
    private var lastWeatherFetch = 0L
    private var lastForecastFetch = 0L
    private var lastPlaceFetchLoc: GeoPoint? = null
    private var lastProximityAlert = 0L

    fun updateLocation(lat: Double, lon: Double, altitude: Double?, speed: Float) {
        val gp = GeoPoint(lat, lon)
        _userLocation.value = gp
        _locationEnabled.value = true
        _currentSpeed.value = speed
        if (altitude != null && altitude != 0.0) _currentAltitude.value = altitude
    }

    fun fetchWeatherIfNeeded() {
        val loc = _userLocation.value ?: return
        val now = System.currentTimeMillis()
        if (now - lastWeatherFetch > PisteConstants.WEATHER_REFRESH_MS) {
            lastWeatherFetch = now
            viewModelScope.launch {
                val w = withContext(Dispatchers.IO) { fetchWeather(loc.latitude, loc.longitude) }
                _weather.value = w
            }
        }
    }

    fun fetchForecastIfNeeded() {
        val loc = _userLocation.value ?: return
        val now = System.currentTimeMillis()
        if (now - lastForecastFetch > PisteConstants.FORECAST_REFRESH_MS) {
            lastForecastFetch = now
            viewModelScope.launch {
                val f = withContext(Dispatchers.IO) { fetchForecast(loc.latitude, loc.longitude) }
                _forecast.value = f
                _awareness.value = assessAvalancheAwareness(f)
            }
        }
    }

    fun fetchForecastNow() {
        val loc = _userLocation.value ?: return
        viewModelScope.launch {
            val f = withContext(Dispatchers.IO) { fetchForecast(loc.latitude, loc.longitude) }
            _forecast.value = f
        }
    }

    fun refreshPlaceName() {
        val loc = _userLocation.value ?: return
        val last = lastPlaceFetchLoc
        if (last == null || distanceMeters(last, loc) > PisteConstants.PLACE_REFETCH_DISTANCE_M) {
            lastPlaceFetchLoc = loc
            viewModelScope.launch {
                val name = withContext(Dispatchers.IO) { fetchPlaceName(loc.latitude, loc.longitude) }
                _placeName.value = name
            }
        }
    }

    fun refreshSunset() {
        val loc = _userLocation.value ?: return
        _minutesToSunset.value = minutesUntilSunset(loc.latitude, loc.longitude)
    }

    fun showAchievement(text: String) {
        _achievementBanner.value = text
        viewModelScope.launch {
            kotlinx.coroutines.delay(PisteConstants.ACHIEVEMENT_DISPLAY_MS)
            _achievementBanner.value = null
        }
    }

    fun checkProximityAlert(context: android.content.Context) {
        val loc = _userLocation.value ?: return
        val radius = repository.loadSetting("proximity_radius", "0").toIntOrNull() ?: 0
        if (radius <= 0) return
        val now = System.currentTimeMillis()
        if (now - lastProximityAlert <= PisteConstants.PROXIMITY_THROTTLE_MS) return
        for (wp in repository.waypoints.value) {
            if (distanceMeters(loc, GeoPoint(wp.latitude, wp.longitude)) < radius) {
                Haptics.proximity(context)
                lastProximityAlert = now
                break
            }
        }
    }

    fun completeOnboarding() {
        repository.saveSetting("has_onboarded", "true")
    }
}
