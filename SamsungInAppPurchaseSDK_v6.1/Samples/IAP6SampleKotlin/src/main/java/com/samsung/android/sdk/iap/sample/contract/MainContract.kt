package com.samsung.android.sdk.iap.sample.contract

import android.content.Context

interface MainContract {
    interface View {
        fun showBulletCount(count: Int)
        fun showGunImage(level: GunLevel)
        fun showInfiniteBullet()
        fun showChangedInfiniteBullet()
        fun showToastMessage(message: String)
        fun showShotAnimation()
    }

    interface Presenter {
        fun disposeIapHelper()
        fun getGunBulletStatus()
        fun getOwnedList()
        fun purchaseItem(itemId: ItemID)
        fun saveGunBulletStatus()
        fun setContext(context: Context)
        fun shotGun()
    }
}

enum class ItemID(val id: String) {
    CONSUMABLE("consumable"),
    NONCONSUMABLE("non-consumable"),
    SUBSCRIPTION("ARS"),
    SUBSCRIPTION2("ARS2")
}

enum class GunLevel(var level: Int) {
    NORMAL(1), UPGRADE(2)
}

enum class InfiniteBulletLevel(var level: Int) {
    NONE(0), NORMAL(1), UPGRADE(2)
}
