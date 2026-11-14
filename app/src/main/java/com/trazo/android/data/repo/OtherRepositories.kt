package com.trazo.android.data.repo

import com.trazo.android.data.api.ExercisesApi
import com.trazo.android.data.api.SubscriptionApi
import com.trazo.android.data.model.*
import com.trazo.android.data.net.ApiResult
import com.trazo.android.data.net.apiCall

data class ExerciseFilters(
    val query: String = "",
    val movementPattern: String? = null,
    val experience: String? = null,
    val equipment: String? = null,
    val muscle: String? = null
) {
    val active: Boolean get() = movementPattern != null || experience != null || equipment != null || muscle != null
}

class ExerciseRepository(private val api: ExercisesApi, private val baseUrl: String = "") {
    suspend fun images(id: String): ApiResult<ExerciseImageSlots> = apiCall { api.images(id) }

    /** Public image bytes of one slot (0-3); loaded and cached by the image library. */
    fun imageUrl(id: String, slot: Int): String = "${baseUrl}api/v1/exercises/$id/images/$slot"

    suspend fun search(filters: ExerciseFilters, page: Int, pageSize: Int = 30): ApiResult<ExercisePage> = apiCall {
        api.search(
            q = filters.query.trim().ifEmpty { null },
            movementPattern = filters.movementPattern,
            experience = filters.experience,
            equipment = filters.equipment,
            muscle = filters.muscle,
            page = page,
            pageSize = pageSize
        )
    }

    suspend fun detail(id: String): ApiResult<ExerciseDetail> = apiCall { api.detail(id) }
}

class SubscriptionRepository(private val api: SubscriptionApi) {
    suspend fun status(): ApiResult<SubscriptionStatus> = apiCall { api.status() }

    suspend fun verify(productId: String, purchaseToken: String): ApiResult<SubscriptionStatus> =
        apiCall { api.verify(VerifySubscriptionRequest(productId, purchaseToken)) }
}
