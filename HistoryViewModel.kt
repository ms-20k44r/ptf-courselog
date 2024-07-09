package *****

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    val database = CourseDatabase.getDatabase(app)
    val matchDao = database.matchDao()
    val raceDao = database.raceDao()
    var sharedPref = getApplication<Application>().getSharedPreferences("*****",
        AppCompatActivity.MODE_PRIVATE)
    val courseMap = CoursesMap(app.applicationContext)
    val app = app
    //ユーザーのタイムゾーンを取得　文字列型のidで保存してある。使用する時はZoneIdオブジェクトに変換。val userTimeZone = ZoneId.of(userZoneId)
    val userZoneId = sharedPref.getString("userZoneId",ZoneId.systemDefault().id).toString()
    val userTimeZone = ZoneId.of(userZoneId)

    //全体データの取得 #onCreateで実行
    private val _hisAllListLiveData = MutableLiveData<List<xMatchWithRace>>()
    val hisAllListLiveData: LiveData<List<xMatchWithRace>> = _hisAllListLiveData
    //2v2(type_code＝0)のデータ
    private val _his2v2ListLiveData = MutableLiveData<List<xMatchWithRace>>()
    val his2v2ListLiveData: LiveData<List<xMatchWithRace>> = _his2v2ListLiveData
    //FFA(type_code＝1)のデータ
    private val _hisFFAListLiveData = MutableLiveData<List<xMatchWithRace>>()
    val hisFFAListLiveData: LiveData<List<xMatchWithRace>> = _hisFFAListLiveData
    //6v6(type_code＝2)のデータ
    private val _his6v6ListLiveData = MutableLiveData<List<xMatchWithRace>>()
    val his6v6ListLiveData: LiveData<List<xMatchWithRace>> = _his6v6ListLiveData
    //others(type_code＝3)のデータ
    private val _hisOthersListLiveData = MutableLiveData<List<xMatchWithRace>>()
    val hisOthersListLiveData: LiveData<List<xMatchWithRace>> = _hisOthersListLiveData
    //形式別＿総試合数 -> レース数が8以上12以下のMatch件数
    //2v2(type_code＝0)
    private var _totalMatchCount2v2LiveData = MutableLiveData<Int>()
    var totalMatchCount2v2LiveData: LiveData<Int> = _totalMatchCount2v2LiveData
    //FFA(type_code＝1)
    private var _totalMatchCountFFALiveData = MutableLiveData<Int>()
    var totalMatchCountFFALiveData: LiveData<Int> = _totalMatchCountFFALiveData
    //6v6(type_code＝2)
    private var _totalMatchCount6v6LiveData = MutableLiveData<Int>()
    var totalMatchCount6v6LiveData: LiveData<Int> = _totalMatchCount6v6LiveData
    //others(type_code＝3)
    private var _totalMatchCountothersLiveData = MutableLiveData<Int>()
    var totalMatchCountothersLiveData: LiveData<Int> = _totalMatchCountothersLiveData
    //形式別＿12レースの平均スコア
    private var _avgScore12for2v2LiveData = MutableLiveData<Int>()
    var avgScore12for2v2LiveData: LiveData<Int> = _avgScore12for2v2LiveData
    private var _avgScore12forFFALiveData = MutableLiveData<Int>()
    var avgScore12forFFALiveData: LiveData<Int> = _avgScore12forFFALiveData
    private var _avgScore12for6v6LiveData = MutableLiveData<Int>()
    var avgScore12for6v6LiveData: LiveData<Int> = _avgScore12for6v6LiveData
    private var _avgScore12forothersLiveData = MutableLiveData<Int>()
    var avgScore12forothersLiveData: LiveData<Int> = _avgScore12forothersLiveData
    //形式別＿8レースの平均スコア
    private var _avgScore8for2v2LiveData = MutableLiveData<Int>()
    var avgScore8for2v2LiveData: LiveData<Int> = _avgScore8for2v2LiveData
    private var _avgScore8forFFALiveData = MutableLiveData<Int>()
    var avgScore8forFFALiveData: LiveData<Int> = _avgScore8forFFALiveData
    private var _avgScore8for6v6LiveData = MutableLiveData<Int>()
    var avgScore8for6v6LiveData: LiveData<Int> = _avgScore8for6v6LiveData
    private var _avgScore8forothersLiveData = MutableLiveData<Int>()
    var avgScore8forothersLiveData: LiveData<Int> = _avgScore8forothersLiveData

    //グラフ生成用データ
    //形式別＿12レースのデータ
    private val _hisBarChart12for2v2LiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart12for2v2LiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart12for2v2LiveData
    private val _hisBarChart12forFFALiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart12forFFALiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart12forFFALiveData
    private val _hisBarChart12for6v6LiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart12for6v6LiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart12for6v6LiveData
    private val _hisBarChart12forothersLiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart12forothersLiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart12forothersLiveData
    //形式別＿8レースのデータ
    private val _hisBarChart8for2v2LiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart8for2v2LiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart8for2v2LiveData
    private val _hisBarChart8forFFALiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart8forFFALiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart8forFFALiveData
    private val _hisBarChart8for6v6LiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart8for6v6LiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart8for6v6LiveData
    private val _hisBarChart8forothersLiveData = MutableLiveData<Triple<List<Float>, List<Float>, List<String>>>()
    val hisBarChart8forothersLiveData: LiveData<Triple<List<Float>, List<Float>, List<String>>> = _hisBarChart8forothersLiveData


    fun gethisAllList() {
        //非同期でクエリ→データ加工
        viewModelScope.launch {
            val hisAllList = convertToFormattedList(app, matchDao.getHisMatchWithRaces())
//            Log.d("his", "hisAllList:${hisAllList}")
            _hisAllListLiveData.postValue(hisAllList)

            //取得加工した全体データを、形式ごとにフィルターを掛ける
            val his2v2List = hisAllList.filter { it.match.type_code == 0 }
            _his2v2ListLiveData.postValue(his2v2List)
            val hisFFAList = hisAllList.filter { it.match.type_code == 1 }
                _hisFFAListLiveData.postValue(hisFFAList)
            val his6v6List = hisAllList.filter { it.match.type_code == 2 }
                _his6v6ListLiveData.postValue(his6v6List)
            val hisothersList = hisAllList.filter { it.match.type_code == 3 }
                _hisOthersListLiveData.postValue(hisothersList)

            //全体データhisAllListが空データの場合は、以下の処理はしなくて良い
            if (hisAllList.isNotEmpty()) {
                //レース数＝12の形式別リストを用意
                val races12_2v2 = his2v2List.filter { it.Races.size == 12 }
                val races12_FFA = hisFFAList.filter { it.Races.size == 12 }
                val races12_6v6 = his6v6List.filter { it.Races.size == 12 }
                val races12_others = hisothersList.filter { it.Races.size == 12 }
                //レース数＝8の形式別リストを用意
                val races8_2v2 = his2v2List.filter { it.Races.size == 8 }
                val races8_FFA = hisFFAList.filter { it.Races.size == 8 }
                val races8_6v6 = his6v6List.filter { it.Races.size == 8 }
                val races8_others = hisothersList.filter { it.Races.size == 8 }
                //形式別＿総試合数をセット
                _totalMatchCount2v2LiveData.postValue(his2v2List.count { it.Races.size in 8..12 })
                _totalMatchCountFFALiveData.postValue(hisFFAList.count { it.Races.size in 8..12 })
                _totalMatchCount6v6LiveData.postValue(his6v6List.count { it.Races.size in 8..12 })
                _totalMatchCountothersLiveData.postValue(hisothersList.count { it.Races.size in 8..12 })
                //総合データタブ用の処理 -> 引数で形式コードと12レor8レを渡す 1.関数内で平均スコアとグラフデータをセット
                convertTotalDataList(races12_2v2.reversed(), 0,true)
                convertTotalDataList(races8_2v2.reversed(), 0,false)
                convertTotalDataList(races12_FFA.reversed(), 1,true)
                convertTotalDataList(races8_FFA.reversed(), 1,false)
                convertTotalDataList(races12_6v6.reversed(), 2,true)
                convertTotalDataList(races8_6v6.reversed(), 2,false)
                convertTotalDataList(races12_others.reversed(), 3,true)
                convertTotalDataList(races8_others.reversed(), 3,false)
            }

        }
    }


    //変換後のリスト定義
    data class xMatch(
        val match_id: Int,
        val type_code: Int,
        val tag_code: Int,
        val match_title: String,
        val dateTime: String,
        val score: Int
    )
    data class xRace (
        val race_id: Int,
        val course_code: String,
        val rank: Int,
        val race_num: Int,
        val mogi: Int
    )
    data class xMatchWithRace(
        val match: xMatch,
        val Races: List<xRace>
    )

    //リストの変換(日時とコース名とスコア計算)
    fun convertToFormattedList(app: Application, rowData: List<MatchWithRace>): List<xMatchWithRace> {
        if (rowData.isEmpty()) { // 該当する結果が0件である場合は空の＜変換後(x)リスト＞を返却
            return emptyList<xMatchWithRace>()
        } else {
            val formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm")
            var result: Int = 0
//            val sortedData = rowData.sortedByDescending { it.match.dateTime }
            //map関数で、rowDataの各要素に対して順次処理
            return rowData.map { matchWithRace ->
                // Instant型のdateTimeをローカルタイムに変換
                val userDateTime = matchWithRace.match.dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(userTimeZone)
                val formattedDateTime = formatter.format(userDateTime)
                // スコアの計算
                result = matchWithRace.Races.sumBy {
                    when (it.rank) {
                        1 -> 15
                        2 -> 12
                        3 -> 10
                        4 -> 9
                        5 -> 8
                        6 -> 7
                        7 -> 6
                        8 -> 5
                        9 -> 4
                        10 -> 3
                        11 -> 2
                        12 -> 1
                        else -> 0 // エラーハンドリングのため、rankが1から12以外の値の場合は0を加算する
                    }
                }

                val xMatch = xMatch(
                    matchWithRace.match.match_id,
                    matchWithRace.match.type_code,
                    matchWithRace.match.tag_code,
                    matchWithRace.match.match_title,
                    formattedDateTime,
                    result
                )
                val xRaces = matchWithRace.Races.map { race ->
//                    val courseName = courseCodeToNameMap[race.course_code] ?: race.course_code
                    val courseName = courseMap.myCoursesMap[race.course_code] ?.courseName.let {
                        app.applicationContext.getString(it!!)
                    }  ?: race.course_code
                    xRace(
                        race.race_id,
                        courseName,
                        race.rank,
                        race.race_num,
                        race.mogi
                    )
                }

                xMatchWithRace(xMatch, xRaces)
            } //~sortedData.map
        } //~else
    } //~convertToFormattedList関数

    //総合データビュー用のデータ変換　(受)形式コード別,12レ8レ別の逆順リスト -> 平均スコアとグラフデータの生成＆セット
    fun convertTotalDataList(matchWithRace12: List<xMatchWithRace>, type_code: Int, is12race: Boolean) {
//        Log.d("his", "now2v2:${matchWithRace12}")
        //スコアの合計を求める
        val totalScore = matchWithRace12.sumOf { it.match.score }
        //平均スコアの計算
        val avgScore = if (matchWithRace12.isNotEmpty()) {
            //少数は四捨五入で整数に (ここでは％計算ではないのでNot銀行丸め)
            (totalScore.toDouble() / matchWithRace12.size.toDouble()).roundToInt()
        } else {
            0 //リストが空の場合はスコア=0に設定
        }

        // グラフデータ(X軸=リスト要素数のFloat型インクリメント,Y軸=リストのスコアのFloat型)の生成
        val xx = List(matchWithRace12.size) { it.toFloat() + 1 }
        val yy = matchWithRace12.map { it.match.score.toFloat() }
        // グラフのX軸ラベルに設定する日付リストを生成(list:String型"yy/MM/dd HH:mm"含->reDateTime->formattedDate(String))
        val xLavelImmutable = xAxisLabeldataFormatter(matchWithRace12)

        //　LiveDataに格納する　(各タイプと12or8レースで全組み合わせ。引数によって判別)
        when {
            type_code == 0 && is12race -> { //形式：2v2　12レース
                //平均スコア
                _avgScore12for2v2LiveData.postValue(avgScore)
                //グラフデータ＆ラベルリスト
                _hisBarChart12for2v2LiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 0 && !is12race -> { //形式：2v2　8レース
                _avgScore8for2v2LiveData.postValue(avgScore)
                _hisBarChart8for2v2LiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 1 && is12race -> {
                _avgScore12forFFALiveData.postValue(avgScore)
                _hisBarChart12forFFALiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 1 && !is12race -> {
                _avgScore8forFFALiveData.postValue(avgScore)
                _hisBarChart8forFFALiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 2 && is12race -> {
                _avgScore12for6v6LiveData.postValue(avgScore)
                _hisBarChart12for6v6LiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 2 && !is12race -> {
                _avgScore8for6v6LiveData.postValue(avgScore)
                _hisBarChart8for6v6LiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 3 && is12race -> {
                _avgScore12forothersLiveData.postValue(avgScore)
                _hisBarChart12forothersLiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            type_code == 3 && !is12race -> {
                _avgScore8forothersLiveData.postValue(avgScore)
                _hisBarChart8forothersLiveData.postValue(Triple(xx, yy, xLavelImmutable))
            }
            else -> {}// type_codeとis12raceの組み合わせが存在しない場合の処理
        }

        Log.d("his","Triple:${Triple(xx,yy,xLavelImmutable)}")

    } //~convertTotalDataList関数

    // グラフのX軸ラベルに設定する日付リストを生成(list:String型"yy/MM/dd HH:mm"含->reDateTime->formattedDate(String))
    fun xAxisLabeldataFormatter(list: List<xMatchWithRace>): List<String> {
        val xLavel = mutableListOf<String>("先頭")
        var prevYear = "" //ループ処理の中で現地点の年度(yy)を保存
        val formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm")
        val dateFormatter = DateTimeFormatter.ofPattern("M/d H:mm")
        list.forEach {
            val reDateTime = LocalDateTime.parse(it.match.dateTime, formatter)
            val year = reDateTime.format(DateTimeFormatter.ofPattern("yy"))
            val formattedDate = if (year != prevYear) {
                "${reDateTime.format(dateFormatter)} (${year}年~)"
            } else {
                reDateTime.format(dateFormatter)
            }
            xLavel.add(formattedDate)
            prevYear = year
        }
        val xLavelImmutable: List<String> = xLavel.toList()
        return xLavelImmutable
    }

}