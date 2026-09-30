package io.github.relony.pinry.data.api

import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.create
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** Pinry REST API v2. Paths are relative and keep their trailing slash (Django redirects without it). */
interface PinryApi {
    /** The logged-in user as a one-element list, or an empty list when anonymous. */
    @GET("api/v2/profile/users/")
    suspend fun currentUser(): Response<List<User>>

    @POST("api/v2/profile/login/")
    suspend fun login(
        @Header("X-CSRFToken") csrfToken: String,
        @Header("Referer") referer: String,
        @Body body: LoginRequest,
    ): User

    @GET("api/v2/profile/logout/")
    suspend fun logout()

    /** Filters: [board] is the board id (Pinry calls this filter `pins__id`). */
    @GET("api/v2/pins/")
    suspend fun pins(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 30,
        @Query("tags__name") tag: String? = null,
        @Query("submitter__username") user: String? = null,
        @Query("pins__id") board: Int? = null,
        @Query("ordering") ordering: String = "-id",
    ): Page<Pin>

    @GET("api/v2/pins/{id}/")
    suspend fun pin(@Path("id") id: Int): Pin

    @POST("api/v2/pins/")
    suspend fun createPin(@Body pin: NewPin): Pin

    @PATCH("api/v2/pins/{id}/")
    suspend fun updatePin(@Path("id") id: Int, @Body update: PinUpdate): Pin

    @DELETE("api/v2/pins/{id}/")
    suspend fun deletePin(@Path("id") id: Int)

    @Multipart
    @POST("api/v2/images/")
    suspend fun uploadImage(@Part image: MultipartBody.Part): PinImage

    @GET("api/v2/boards/")
    suspend fun boards(
        @Query("submitter__username") user: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int = 50,
    ): Page<Board>

    @GET("api/v2/boards/{id}/")
    suspend fun board(@Path("id") id: Int): Board

    @GET("api/v2/boards-auto-complete/")
    suspend fun boardNames(@Query("submitter__username") user: String): List<BoardName>

    @POST("api/v2/boards/")
    suspend fun createBoard(@Body board: NewBoard): Board

    @PATCH("api/v2/boards/{id}/")
    suspend fun updateBoard(@Path("id") id: Int, @Body update: BoardUpdate): Board

    @DELETE("api/v2/boards/{id}/")
    suspend fun deleteBoard(@Path("id") id: Int)

    @GET("api/v2/tags-auto-complete/")
    suspend fun tags(): List<TagName>
}

fun createPinryApi(baseUrl: HttpUrl, client: OkHttpClient): PinryApi =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(PinryJson.asConverterFactory("application/json".toMediaType()))
        .build()
        .create()
