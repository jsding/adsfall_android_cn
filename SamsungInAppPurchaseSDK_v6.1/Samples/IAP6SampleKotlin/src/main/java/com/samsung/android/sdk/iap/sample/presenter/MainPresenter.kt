package com.samsung.android.sdk.iap.sample.presenter

import android.content.Context
import android.util.Log
import com.samsung.android.sdk.iap.lib.helper.HelperDefine.IAP_ERROR_NETWORK_NOT_AVAILABLE
import com.samsung.android.sdk.iap.lib.helper.IapHelper
import com.samsung.android.sdk.iap.lib.vo.ConsumeVo
import com.samsung.android.sdk.iap.lib.vo.ErrorVo
import com.samsung.android.sdk.iap.lib.vo.OwnedProductVo
import com.samsung.android.sdk.iap.lib.vo.PurchaseVo
import com.samsung.android.sdk.iap.sample.contract.GunLevel
import com.samsung.android.sdk.iap.sample.contract.InfiniteBulletLevel
import com.samsung.android.sdk.iap.sample.contract.ItemID
import com.samsung.android.sdk.iap.sample.contract.MainContract
import com.samsung.android.sdk.iap.sample.model.GunBulletData
import com.samsung.android.sdk.iap.sample.model.ItemRepository
import com.samsung.android.sdk.iap.sample.model.MainInteractor
import java.util.*

class MainPresenter(private val view: MainContract.View) : MainContract.Presenter {
    private lateinit var mainInteractor: MainInteractor
    private lateinit var itemRepository: ItemRepository

    private val CONSUME_SUCCESS_CODE = 0
    private val PASS_THROUGH_PARAM = "TEMP_PASS_THROUGH"
    private val TAG = MainPresenter::class.java.simpleName

    private var bulletCount: Int = 0
    private var gunLevel: GunLevel = GunLevel.NORMAL
    private var infiniteBulletLevel: InfiniteBulletLevel = InfiniteBulletLevel.NONE

    override fun disposeIapHelper() {
        mainInteractor.disposeIapHelper()
    }

    override fun getOwnedList() {
        mainInteractor.getOwnedList(IapHelper.PRODUCT_TYPE_ALL) onGetOwnedProducts@{ _errorVo: ErrorVo?, _ownedList: ArrayList<OwnedProductVo>? ->
            Log.d(TAG, "onGetOwnedProducts is invoked")
            if (_errorVo == null) return@onGetOwnedProducts
            if (_errorVo.errorCode == IapHelper.IAP_ERROR_NONE) {
                if (_ownedList == null) return@onGetOwnedProducts
                var purchaseIds: String = ""
                for (item in _ownedList) {
                    if (item.isConsumable) {
                        Log.d(TAG, "Request to consume an item: ${item.itemId}, ${item.purchaseId}")
                        if (purchaseIds.isNotEmpty()) {
                            purchaseIds += ","
                        }
                        purchaseIds += item.purchaseId
                    } else {
                        when (item.itemId) {
                            ItemID.NONCONSUMABLE.id -> {
                                Log.d(TAG, "The gun was upgraded")
                                gunLevel = GunLevel.UPGRADE
                                view.showGunImage(gunLevel)
                            }
                            ItemID.SUBSCRIPTION.id -> {
                                Log.d(TAG, "The infinite bullet was purchased")
                                infiniteBulletLevel = InfiniteBulletLevel.NORMAL
                                view.showInfiniteBullet()
                            }
                            ItemID.SUBSCRIPTION2.id -> {
                                Log.d(TAG, "The upgraded infinite bullet was purchased")
                                infiniteBulletLevel = InfiniteBulletLevel.UPGRADE
                                view.showChangedInfiniteBullet()
                            }
                        }
                    }
                }
                if (purchaseIds.isNotEmpty()) {
                    consumeItem(purchaseIds)
                }
                saveGunBulletStatus()
            } else {
                Log.e(TAG, "onGetOwnedProducts > ErrorCode [${_errorVo.errorCode}]")
                Log.e(TAG, "onGetOwnedProducts > ErrorString [${_errorVo.errorString ?: ""}]")
            }
        }
    }

