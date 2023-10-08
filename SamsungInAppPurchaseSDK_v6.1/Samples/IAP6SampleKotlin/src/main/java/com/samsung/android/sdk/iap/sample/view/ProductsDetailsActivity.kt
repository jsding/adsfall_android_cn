package com.samsung.android.sdk.iap.sample.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.sample.contract.ProductsDetailsContract
import com.samsung.android.sdk.iap.sample.presenter.ProductsDetailsPresenter
import com.samsung.android.sdk.iap.v6.sample2.R
import kotlinx.android.synthetic.main.activity_products_details.noDataText
import kotlinx.android.synthetic.main.activity_products_details.productList
import java.util.ArrayList

class ProductsDetailsActivity : AppCompatActivity(), ProductsDetailsContract.View {
    private val KEY_PRODUCT_IDS = "ProductIds"
    private val presenter: ProductsDetailsContract.Presenter = ProductsDetailsPresenter(this)
    private val adapterItems: ArrayList<ProductVo> = ArrayList<ProductVo>()
    private lateinit var adapter: ProductDetailsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_products_details)
        initView()

        val intent: Intent = intent
        if (intent.extras?.containsKey(KEY_PRODUCT_IDS) as Boolean) {
            val productIds = intent.extras?.getString(KEY_PRODUCT_IDS) as String
            presenter.setContext(applicationContext)
            presenter.getProductsDetails(productIds)
        } else {
            Toast.makeText(
                this,
                R.string.mids_sapps_pop_an_invalid_value_has_been_provided_for_samsung_in_app_purchase,
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    override fun showProductsDetails(productList: ArrayList<ProductVo>) {
        adapterItems.addAll(productList)
        adapter.notifyDataSetChanged()
    }

    private fun initView() {
        adapter = ProductDetailsAdapter(this, R.layout.products_details_list, adapterItems)
        productList.adapter = adapter
        productList.emptyView = noDataText
        productList.visibility = View.GONE
    }
}