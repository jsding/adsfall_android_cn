package com.samsung.android.sdk.iap.sample.model

import android.content.Context
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

class ProductDetailsInteractor(private val context: Context) {
    private val iapHelper: IapHelper = IapHelper.getInstance(context)

    fun getProductsDetails(ids: String, callback: (ErrorVo, ArrayList<ProductVo>) -> Unit) {
        iapHelper.getProductsDetails(ids, callback)
    }
}