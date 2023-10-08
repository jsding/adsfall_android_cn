package com.samsung.android.sdk.iap.sample.model

import android.content.Context
import com.samsung.android.sdk.iap.lib.helper.HelperDefine
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import java.util.ArrayList


class MainInteractor(private val context: Context) {
    private val PASS_THROUGH_PARAM = "TEMP_PASS_THROUGH"
    private val iapHelper: IapHelper = IapHelper.getInstance(context)

    init {
        iapHelper.setOperationMode(HelperDefine.OperationMode.OPERATION_MODE_PRODUCTION)
    }

    fun purchaseItem(itemId: String, callback: (ErrorVo?, PurchaseVo?) -> Unit) {
        iapHelper.startPayment(itemId, PASS_THROUGH_PARAM, callback)
    }

    fun consumeItem(purchaseId: String, callback: (ErrorVo?, ArrayList<ConsumeVo>?) -> Unit) {
        iapHelper.consumePurchasedItems(purchaseId, callback)
    }

    fun disposeIapHelper() {
        iapHelper.dispose()
    }

    fun getOwnedList(itemType: String, callback: (ErrorVo?, ArrayList<OwnedProductVo>?) -> Unit) {
        iapHelper.getOwnedList(itemType, callback)
    }
}