    override fun purchaseItem(itemId: ItemID) {
        if ((itemId == ItemID.CONSUMABLE) && (bulletCount >= 5 || infiniteBulletLevel.level > 0)) {
            view.showToastMessage("You already have max bullets!")
            return
        }

        mainInteractor.purchaseItem(itemId.id) onPayment@{ _errorVo: ErrorVo?, _purchaseVo: PurchaseVo? ->
            Log.d(TAG, "onPayment is invoked")
            if (_errorVo == null) return@onPayment
            if (_errorVo.errorCode == IapHelper.IAP_ERROR_NONE) {
                if (_purchaseVo == null) return@onPayment
                if (_purchaseVo.passThroughParam != null) {
                    if (PASS_THROUGH_PARAM.equals(_purchaseVo.passThroughParam)) {
                        Log.d(TAG, "passThroughParam is MATCHED")
                    } else {
                        Log.e(TAG, "passThroughParam is MISMATCHED")
                    }
                }

                when (_purchaseVo.itemId) {
                    ItemID.CONSUMABLE.id -> {
                        Log.d(
                                TAG,
                                "Request to consume an item: ${_purchaseVo.itemId}, ${_purchaseVo.purchaseId}"
                        )
                        consumeItem(_purchaseVo.purchaseId)
                    }
                    ItemID.NONCONSUMABLE.id -> {
                        Log.d(TAG, "The gun has been upgraded")
                        gunLevel = GunLevel.UPGRADE
                        view.showGunImage(gunLevel)
                    }
                    ItemID.SUBSCRIPTION.id -> {
                        Log.d(TAG, "The infinite bullet has been purchased")
                        infiniteBulletLevel = InfiniteBulletLevel.NORMAL
                        view.showInfiniteBullet()
                    }
                    else -> {
                        Log.e(TAG, "Unsupported Item ID, ${_purchaseVo.itemId}")
                        return@onPayment
                    }
                }
            } else {
                Log.e(TAG, "onPayment > ErrorCode [${_errorVo.errorCode}]")
                Log.e(TAG, "onPayment > ErrorString [${_errorVo.errorString ?: ""}]")
                // In case of network error from GalaxyStore 4.5.20.7 version and IAP SDK 6.1 version,
                // IAP error popup is not displayed.
                // As needed, the app can display network error to users.
                if (_errorVo.errorCode == IAP_ERROR_NETWORK_NOT_AVAILABLE) {
                    view.showToastMessage(_errorVo.errorString)
                }
            }
        }
    }

    private fun consumeItem(purchaseId: String) {
        mainInteractor.consumeItem(purchaseId) onConsumePurchasedItems@{ _errorVo: ErrorVo?, _consumeList: ArrayList<ConsumeVo>? ->
            Log.d(TAG, "onConsumePurchasedItems is invoked")
            if (_errorVo == null) return@onConsumePurchasedItems
            if (_errorVo.errorCode == IapHelper.IAP_ERROR_NONE) {
                if (_consumeList == null) return@onConsumePurchasedItems
                for (item in _consumeList) {
                    if (item.statusCode == CONSUME_SUCCESS_CODE) {
                        Log.d(TAG, "onConsumePurchasedItems > Plus a bullet")
                        view.showBulletCount(++bulletCount)
                    }
                }
            } else {
                Log.e(TAG, "onConsumePurchasedItems > ErrorCode [${_errorVo.errorCode}]")
                Log.e(TAG, "onConsumePurchasedItems > ErrorString [${_errorVo.errorString ?: ""}]")
            }
        }
    }

    override fun setContext(context: Context) {
        mainInteractor = MainInteractor(context)
        itemRepository = ItemRepository(context)
    }

    override fun shotGun() {
        if (infiniteBulletLevel == InfiniteBulletLevel.NONE) {
            if (bulletCount == 0) {
                view.showToastMessage("You are out of bullets! Try get some!")
                return
            } else {
                view.showBulletCount(--bulletCount)
            }
        }
        view.showShotAnimation()
    }

    override fun getGunBulletStatus() {
        val data: GunBulletData = itemRepository.getPreference()

        if (data.infiniteBulletLevel > 0) {
            when (data.infiniteBulletLevel) {
                1 -> view.showInfiniteBullet()
                2 -> view.showChangedInfiniteBullet()
            }
            infiniteBulletLevel.level = data.infiniteBulletLevel
        } else {
            view.showBulletCount(data.bulletCount)
            bulletCount = data.bulletCount
        }

        when (data.gunLevel) {
            1 -> view.showGunImage(GunLevel.NORMAL)
            2 -> view.showGunImage(GunLevel.UPGRADE)
        }
        gunLevel.level = data.gunLevel
    }

    override fun saveGunBulletStatus() {
        itemRepository.setPreference(GunBulletData(bulletCount, gunLevel.level, infiniteBulletLevel.level))
    }
}