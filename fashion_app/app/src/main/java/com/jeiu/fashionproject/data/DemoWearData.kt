package com.jeiu.fashionproject.data

import android.content.Context
import android.content.pm.ApplicationInfo
import org.json.JSONObject

/**
 * 시연용 샘플 착용 기록 (계획 3 — 5단계).
 *
 * "3개월 넘게 안 입은 옷" 알림은 실제로 90일을 기다려야 볼 수 있으므로,
 * 버튼 하나로 샘플 옷의 등록일을 약 6개월 전으로 옮기고 지난 6개월치 착용 기록을 넣는다.
 *  - 최근에 자주 입은 옷: 베이지 니트, 블랙 슬랙스, 체크 셔츠, 스트라이프 블라우스, 스니커즈
 *  - 오래 안 입은 옷(알림 대상): 와이드 청바지, 카고 팬츠, 블랙 미니 스커트(한 번도 안 입음), 베이지 블레이저, 로퍼
 *  - 오래 안 입었지만 계절이 안 맞아 빠지는 옷: 화이트 반팔티(여름), 데님 반팔 셔츠(여름) — 가을 기준
 * 직접 기록한 날짜는 건드리지 않고, [지우기]로 시연 기록과 등록일만 원래대로 되돌린다.
 */
object DemoWearData {
    private const val PREFS = "demo_wear_data"
    private const val KEY_ORIGINAL_ADDED = "original_added_at"   // 샘플 옷의 원래 등록일 백업
    private const val SAMPLE_AGE_DAYS = 200

    /** 개발용(디버그) 앱에서만 시연 버튼을 보여준다. */
    fun isAvailable(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    fun isApplied(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).contains(KEY_ORIGINAL_ADDED)

    /** (며칠 전, 입은 샘플 옷 번호들) */
    private val pattern: List<Pair<Int, List<String>>> = listOf(
        // 최근 두 달 — 자주 입는 옷
        2 to listOf("sample_01", "sample_06", "sample_11"),
        5 to listOf("sample_04", "sample_06", "sample_11"),
        8 to listOf("sample_02", "sample_06", "sample_11"),
        12 to listOf("sample_01", "sample_06", "sample_11"),
        15 to listOf("sample_04", "sample_06", "sample_11"),
        19 to listOf("sample_01", "sample_06", "sample_11"),
        27 to listOf("sample_02", "sample_06", "sample_11"),
        33 to listOf("sample_01", "sample_04", "sample_11"),
        45 to listOf("sample_02", "sample_06", "sample_11"),
        58 to listOf("sample_01", "sample_06", "sample_11"),
        // 3개월 이상 전 — 이후로 안 입은 옷
        100 to listOf("sample_03", "sample_06"),                // 화이트 반팔티 (여름 옷)
        110 to listOf("sample_04", "sample_06", "sample_12"),   // 로퍼
        125 to listOf("sample_02", "sample_05", "sample_11"),   // 와이드 청바지
        140 to listOf("sample_10", "sample_06", "sample_11"),   // 데님 반팔 셔츠 (여름 옷)
        150 to listOf("sample_01", "sample_07", "sample_11"),   // 카고 팬츠
        175 to listOf("sample_09", "sample_05", "sample_12"),   // 베이지 블레이저
    )

    /** 시연 데이터 넣기. 넣은 기록 수를 돌려준다. */
    fun apply(context: Context): Int {
        WardrobeStore.load(context)
        WearLogStore.load(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sampleIds = SampleWardrobe.items().map { it.id }.toSet()
        val samples = WardrobeStore.items.value.filter { it.id in sampleIds }

        // 원래 등록일을 한 번만 백업하고, 약 6개월 전으로 옮긴다
        if (!prefs.contains(KEY_ORIGINAL_ADDED)) {
            val backup = JSONObject()
            samples.forEach { backup.put(it.id, it.addedAt) }
            prefs.edit().putString(KEY_ORIGINAL_ADDED, backup.toString()).apply()
        }
        val oldAddedAt = System.currentTimeMillis() - SAMPLE_AGE_DAYS * 86_400_000L
        samples.forEach { WardrobeStore.update(context, it.copy(addedAt = oldAddedAt)) }

        val alive = samples.map { it.id }.toSet()   // 지운 샘플 옷은 기록에서 뺀다
        val logs = pattern.mapNotNull { (daysAgo, ids) ->
            val worn = ids.filter { it in alive }
            if (worn.isEmpty()) null else WearLog(WearDates.daysAgo(daysAgo), worn, demo = true)
        }
        return WearLogStore.addIfAbsent(context, logs)
    }

    /** 시연 데이터 지우기 — 시연 기록만 지우고 샘플 옷 등록일을 되돌린다. */
    fun clear(context: Context) {
        WardrobeStore.load(context)
        WearLogStore.load(context)
        WearLogStore.removeDemo(context)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_ORIGINAL_ADDED, null)?.let { raw ->
            val backup = JSONObject(raw)
            WardrobeStore.items.value.filter { backup.has(it.id) }.forEach {
                WardrobeStore.update(context, it.copy(addedAt = backup.getLong(it.id)))
            }
        }
        prefs.edit().remove(KEY_ORIGINAL_ADDED).apply()
    }
}
