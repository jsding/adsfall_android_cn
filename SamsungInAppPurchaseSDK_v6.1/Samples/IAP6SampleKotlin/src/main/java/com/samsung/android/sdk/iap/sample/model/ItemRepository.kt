package com.samsung.android.sdk.iap.sample.model

import android.content.Context
import android.util.Log

class ItemRepository(private val context: Context) {
    private val SP_FILE_NAME = "ShuttingBullet"
    private val SP_KEY_BULLET_COUNT = "BulletCount"
    private val SP_KEY_GUN_LEVEL = "GunLevel"
    private val SP_KEY_INFINITE_BULLET = "InfiniteBullet"
    private val TAG = ItemRepository::class.java.simpleName

    fun setPreference(data: GunBulletData) {
        val sharedPreferences = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(SP_KEY_BULLET_COUNT, data.bulletCount)
        editor.putInt(SP_KEY_GUN_LEVEL, data.gunLevel)
        editor.putInt(SP_KEY_INFINITE_BULLET, data.infiniteBulletLevel)
        editor.apply()
        Log.d(TAG, "Set Preference - bulletCount: ${data.bulletCount}, " +
                "level: ${data.gunLevel}, isInfinite: ${data.infiniteBulletLevel}")
    }

    fun getPreference(): GunBulletData {
        val sharedPreferences = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
        val bulletCount = sharedPreferences.getInt(SP_KEY_BULLET_COUNT, 5)
        val gunLevel = sharedPreferences.getInt(SP_KEY_GUN_LEVEL, 1)
        val infiniteBulletLevel = sharedPreferences.getInt(SP_KEY_INFINITE_BULLET, 0)
        Log.d(TAG, "Get Preference - bulletCount: $bulletCount, " +
                "level: $gunLevel, isInfinite: $infiniteBulletLevel")
        return GunBulletData(bulletCount, gunLevel, infiniteBulletLevel)
    }
}

data class GunBulletData(var bulletCount: Int, var gunLevel: Int, var infiniteBulletLevel: Int)
