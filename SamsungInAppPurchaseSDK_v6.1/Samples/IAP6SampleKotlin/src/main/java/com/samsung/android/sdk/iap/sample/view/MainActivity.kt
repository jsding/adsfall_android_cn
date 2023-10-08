package com.samsung.android.sdk.iap.sample.view

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.TransitionDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.samsung.android.sdk.iap.sample.contract.GunLevel
import com.samsung.android.sdk.iap.sample.contract.ItemID
import com.samsung.android.sdk.iap.sample.contract.MainContract
import com.samsung.android.sdk.iap.sample.presenter.MainPresenter
import com.samsung.android.sdk.iap.v6.sample2.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainContract.View {
    private val presenter: MainContract.Presenter = MainPresenter(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        presenter.setContext(applicationContext)
        presenter.getGunBulletStatus()
        presenter.getOwnedList()
    }

    override fun onStop() {
        super.onStop()
        presenter.saveGunBulletStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        presenter.disposeIapHelper()
    }

    override fun showBulletCount(count: Int) {
        textBulletCnt.visibility = View.VISIBLE
        textMaxBullet.visibility = View.VISIBLE
        textInfiniteBullet.visibility = View.INVISIBLE
        textBulletCnt.text = count.toString()
    }

    override fun showInfiniteBullet() {
        textBulletCnt.visibility = View.INVISIBLE
        textMaxBullet.visibility = View.INVISIBLE
        textInfiniteBullet.visibility = View.VISIBLE
        textInfiniteBullet.setTextColor(Color.WHITE)
    }

    override fun showChangedInfiniteBullet() {
        textBulletCnt.visibility = View.INVISIBLE
        textMaxBullet.visibility = View.INVISIBLE
        textInfiniteBullet.visibility = View.VISIBLE
        textInfiniteBullet.setTextColor(Color.RED)
    }

    override fun showGunImage(level: GunLevel) {
        when (level) {
            GunLevel.NORMAL -> {
                imageGun01.visibility = View.VISIBLE
                imageGun02.visibility = View.INVISIBLE
            }
            GunLevel.UPGRADE -> {
                imageGun01.visibility = View.INVISIBLE
                imageGun02.visibility = View.VISIBLE
            }
        }
    }

    override fun showToastMessage(message: String) {
        val toast = Toast.makeText(this.applicationContext, message, Toast.LENGTH_SHORT)
        toast.show()
    }

    override fun showShotAnimation() {
        val drawable: TransitionDrawable = imageViewShot.drawable as TransitionDrawable
        drawable.startTransition(500)
        drawable.reverseTransition(500)
    }

    fun onClick(view: View) {
        when (view) {
            btn_shot -> presenter.shotGun()
            btn_get_a_bullet -> presenter.purchaseItem(ItemID.CONSUMABLE)
            btn_upgrade_the_gun -> presenter.purchaseItem(ItemID.NONCONSUMABLE)
            btn_get_infinite_bullets -> presenter.purchaseItem(ItemID.SUBSCRIPTION)
            buttonProductsDetails -> {
                val intent = Intent(this, ProductsDetailsActivity::class.java)
                intent.putExtra("ProductIds", "")
                startActivity(intent)
            }
        }
    }
}