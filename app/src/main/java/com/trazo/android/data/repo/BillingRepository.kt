package com.trazo.android.data.repo

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*
import com.trazo.android.data.model.SubscriptionStatus
import com.trazo.android.data.net.ApiError
import com.trazo.android.data.net.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** What the plans screen shows about the subscription offered by Google Play. Prices are localized by Google. */
data class BillingOffer(val productId: String, val title: String, val formattedPrice: String, val billingPeriod: String, val hasFreeTrial: Boolean)

sealed interface PurchaseResult {
    /** Play charged the user and the backend accepted the purchase; [status] is what the API now says about access. */
    data class Verified(val status: SubscriptionStatus) : PurchaseResult
    data object Pending : PurchaseResult
    data object Canceled : PurchaseResult
    data class Failed(val error: ApiError? = null, val billingCode: Int? = null) : PurchaseResult
}

/**
 * Google Play Billing, kept inactive until the backend demands payment: nothing connects to Play unless the plans screen
 * is shown. The backend decides access; the app only hands over the purchase token (docs/subscriptions.md).
 */
interface BillingRepository {
    val results: SharedFlow<PurchaseResult>
    suspend fun loadOffer(): BillingOffer?
    suspend fun launch(activity: Activity, userId: String): Boolean
    suspend fun restore(): PurchaseResult?
    fun close()
}

class PlayBillingRepository(
    context: Context,
    private val subscriptions: SubscriptionRepository,
    private val productId: String
) : BillingRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val appContext = context.applicationContext
    private val _results = MutableSharedFlow<PurchaseResult>(extraBufferCapacity = 4)
    override val results: SharedFlow<PurchaseResult> = _results

    private var details: ProductDetails? = null
    private var client: BillingClient? = null

    private val listener = PurchasesUpdatedListener { result, purchases ->
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> _results.emit(process(purchases.orEmpty()) ?: PurchaseResult.Failed())
                BillingClient.BillingResponseCode.USER_CANCELED -> _results.emit(PurchaseResult.Canceled)
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> _results.emit(restore() ?: PurchaseResult.Failed(billingCode = result.responseCode))
                else -> _results.emit(PurchaseResult.Failed(billingCode = result.responseCode))
            }
        }
    }

    private suspend fun connected(): BillingClient? {
        client?.takeIf { it.isReady }?.let { return it }
        val created = BillingClient.newBuilder(appContext)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .build()
        val ready = CompletableDeferred<Boolean>()
        created.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) { ready.complete(result.responseCode == BillingClient.BillingResponseCode.OK) }
            override fun onBillingServiceDisconnected() { client = null }
        })
        if (!ready.await()) { created.endConnection(); return null }
        client = created
        return created
    }

    override suspend fun loadOffer(): BillingOffer? {
        val billing = connected() ?: return null
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            listOf(QueryProductDetailsParams.Product.newBuilder().setProductId(productId).setProductType(BillingClient.ProductType.SUBS).build())
        ).build()
        val found = billing.queryProductDetails(params).productDetailsList?.firstOrNull() ?: return null
        details = found
        val offer = pickOffer(found) ?: return null
        // The last pricing phase is the recurring price; an earlier phase at 0 is the free trial.
        val phases = offer.pricingPhases.pricingPhaseList
        val recurring = phases.last()
        return BillingOffer(
            productId, found.name, recurring.formattedPrice, recurring.billingPeriod,
            hasFreeTrial = phases.size > 1 && phases.first().priceAmountMicros == 0L
        )
    }

    override suspend fun launch(activity: Activity, userId: String): Boolean {
        val billing = connected() ?: return false
        val product = details ?: run { loadOffer(); details } ?: return false
        val offer = pickOffer(product) ?: return false
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).setOfferToken(offer.offerToken).build()))
            // The TRAZO user id (JWT sub): the backend rejects a purchase made by a different account.
            .setObfuscatedAccountId(userId)
            .build()
        return billing.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override suspend fun restore(): PurchaseResult? {
        val billing = connected() ?: return null
        val purchases = billing.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build())
        return process(purchases.purchasesList)
    }

    override fun close() { client?.endConnection(); client = null }

    private suspend fun process(purchases: List<Purchase>): PurchaseResult? {
        val mine = purchases.filter { productId in it.products }
        if (mine.isEmpty()) return null
        mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }?.let { purchase ->
            return when (val verified = subscriptions.verify(productId, purchase.purchaseToken)) {
                is ApiResult.Success -> PurchaseResult.Verified(verified.value)
                is ApiResult.Failure -> PurchaseResult.Failed(error = verified.error)
            }
        }
        return PurchaseResult.Pending
    }

    private fun pickOffer(product: ProductDetails): ProductDetails.SubscriptionOfferDetails? {
        val offers = product.subscriptionOfferDetails.orEmpty()
        // Prefer the offer that starts with a free trial (the ~14 days configured in Play Console), else the base plan.
        return offers.firstOrNull { it.pricingPhases.pricingPhaseList.firstOrNull()?.priceAmountMicros == 0L } ?: offers.firstOrNull()
    }
}
