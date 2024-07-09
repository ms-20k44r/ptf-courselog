package *****
import android.app.AlertDialog
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import net.s20k.m44r.mk8dxcourse.databinding.ActivitySubBinding

class SubActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySubBinding
    private lateinit var sharedPref: SharedPreferences
    private lateinit var viewModel: MainViewModel
    private lateinit var recyclerView: RecyclerView
    private lateinit var database: CourseDatabase
    private lateinit var matchDao: MatchDao

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //ViewBindingの設定
        binding = ActivitySubBinding.inflate(layoutInflater)
        setContentView(binding.root)
        Log.d("lifecycle", "SubActivity created")

        // SharedPreferencesの取得
        sharedPref = getSharedPreferences("*****", MODE_PRIVATE)

        //ViewModelのインスタンス生成
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        // CourseDatabaseの初期化
        database =CourseDatabase.getDatabase(this@SubActivity)
        matchDao = database.matchDao()

        //Viewの取得
        recyclerView = binding.rv
        recyclerView.layoutManager = LinearLayoutManager(this)
        val deleteButton = binding.deletebutton
        val spntype = binding.spntype
        val spntag = binding.spntag
        val subInfo1 = binding.subInfo1

        //スピナー2つにカスタムデザインを適用
        val adapterSpntype = ArrayAdapter(this, R.layout.custom_spinner_sub, resources.getStringArray(R.array.spntype))
        adapterSpntype.setDropDownViewResource(R.layout.custom_spinner_dropdown_sub)
        spntype.adapter = adapterSpntype
        val adapterSpntag = ArrayAdapter(this, R.layout.custom_spinner_sub, resources.getStringArray(R.array.spntag))
        adapterSpntag.setDropDownViewResource(R.layout.custom_spinner_dropdown_sub)
        spntag.adapter = adapterSpntag

        //データ生成トリガー
        viewModel.getoriginalList()
        // LiveDataを監視し、更新があったら(＝非同期処理の完了後に)UIに適用させる
        viewModel.originalListLiveData.observe(this, Observer { originalList ->
            //リサイクラービューにデータをセットする(originalList)
            val adapter = RecyclerAdapter(originalList,this, viewModel)
            recyclerView.adapter = adapter
            //originalListが空であった場合、textEmptyViewに書き換え
            if (originalList.isEmpty()) {
                subInfo1.text = getString(R.string.dataFilterEmptytext)
            }
        })

        //削除ボタンクリックで、トグル選択を読み取ってクエリ -> dialog表示でOKなら削除＆トースト表示
        deleteButton.setOnClickListener {
            //トグルのインデックスを取得(名称も)
            val typeIndex = spntype.selectedItemPosition
            val tagIndex = spntag.selectedItemPosition
            val type = spntype.selectedItem.toString()
            val tag = spntag.selectedItem.toString()
//            Toast.makeText(this, "${typeIndex}：${tagIndex}",Toast.LENGTH_SHORT).show()
            val job = viewModel.viewModelScope.launch {
                val matches: Any = when {
                    //もし形式が'全て(4)'かつタグが'全て(3)'だったら、全てのレコード件数を取得するクエリ→getMatchesCount
                    typeIndex == 4 && tagIndex == 3 -> matchDao.getMatchesDeleteAll()
                    //もし形式が'全て(4)'だったら、タグが一致するレコードを取得するクエリ→getMatchesByTag
                    typeIndex == 4 -> matchDao.getMatchesByTag(tagIndex)
                    //もしタグが'全て(3)'だったら、形式が一致するレコードを取得するクエリ→getMatchesByType
                    tagIndex == 3 -> matchDao.getMatchesByType(typeIndex)
                    //そうでなければ、形式とタグが一致するレコードを取得するクエリ→getMatchesByTypeAndTag
                    else -> matchDao.getMatchesByTypeAndTag(typeIndex,tagIndex)
                }
                //データセットの件数を取得
                val recordCount = if (typeIndex == 4 && tagIndex == 3) {
                    matches as Int
                } else {
                    matches as List<Match>
                    matches.size
                }
                //dialogに条件と件数を表示 削除OKで、データセットを@delete、完了後にトースト表示＆リサイクラービューデータの再取得＆シェアプリtrue。
                if (recordCount != 0) { //該当レコードが見つかった場合
                    val dialog = AlertDialog.Builder(this@SubActivity)
                        .setTitle("条件指定削除")
                        .setMessage("条件：$type かつ $tag\n該当：$recordCount 件\n全て削除しますか？")
                        .setPositiveButton("削除") { _, _ ->
                            // 削除を実行する -> リサイクラービューの再設定＆シェアプリtrueに。
                            viewModel.viewModelScope.launch {
                                if (typeIndex == 4 && tagIndex == 3) { //全件削除の場合
                                    matchDao.deleteAllrecords()
                                    Toast.makeText(this@SubActivity, "削除が完了しました", Toast.LENGTH_SHORT).show()
                                    listReset()
                                } else { //一部削除の場合
                                    val matchesArray = (matches as List<Match>).toTypedArray()
                                    matchDao.deleteAll(*matchesArray)
                                    Toast.makeText(this@SubActivity, "削除が完了しました", Toast.LENGTH_SHORT).show()
                                    listReset()
                                }
                            }
                        }
                        .setNeutralButton("キャンセル",null)
                        .setCancelable(false)
                        .create()
                    dialog.show()
                } else { //該当レコードが見つからなかった場合は通知のみ
                    val dialog = AlertDialog.Builder(this@SubActivity)
                        .setTitle("条件指定削除")
                        .setMessage("条件：$type かつ $tag\n該当：$recordCount 件\n該当するレコードはありません。")
                        .setPositiveButton("OK",null)
                        .setCancelable(false)
                        .create()
                    dialog.show()
                }
            //エラー処理
            }.also { job ->
                job.invokeOnCompletion { throwable ->
                    if (throwable is CancellationException) {
                        // ジョブがキャンセルされた場合にログを残す
                        Log.d("shar", "Job cancelled -> 条件指定削除")
                    } else if (throwable != null) {
                        // 例外が発生した場合の処理をここに記述
                        Log.e("shar", "An error occurred", throwable)
                    }
                }
            }

        } //～deleteButton.setOnClickListener

        binding.hatenabtn.setOnClickListener {
            val dialog = HatenaSubDialog()
            dialog.show(supportFragmentManager, "dialog_hatena_sub")
        }

        //homeボタンのクリックでSubActivityを終了(このアクティビティは@MainAcからしか呼び出されない)
        binding.homebtnSubAc.setOnClickListener {
            finish()
        }

    } //～onCreate()

    //リサイクラービューのデータ再読み込み(一括削除後の再設定)
    fun listReset() {
        //データ生成トリガー
        viewModel.getoriginalList()
        // LiveDataを監視し、更新があったら(＝非同期処理の完了後に)UIに適用させる
        viewModel.originalListLiveData.observe(this, Observer { originalList ->
            //リサイクラービューにデータをセットする(originalList)
            val adapter = RecyclerAdapter(originalList,this, viewModel)
            recyclerView.adapter = adapter
            //originalListが空であった場合、textEmptyViewに書き換え
            if (originalList.isEmpty()) {
                val subInfo1 = binding.subInfo1
                subInfo1.text = getString(R.string.dataFilterEmptytext)
            }
        })
    }

}