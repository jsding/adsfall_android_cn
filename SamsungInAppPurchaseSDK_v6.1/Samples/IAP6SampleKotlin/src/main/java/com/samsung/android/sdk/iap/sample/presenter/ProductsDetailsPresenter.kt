package com.samsung.android.sdk.iap.sample.presenter

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.sample.contract.ProductsDetailsContract
import com.samsung.android.sdk.iap.sample.model.ProductDetailsInteractor
import java.util.ArrayList

class ProductsDetailsPresenter(private val view: ProductsDetailsContract.View) :
    ProductsDetailsContract.Presenter {
    private lateinit var interactor: ProductDetailsInteractor
    private val TAG = ProductsDetailsPresenter::class.java.simpleName

    override fun getProductsDetails(productIds: String) {
        interactor.getProductsDetails(productIds) onGetProducts@{ _errorVo: ErrorVo?, _productList: ArrayList<ProductVo>? ->
            Log.d(TAG, "onGetProducts is invoked")
            if (_errorVo == null) return@onGetProducts
            if (_errorVo.errorCode == IapHelper.IAP_ERROR_NONE) {
                if (_productList == null) return@onGetProducts
                view.showProductsDetails(_productList)
            } else {
                Log.e(TAG, "onGetProducts > ErrorCode [${_errorVo.errorCode}]")
                Log.e(TAG, "onGetProducts > ErrorString [${_errorVo.errorString ?: ""}]")
            }
        }
    }

    override fun setContext(context: Context) {
        interactor = ProductDetailsInteractor(context)
    }
}