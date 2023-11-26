package com.android.client;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

public class SKUDetail {
  private String mSku;
  private String mType;
  private String mPrice;
  private long mPriceAmountMicros;
  private String mPriceCurrencyCode;
  private String mTitle;
  private String mDescription;
  private double usd;

  public SKUDetail(String type, String sku, String price, long priceAmountMicros, String currencyCode, String title, String description, double usd) {
    this.mType = type;
    this.mSku = sku;
    this.mPrice = price;
    this.mPriceAmountMicros = priceAmountMicros;
    this.mPriceCurrencyCode = currencyCode;
    this.mTitle = title;
    this.mDescription = description;
    this.usd = usd;
  }

  public double getUsd() {
    return usd;
  }


  public String getSku() {
    return mSku;
  }

  public String getPrice() {
    return mPrice;
  }

  public long getPriceAmountMicros() {
    return mPriceAmountMicros;
  }

  public String getPriceCurrencyCode() {
    return mPriceCurrencyCode;
  }

  public String getTitle() {
    return mTitle;
  }

  public String getDescription() {
    return mDescription;
  }

  @NonNull
  public JSONObject toJson() {
    JSONObject json = new JSONObject();
    try {
      json.put("id", this.mSku);
      json.put("type", this.mType);
      json.put("price", this.mPrice);
      json.put("price_amount", this.mPriceAmountMicros / 1000000.0f);
      json.put("currency", this.mPriceCurrencyCode);
      json.put("title", this.mTitle);
      json.put("desc", this.mDescription);
      json.put("usd", this.usd);
      return json;
    } catch (JSONException ignored) {
    }
    return json;
  }

  @NonNull
  @Override
  public String toString() {
    return toJson().toString();
  }
}
