package com.example.soundboard

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

interface FreesoundApi {
    @Headers("Authorization: Token GUZRyQWmhNzPLMA9WqY0lFSn5jKsHfRLQpNOTfoV")
    @GET("sounds/{id}/")
    fun getSoundById(@Path("id") id: Int): Call<Sound>
}