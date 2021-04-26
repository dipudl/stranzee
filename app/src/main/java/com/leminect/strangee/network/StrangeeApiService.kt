package com.leminect.strangee.network

import com.leminect.strangee.model.Strangee
import com.leminect.strangee.model.User
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

public const val BASE_URL = "http://192.168.4.2:3000/"
// "http://192.168.1.101:3000/" // "http://10.0.2.2:3000/" // "http://192.168.1.101:3000/"

private val moshi: Moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

private val retrofit: Retrofit = Retrofit.Builder()
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .baseUrl(BASE_URL)
    .build()

data class AuthBackData(
    val message: String,
    val data: User,
    val token: String,
)

data class LoginDetail(
    val email: String,
    val password: String,
)

data class CheckRegistrationInput(
    val email: String,
)

data class CheckRegistration(
    @Json(name = "user_not_exist") val userNotExist: Boolean,
)

data class StrangeeBackData(
    val data: List<Strangee>,
    val createdAt: String,
)

data class SaveStrangeeBackData(
    val userId: String,
    val error: Boolean,
    val saveStatus: Boolean,
)

data class BlockProfileBackData(
    val userId: String,
    val error: Boolean,
    val blockedStatus: Boolean,
)

data class SaveStrangeePostData(
    val strangeeId: String,
    val currentSavedStatus: Boolean,
)

interface StrangeeApiService {
    @Multipart
    @POST("signup")
    suspend fun postSignUp(
        @Part image: MultipartBody.Part,
        @Part("email") email: RequestBody,
        @Part("password") password: RequestBody,
        @Part("firstName") firstName: RequestBody,
        @Part("lastName") lastName: RequestBody,
        @Part("country") country: RequestBody,
        @Part("gender") gender: RequestBody,
        @Part("aboutMe") aboutMe: RequestBody,
        @Part("interestedIn") interestedIn: RequestBody,
        @Part("birthday") birthday: RequestBody,
    ): AuthBackData

    @Multipart
    @POST("profileImage")
    suspend fun postProfileImage(
        @Header("Authorization") token: String,
        @Part("_id") userId: RequestBody,
        @Part image: MultipartBody.Part,
    ): Boolean

    @Multipart
    @POST("imageUpload")
    suspend fun postImage(
        @Header("Authorization") token: String,
        @Part image: MultipartBody.Part,
    ): String

    @POST("check_registration")
    suspend fun postCheckRegistration(@Body input: CheckRegistrationInput): CheckRegistration

    @POST("login")
    suspend fun postLogin(@Body detail: LoginDetail): AuthBackData

    @GET("strangee")
    suspend fun getStrangee(
        @Header("Authorization") token: String,
        @Query("user") user: String,
        @Query("lastCreatedAt") createdAt: String,
        @Query("filterOn") filter: Boolean = false,
    ): StrangeeBackData

    @POST("save")
    suspend fun saveStrangee(
        @Header("Authorization") token: String,
        @Body data: SaveStrangeePostData,
    ): SaveStrangeeBackData

    @POST("editDetails")
    suspend fun postEditDetails(
        @Header("Authorization") token: String,
        @Body user: User,
    ): Boolean

    @GET("saved")
    suspend fun getSaved(
        @Header("Authorization") token: String,
        @Query("_id") userId: String,
    ): List<Strangee>

    @POST("removeSaved")
    suspend fun removeSavedProfile(
        @Header("Authorization") token: String,
        @Query("savedUserId") savedUserId: String,
    ): Boolean

    @GET("blocked")
    suspend fun getIsBlocked(
        @Header("Authorization") token: String,
        @Query("_id") userId: String,
    ): Boolean

    @POST("block")
    suspend fun postBlockProfile(
        @Header("Authorization") token: String,
        @Query("_id") strangeeUserId: String,
        @Query("blockedStatus") blockedStatus: Boolean,
    ): BlockProfileBackData

    @POST("report")
    suspend fun postReportProfile(
        @Header("Authorization") token: String,
        @Query("_id") userId: String,
        @Query("reportedUserId") strangeeUserId: String,
        @Query("message") message: String,
    ): Boolean

    @POST("whoCheckedMe")
    suspend fun postWhoCheckedMe(
        @Header("Authorization") token: String,
        @Query("_id") userId: String,
    ): Boolean

    @GET("whoCheckedMe")
    suspend fun getWhoCheckedMe(
        @Header("Authorization") token: String,
        @Query("_id") userId: String,
    ): List<Strangee>

    @POST("removeWhoCheckedMe")
    suspend fun removeWhoCheckedMe(
        @Header("Authorization") token: String,
        @Query("_id") strangeeUserId: String,
    ): Boolean
}

object StrangeeApi {
    val retrofitService: StrangeeApiService by lazy {
        retrofit.create(StrangeeApiService::class.java)
    }
}