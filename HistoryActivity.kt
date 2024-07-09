package *****

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import net.s20k.m44r.mk8dxcourse.databinding.ActivityHistoryBinding

class HistoryActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryBinding
    private lateinit var sharedPref: SharedPreferences
    private lateinit var viewModel: HistoryViewModel
    private lateinit var viewPager2: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //ViewBindingの設定
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("lifecycle", "HistoryActivity作成")

        // SharedPreferencesの取得
        sharedPref = getSharedPreferences("*****", MODE_PRIVATE)
        //ViewModelのインスタンス生成
        viewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        //Viewの取得
        viewPager2 = binding.viewPager2
        val tabLayout = binding.tabLayout
        val adapter = HistoryPagerAdapter(supportFragmentManager, lifecycle)

        //スピナー(cate)にカスタムデザインを適用
        val adapterSpnCate = ArrayAdapter(this, R.layout.custom_spinner_cate, resources.getStringArray(R.array.spnh3))
        adapterSpnCate.setDropDownViewResource(R.layout.custom_spinner_dropdown_cate)
        binding.spnh1.adapter = adapterSpnCate

        // タブの追加
        tabLayout.addTab(tabLayout.newTab().setText("形式別データ"))
        tabLayout.addTab(tabLayout.newTab().setText("試合の記録"))
        // ViewPagerのアダプターを設定
        viewPager2.adapter = adapter
        // ViewPagerのページ変更時にタブを切り替える
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback(){
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tabLayout.selectTab(tabLayout.getTabAt(position))
            }
        })
        // タブがクリックされた時にViewPagerのページを切り替える
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager2.currentItem = tab.position
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        //全体データの読み込み(大会タグの全レコードを取得)
        viewModel.gethisAllList()

        binding.hatenabtn.setOnClickListener {
            val dialog = HatenaHistoryDialog()
            dialog.show(supportFragmentManager, "dialog_hatena_history")
        }

        //スピナー(h1)に対してイベントリスナーを登録(アクティビティ切り替え)
        binding.spnh1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                //選択項目を取得
                when (position) { //0->大会ヒストリー 1->総合データ 2->コース別データ
                    1 -> { //1->総合データ
                        //HomeActivityに画面遷移する処理
                        val intent = Intent(this@HistoryActivity, HomeActivity::class.java)
                        startActivity(intent)
                    }
                    2 -> { //2->コース別データ
                        //CourseActivityに画面遷移する処理
                        val intent = Intent(this@HistoryActivity, CourseActivity::class.java)
                        startActivity(intent)

                    }
                    else -> { //0->大会ヒストリー
                        //何もしない
                    }
                }
            }
            override fun onNothingSelected(p0: AdapterView<*>?) { //項目が選択されなかった場合の処理(なし)
            }
        }

        //homeボタンのクリックでHistoryActivityを終了(MainActivityがonCreate。Home,Course,Historyのスタックを削除)
        binding.homebtnHistoryAc.setOnClickListener {
            val intent = Intent(this@HistoryActivity, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

    }

    //region lifecycle
    override fun onStart() {
        super.onStart()
        Log.d("lifecycle", "History_onStart")
    }

    override fun onRestart() {
        super.onRestart()
        Log.d("lifecycle", "History_onRestart★")
    }

    override fun onResume() {
        super.onResume()
        Log.d("lifecycle", "History_onResume")
        binding.spnh1.setSelection(0)
    }

    override fun onPause() {
        super.onPause()
        Log.d("lifecycle", "History_onPause")
    }

    override fun onStop() {
        super.onStop()
        Log.d("lifecycle", "History_onStop★")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("lifecycle", "History_破棄")
    }
    //endregion

}