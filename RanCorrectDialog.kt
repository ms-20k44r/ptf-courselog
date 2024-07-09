package *****

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.DialogFragment

class RanCorrectDialog : DialogFragment() {
    private lateinit var courseMap : CoursesMap
    private lateinit var listSugestwidRCD: ListView
    //(RanCor)Listener,実行するメソッドranCorrectPositiveButton()を定義したInterface
    private lateinit var listenerRanCor: RanCorrectDialogListener
    interface RanCorrectDialogListener {
        fun ranCorrectPositiveButton(correctCourse: String, ranposition: Int)
    }

    //onAttachの中でListenerにセットする
    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            //MainActivity(呼び出し元)を各Listenerに変換する
            val mainActivity: MainActivity = activity as MainActivity
            listenerRanCor = mainActivity
        } catch (e: ClassCastException) {
            throw ClassCastException((context.toString() + " must implement NoticeDialogListener"))
        }
    }//@onAttach()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        courseMap = CoursesMap(requireContext())
        // MainActivityでセットしたargumentsからranpositionの値を受け取る
        val ranpositionInt = arguments?.getInt("ranposition")

        val dialog = activity?.let {
            val layout = LayoutInflater.from(it)
                .inflate(R.layout.rancorrect_dialog, null)

            listSugestwidRCD = layout.findViewById<ListView>(R.id.listRCD)
            //検索サジェストとして表示されるリスト(初期は空)
            val adapterRCD = ArrayAdapter(requireContext(), R.layout.list_rcd, mutableListOf<String>())
            listSugestwidRCD.adapter = adapterRCD
            //検索ボックスに入力されたときの処理
            val svRCD = layout.findViewById<SearchView>(R.id.searchRCD)
            svRCD.setOnQueryTextListener(
                object : SearchView.OnQueryTextListener {
                    //入力中の内容が変更される度に呼ばれるメソッド
                    override fun onQueryTextChange(text: String?): Boolean {
                        adapterRCD.clear()
                        //ユーザーの入力候補と入力テキストが一致していれば、該当コースを(アダプターを介して)リストに追加
                        courseMap.myCoursesMap.values.forEach { stringImage ->
                            if (stringImage.courseSugest.contains(text?.uppercase())) {
                                val result = getString(stringImage.searchItemName)
                                adapterRCD.add(result)
                            }
                        }
                        refreshSvList(adapterRCD) //初期化メソッドを実行
                        return false
                    }//@入力中の内容が変更される度に呼ばれるメソッド
                    override fun onQueryTextSubmit(arg0: String?): Boolean {
                        return false
                    }
                }//@SearchView.OnQueryTextListener
            )//@svRCD.setOnQueryTextListener

            //サジェストのリスト項目をクリックした時の処理
            listSugestwidRCD.setOnItemClickListener { av, view, position, id ->

                //リスト項目をテキスト型で取得、変数courseCorrectItemに代入
                val courseCorrectItem = (view as TextView).text as String
                //Toast.makeText(this@MainActivity, "$courseCorrectItem", Toast.LENGTH_SHORT).show()

                //リスト項目によって該当コース名を表示する
                val correctcourse = layout.findViewById<TextView>(R.id.correctcourse)
                val result = findCourseName(courseCorrectItem)
                if (result != "null") {
                    correctcourse.setText(result)
                }

            }

            //背景をタップした時にソフトウェアキーボードを閉じる
            val constRCDview = layout.findViewById<ConstraintLayout>(R.id.constRCDview)
            constRCDview.setOnClickListener {
                val inputMethodManager = requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                val constRCDview = layout.findViewById<ConstraintLayout>(R.id.constRCDview)
                inputMethodManager.hideSoftInputFromWindow(constRCDview.windowToken, InputMethodManager.HIDE_NOT_ALWAYS)
                //背景全体にフォーカスを移動する
                constRCDview.requestFocus()
            }


            //上記でInflateしたレイアウトファイル内のウィジェットの取得と操作
            val dialogTitle = layout.findViewById<TextView>(R.id.dialogTitle)
            dialogTitle.text = ("${ranpositionInt.toString()}" + getString(R.string.title_ranCorrect))

            //ダイアログ生成
            AlertDialog.Builder(it).apply {
                setView(layout)
                //OKボタンの設定_Activityにコールバック
                setPositiveButton("変更") { _, _ ->
                    val correctcourse = layout.findViewById<TextView>(R.id.correctcourse)
                    val correctCoursename = correctcourse.text.toString()
                    if (ranpositionInt != null) {
                        listenerRanCor.ranCorrectPositiveButton(correctCoursename, ranpositionInt)
                    }
                }
                setNeutralButton("キャンセル") { dialog, which -> }
            }.create()

        } //@val dialog = activity?.let

        return dialog ?: throw IllegalStateException("Activity is null.")
    } //@onCreateDialog

    private fun findCourseName(searchItemNamesln: String): String {
        for ((key, value) in courseMap.myCoursesMap) { //myCoursesMapをループして一致するものを探す
            if (searchItemNamesln == getString(value.searchItemName)) {
                return getString(value.courseName)
            }
        }
        return "null" //一致するものが無ければnullを返す
    }

    //コース検索リストのリストアイテム初期化メソッド
    private fun refreshSvList(svAdapter: ArrayAdapter<String>) {
        var totalHeight = 0
        val sugeItem = svAdapter.getCount()
        if (sugeItem <= 4) {
            //個々のアイテムの高さを測り、加算していく
            for (i in 0 until svAdapter.getCount()) {
                val listItem = svAdapter.getView(i, null, listSugestwidRCD)
                listItem.measure(0, 0)
                totalHeight += listItem.getMeasuredHeight()
            }
            val params = listSugestwidRCD.getLayoutParams()
            params.height = totalHeight + (listSugestwidRCD.getDividerHeight() * (listSugestwidRCD.getCount() - 1))
            listSugestwidRCD.setLayoutParams(params)
        } else {
            for (i in 0 until 4) {
                val listItem = svAdapter.getView(i, null, listSugestwidRCD)
                listItem.measure(0, 0)
                totalHeight += listItem.getMeasuredHeight()
            }
            val params = listSugestwidRCD.getLayoutParams()
            params.height = (totalHeight * 7/8) + (listSugestwidRCD.getDividerHeight() * 3)
            listSugestwidRCD.setLayoutParams(params)
        }
    }


} //@RanCorrectDialog