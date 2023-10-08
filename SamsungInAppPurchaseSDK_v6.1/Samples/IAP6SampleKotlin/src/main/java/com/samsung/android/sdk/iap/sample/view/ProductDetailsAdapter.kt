package com.samsung.android.sdk.iap.sample.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.samsung.android.sdk.iap.lib.vo.ProductVo
import com.samsung.android.sdk.iap.v6.sample2.R

class ProductDetailsAdapter(
    context: Context,
    private var resId: Int,
    private var items: ArrayList<ProductVo>) : ArrayAdapter<ProductVo>(context, resId, items) {
    private class ViewHolder {
        var productName: TextView? = null
        var productPriceString: TextView? = null
        var productType: TextView? = null
        var productDescription: TextView? = null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(resId, null)
            viewHolder = ViewHolder()
            viewHolder.productName = view.findViewById(R.id.productName)
            viewHolder.productPriceString = view.findViewById(R.id.productPriceString)
            viewHolder.productType = view.findViewById(R.id.productType)
            viewHolder.productDescription = view.findViewById(R.id.productDescription)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val product: ProductVo = items[position]

        viewHolder.productName?.text = product.itemName
        viewHolder.productPriceString?.text = product.itemPriceString
        viewHolder.productType?.text = "Type: " + product.type
        viewHolder.productDescription?.text = product.itemDesc

        return view
    }
}