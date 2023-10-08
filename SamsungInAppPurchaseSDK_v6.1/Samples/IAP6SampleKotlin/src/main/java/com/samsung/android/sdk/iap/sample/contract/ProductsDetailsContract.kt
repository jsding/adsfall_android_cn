package com.samsung.android.sdk.iap.sample.contract

import android.content.Context
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import java.util.ArrayList

interface ProductsDetailsContract {
    interface View {
        fun showProductsDetails(productList: ArrayList<ProductVo>)
    }

    interface Presenter {
        fun getProductsDetails(productIds: String)
        fun setContext(context: Context)
    }
}