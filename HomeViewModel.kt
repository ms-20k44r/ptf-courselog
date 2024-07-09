package *****

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.round

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    val database = CourseDatabase.getDatabase(app)
    val matchDao = database.matchDao()
    val raceDao = database.raceDao()
    var sharedPref = getApplication<Application>().getSharedPreferences("*****",
        AppCompatActivity.MODE_PRIVATE)
    //ユーザーのタイムゾーンを取得　文字列型のidで保存してある。使用する時はZoneIdオブジェクトに変換。val userTimeZone = ZoneId.of(userZoneId)
    val userZoneId = sharedPref.getString("userZoneId",ZoneId.systemDefault().id).toString()
    val userTimeZone = ZoneId.of(userZoneId)

    //全体データの取得 #onCreateで実行
    private val _homeAllListLiveData = MutableLiveData<List<MatchWithRace>>()
    val homeAllListLiveData: LiveData<List<MatchWithRace>> = _homeAllListLiveData
    //折れ線グラフ作成用データ
    private val _LineChartLiveData = MutableLiveData<Pair<List<Date>, List<Float>>>()
    val LineChartLiveData: LiveData<Pair<List<Date>, List<Float>>> = _LineChartLiveData
    //試合円グラフ・レース円グラフ　作成用データ
    private val _MatchPieChartLiveData = MutableLiveData<Map<String, Float>>()
    val MatchPieChartLiveData: LiveData<Map<String, Float>> = _MatchPieChartLiveData
    private val _RacePieChartLiveData = MutableLiveData<Map<String, Float>>()
    val RacePieChartLiveData: LiveData<Map<String, Float>> = _RacePieChartLiveData
    //試合円グラフ・レース円グラフ　補助データ
    private val _MatchCountDescrLiveData = MutableLiveData<Pair<List<Int>, Int>>()
    val MatchCountDescrLiveData: LiveData<Pair<List<Int>, Int>> = _MatchCountDescrLiveData
    private val _RaceCountDescrLiveData = MutableLiveData<Pair<List<Int>, Int>>()
    val RaceCountDescrLiveData: LiveData<Pair<List<Int>, Int>> = _RaceCountDescrLiveData
    //試合横棒帯グラフ・レース横棒帯グラフ　作成用データ
    private val _HBarMatchChartLiveData = MutableLiveData<Pair<List<Pair<String, Float>>, List<Int>>>()
    val HBarMatchChartLiveData: LiveData<Pair<List<Pair<String, Float>>, List<Int>>> = _HBarMatchChartLiveData
    private val _HBarRaceChartLiveData = MutableLiveData<Pair<List<Pair<String, Float>>, List<Int>>>()
    val HBarRaceChartLiveData: LiveData<Pair<List<Pair<String, Float>>, List<Int>>> = _HBarRaceChartLiveData

    fun gethomeAllList() {
        //非同期でクエリ→データ加工
        viewModelScope.launch {
            val homeAllList = matchDao.getAllMatchWithRaces()
            Log.d("home", "homeAllList:${homeAllList}")
            Log.d("home", "homeAllList総試合数:${homeAllList.size}")
            Log.d("home", "homeAllList総レース数:${homeAllList.flatMap { it.Races }.count()}")
            _homeAllListLiveData.postValue(homeAllList)

            //もし全体の取得データhomeAllListが空だったら、以降の処理は必要ない
            if (homeAllList.size != 0) {
                val homeAllList2 = convertToFormattedList(matchDao.getAllMatchWithRaces())
//            Log.d("home", "homeAllList2:${homeAllList2}")
                //　＜折れ線グラフ表示用データの生成＞ (関数内でLiveDataに格納)
                DatebyRacesLineChart(homeAllList)
                //　＜円グラフ [試合] 表示用データの生成＞homeAllListを元に加工 ＜横棒帯グラフ [試合] 表示用データの生成＞
                MatchPieChart(homeAllList)
                //　＜円グラフ [レース] 表示用データの生成＞homeAllListから情報生成
                RacePieHBarChart(homeAllList)
                //　＜横棒帯グラフ [レース] 表示用データの生成＞
                RaceHBarChart(homeAllList)
            }

        } //~launch
    } //~gethomeAllList()

    //変換後のリスト定義(接頭辞x)
    data class xMatch(
        val type_code: Int,
        val tag_code: Int,
        val dateTime: String
    )
    data class xRace (
        val race_num: Int,
    )
    data class xMatchWithRace(
        val match: xMatch,
        val Races: List<xRace>
    )

    //リストの変換(日時とコース名)
    fun convertToFormattedList(rowData: List<MatchWithRace>): List<xMatchWithRace> {
        if (rowData.isEmpty()) { // 該当する結果が0件である場合は空の＜変換後(x)リスト＞を返却
            return emptyList<xMatchWithRace>()
        } else {
            val formatter = DateTimeFormatter.ofPattern("yy/MM/dd HH:mm")

            return rowData.map { matchWithRace ->
                val userDateTime = matchWithRace.match.dateTime.atZone(ZoneOffset.UTC).withZoneSameInstant(userTimeZone)
                val formattedDateTime = formatter.format(userDateTime)
                val xMatch = xMatch(
                    matchWithRace.match.type_code,
                    matchWithRace.match.tag_code,
                    formattedDateTime
                )
                val xRaces = matchWithRace.Races.map { race ->
                    xRace(
                        race.race_num,
                    )
                }
                xMatchWithRace(xMatch, xRaces)
            }
        }
    }

    //折れ線グラフ表示用データの生成
    fun DatebyRacesLineChart(homeAllList: List<MatchWithRace>) {
        //日付のみのリスト、ユーザータイムゾーン
        val x = mutableListOf<Date>()
        val userTimeZone = TimeZone.getDefault()
        //日付ごとにレース数を集計するためのMap
        val raceCountMap = mutableMapOf<Date, Float>()
        //日付(String型)のフォーマットを指定
        val dateFormat = SimpleDateFormat("yy/M/d", Locale.getDefault())
        dateFormat.timeZone = userTimeZone //これは日付(String型)変換用
        // リストを順次処理
        for (matchWithRace in homeAllList) {
            // Instant型をユーザータイムゾーンのDate型に変換　Instant→ユーザーZonedDateTime→withで時間以降切り捨て(00:00:00)→instant→Date型
            val formattedDate: Date = Date.from(matchWithRace.match.dateTime.atZone(userTimeZone.toZoneId()).with(LocalTime.MIDNIGHT).toInstant())
            //日付(String型)を生成
            val dateString = dateFormat.format(Date.from(matchWithRace.match.dateTime))
            // 日付がリストxに含まれていなければ追加
            if (!x.contains(formattedDate)) {
                x.add(formattedDate)
            }
            // 日付ごとのレース数を集計
            val raceCount = raceCountMap[formattedDate] ?: 0f
            val newRaceCount = raceCount + matchWithRace.Races.size.toFloat()
            raceCountMap[formattedDate] = newRaceCount
        } //~for
        val xlist: List<Date> = x.toList()
        //日付ごとに集計したレース数のリスト
        val ylist = raceCountMap.values.toList()

        //  LiveDataにグラフ表示用データを格納
        _LineChartLiveData.postValue(Pair(xlist,ylist))
        Log.d("home", "ChartPair:${Pair(xlist,ylist)}")
    }

    //円グラフ [試合] 表示用データの生成
    fun MatchPieChart(homeAllList: List<MatchWithRace>) {
        //Raceの要素数が8以上のMatch(WithRace)＝”試合”だけを抽出 該当するものが存在しない場合空のリスト
        val filteredMatches = homeAllList.filter { matchWithRace ->
            matchWithRace.Races.size >= 8
        }

        if (filteredMatches.isEmpty()) {
            // 該当するMatchが存在しなかった場合の処理
            //LiveDataに空のリストを渡す
            val resultEmpty: Map<String, Float> = mapOf()
            _MatchPieChartLiveData.postValue(resultEmpty)

            // 試合横棒帯グラフMatchHBarChart(filteredMatches)を呼び出さないで、そっちのLiveDataにも空リスト渡す
            val resultEmptyy: List<Pair<String, Float>> = listOf()
            val resultEmptyyy = listOf<Int>()

            _HBarMatchChartLiveData.postValue(Pair(resultEmptyy, resultEmptyyy))

        } else {
            //tag_codeごとにMatchをグループ化して、グループ化されたMatchの数を取得
            val tagCodeToMatchCountMap = filteredMatches
                .groupBy { it.match.tag_code }
                .mapValues { it.value.size }

            //tag_codeごとのマッチ数のリスト
            val matchCountList = tagCodeToMatchCountMap.values.toList()
            //総マッチ数
            val totalCount = tagCodeToMatchCountMap.values.sum()

            //MapのKey：tag_codeの値を特定のString型に変換 ＆ MapのValue：計算した(tag_codeごとの)割合
            val tagCodeToRatioMap = tagCodeToMatchCountMap
                .mapKeys { it -> when (it.key) {
                    0 -> "模擬"
                    1 -> "大会"
                    2 -> "野良"
                    else -> error("Unexpected tag code: ${it.key}")
                }} //(以下)各tag_codeの試合数を全体の試合数で割ることで、tag_codeごとの試合数の比率を求める
                .mapValues { round(it.value.toFloat() / totalCount * 100) } //round()で銀行丸め
//            .toList().sortedByDescending { (_, ratio) -> ratio }.toMap() //値が大きい順番にソートして格納(不要)

            //※タグごとの試合数の比率を確認表示
            tagCodeToRatioMap.forEach { (tagCode, ratio) ->
                Log.d("home", "$tagCode の試合数の比率: $ratio")
//            println("tag_code $tagCode の試合数の比率: $ratio")
            }

            //  LiveDataにグラフ表示用データを格納
            _MatchCountDescrLiveData.postValue(Pair(matchCountList, totalCount))
            Log.d("home", "MatchPiePairDescr:${Pair(matchCountList, totalCount)}")
            _MatchPieChartLiveData.postValue(tagCodeToRatioMap)
            Log.d("home", "MatchPiePair:${tagCodeToRatioMap}")

            //横棒帯グラフ [試合]作成のために、8レース以上のものを抽出したfilteredMatchesを渡す
            MatchHBarChart(filteredMatches)

        }

    }

    //円グラフ [レース] 表示用データの生成
    fun RacePieHBarChart(homeAllList: List<MatchWithRace>) {

        //tag_codeごとにMatchをグループ化して、グループ化されたMatchに含まれるレース数を取得
        val tagCodeToRaceCountMap = homeAllList
            .groupBy { it.match.tag_code }
            .mapValues { matches ->
                matches.value.sumBy { matchWithRace ->
                    matchWithRace.Races.size
                }
            }
        //tag_codeごとのレース数のリスト
        val raceCountList = tagCodeToRaceCountMap.values.toList()
        //総レース数
        val totalRaceCount = tagCodeToRaceCountMap.values.sum()

        //MapのKey：tag_codeの値を特定のString型に変換 ＆ MapのValue：計算した(tag_codeごとの)割合
        val tagCodeToRatioRaceMap = tagCodeToRaceCountMap
            .mapKeys { it -> when (it.key) {
                0 -> "模擬"
                1 -> "大会"
                2 -> "野良"
                else -> error("Unexpected tag code: ${it.key}")
            }} //(以下)各tag_codeのレース数を全体の試合数で割ることで、tag_codeごとの試合数の比率を求める
            .mapValues { round(it.value.toFloat() / totalRaceCount * 100) } //round()で銀行丸め

        //※tag_codeごとのレース数を表示
        tagCodeToRaceCountMap.forEach { (tagCode, count) ->
            Log.d("home", "tag_code $tagCode のレース数: $count")
        }
        //※タグごとのレース数の比率を確認表示
        tagCodeToRatioRaceMap.forEach { (tagCode, ratio) ->
            Log.d("home", "$tagCode のレース数の比率: $ratio")
        }

        //  LiveDataにグラフ表示用データを格納
        _RaceCountDescrLiveData.postValue(Pair(raceCountList, totalRaceCount))
        Log.d("home", "RacePiePairDescr:${Pair(raceCountList, totalRaceCount)}")
        _RacePieChartLiveData.postValue(tagCodeToRatioRaceMap)
        Log.d("home", "RacePiePair:${tagCodeToRatioRaceMap}")
    }

    //横棒帯グラフ [試合] 表示用データの生成
    fun MatchHBarChart(filteredMatches: List<MatchWithRace>) {
        //※8レース以上の試合が渡ってきている　空のリストではない。
        val nullTypeEmptyAddMap = makeNullTypeEmptyAddMap(filteredMatches)

        //形式ごとのMatch数を取得
        val TypeWithMatchCountMap = nullTypeEmptyAddMap
            .mapValues { (_, matches) ->
            matches.size
        }
        //Valueの値の降順(大きい順)にソート　※以下のtoList()では、Mapの各要素をPair<キー,値>に変換したリストになる
        val sortedTypeWithMatchCountMap = TypeWithMatchCountMap.toList()
            .sortedByDescending { it.second }
            .toMap()

        //総マッチ数 -> 割合計算で使用
        val totalCount = sortedTypeWithMatchCountMap.values.sum()

        //Key:形式コードを文字列に変換したリスト -> 棒ごとのカテゴリ名(stackLabels)に使用　＆　Value:割合 -> Y軸データのリスト
        // ↓→ MapのKey：tag_codeの値を特定のString型に変換 ＆ MapのValue：計算した(tag_codeごとの)割合
        val typeMatchRatioMap = sortedTypeWithMatchCountMap
            .mapKeys { it -> when (it.key) {
                0 -> "2v2"
                1 -> "FFA"
                2 -> "6v6"
                3 -> "others"
                4 -> "野良"
                else -> error("Unexpected tag code: ${it.key}")
            }} //(以下)各形式の試合数を全体の試合数で割ることで、形式ごとの試合数の比率を求める
            .mapValues { round(it.value.toFloat() / totalCount * 100) } //round()で銀行丸め
            .toList()

        //マッチ数のリスト(ソート後のやつです) -> ツールチップで表示する「○○試合」
        val typeMatchCountList = sortedTypeWithMatchCountMap.values.toList()
        Log.d("home", "HBarPair:${Pair(typeMatchRatioMap, typeMatchCountList)}")

        //  LiveDataにグラフ表示用データを格納
        _HBarMatchChartLiveData.postValue(Pair(typeMatchRatioMap, typeMatchCountList))

    }

    //横棒帯グラフ [レース] 表示用データの生成
    fun RaceHBarChart(homeAllList: List<MatchWithRace>) {
        //※空のリストではない。
        val nullTypeEmptyAddMap = makeNullTypeEmptyAddMap(homeAllList)

        //形式ごとのMatchのレース数を取得
        val TypeWithMatchCountMap = nullTypeEmptyAddMap
            .mapValues { matches ->
                matches.value.sumBy { matchWithRace ->
                    matchWithRace.Races.size
                }
            }
        //Valueの値の降順(大きい順)にソート　※以下のtoList()では、Mapの各要素をPair<キー,値>に変換したリストになる
        val sortedTypeWithMatchCountMap = TypeWithMatchCountMap.toList()
            .sortedByDescending { it.second }
            .toMap()

        //総レース数 -> 割合計算で使用
        val totalCount = sortedTypeWithMatchCountMap.values.sum()

        //Key:形式コードを文字列に変換したリスト -> 棒ごとのカテゴリ名(stackLabels)に使用　＆　Value:割合 -> Y軸データのリスト
        // ↓→ MapのKey：tag_codeの値を特定のString型に変換 ＆ MapのValue：計算した(tag_codeごとの)割合
        val typeMatchRatioMap = sortedTypeWithMatchCountMap
            .mapKeys { it -> when (it.key) {
                0 -> "2v2"
                1 -> "FFA"
                2 -> "6v6"
                3 -> "others"
                4 -> "野良"
                else -> error("Unexpected tag code: ${it.key}")
            }} //(以下)各形式の試合数を全体の試合数で割ることで、形式ごとの試合数の比率を求める
            .mapValues { round(it.value.toFloat() / totalCount * 100) } //round()で銀行丸め
            .toList()

        //マッチ数のリスト(ソート後のやつです) -> ツールチップで表示する「○○試合」
        val typeMatchCountList = sortedTypeWithMatchCountMap.values.toList()
        Log.d("home", "HBarRacePair:${Pair(typeMatchRatioMap, typeMatchCountList)}")

        //  LiveDataにグラフ表示用データを格納
        _HBarRaceChartLiveData.postValue(Pair(typeMatchRatioMap, typeMatchCountList))

    }

    //横棒帯グラフから呼ばれる。
    fun makeNullTypeEmptyAddMap(list: List<MatchWithRace>) : Map<Int, List<MatchWithRace>> {
        // type_codeでグループ化したMapを作成
        val typeGroupedMap = list.groupBy { it.match.type_code }
        // type_codeが1(個人)のリストを取得
        val type1List = typeGroupedMap[1]
        // tag_codeが2(野良)であるものとそれ以外のものとでグループ分けする　(tag2List＝野良, otherList＝野良以外＝純粋なFFA)
        val (tag2List, otherList) = type1List?.partition { it.match.tag_code == 2 } ?: Pair(emptyList(), emptyList())
        // 元のMapからtype_code=1のキーと値を削除し、Map2を追加する
        val updatedTypeGroupedMap = typeGroupedMap - 1 + mapOf(1 to otherList, 4 to tag2List) //4が野良リスト
        // type_codeがリストに存在しない場合は、空のリストで追加する　キー:typeCodes,値:emptyList()
        val typeCodes = setOf(0, 1, 2, 3, 4) //注)存在する可能性がある全てのtype_codeを記載する。漏れるとnullTypeEmptyAddMapで存在しなくなる
        val nullTypeEmptyAddMap = typeCodes.associateWith { updatedTypeGroupedMap[it] ?: emptyList() }
        return nullTypeEmptyAddMap
    }


}