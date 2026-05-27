package com.example.data

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class CurrentWeather(
    @Json(name = "temperature") val temperature: Double,
    @Json(name = "windspeed") val windspeed: Double,
    @Json(name = "weathercode") val weathercode: Int,
    @Json(name = "time") val time: String
)

data class WeatherResponse(
    @Json(name = "current_weather") val currentWeather: CurrentWeather
)

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true
    ): WeatherResponse
}

object WeatherClient {
    private const val BASE_URL = "https://api.open-meteo.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val api: WeatherApi = retrofit.create(WeatherApi::class.java)
}

data class LocalCity(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val state: String = ""
)

val IndianCities = listOf(
    LocalCity("Mumbai", 19.0760, 72.8777, "Maharashtra"),
    LocalCity("New Delhi", 28.6139, 77.2090, "Delhi"),
    LocalCity("Bangalore", 12.9716, 77.5946, "Karnataka"),
    LocalCity("Kolkata", 22.5726, 88.3639, "West Bengal"),
    LocalCity("Chennai", 13.0827, 80.2707, "Tamil Nadu"),
    LocalCity("Hyderabad", 17.3850, 78.4867, "Telangana"),
    LocalCity("Ahmedabad", 23.0225, 72.5714, "Gujarat"),
    LocalCity("Pune", 18.5204, 73.8567, "Maharashtra"),
    LocalCity("Jaipur", 26.9124, 75.7873, "Rajasthan"),
    LocalCity("Lucknow", 26.8467, 80.9462, "Uttar Pradesh")
)
