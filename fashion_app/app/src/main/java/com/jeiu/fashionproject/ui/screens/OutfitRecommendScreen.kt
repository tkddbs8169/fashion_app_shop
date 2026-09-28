package com.jeiu.fashionproject.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jeiu.fashionproject.data.StylePreferenceStore
import com.jeiu.fashionproject.data.WardrobeStore
import com.jeiu.fashionproject.data.WearLogStore
import com.jeiu.fashionproject.data.advice
import com.jeiu.fashionproject.recommend.OutfitEngine
import com.jeiu.fashionproject.recommend.Situation
import com.jeiu.fashionproject.ui.components.NotEnoughClothesCard
import com.jeiu.fashionproject.ui.components.OutfitCard
import com.jeiu.fashionproject.ui.theme.*
import com.jeiu.fashionproject.ui.viewmodel.WeatherUiState
import com.jeiu.fashionproject.ui.viewmodel.WeatherViewModel

/**
 * 코디 추천 화면 (계획 4 — 추천).
 * 홈의 [AI 추천 보기]·[전체보기]로 들어온다.
 *  - 오늘: 오늘 날씨에 맞는 옷장 코디
 *  - 상황별: 면접·데이트·여행·운동 중 하나를 고르면 그 상황에 맞는 코디 (맞는 옷이 없으면 부족한 옷 안내)
 * (3단계에서 [5일 예보] 탭을 추가할 예정)
 */
@Composable
fun OutfitRecommendScreen(
    onBack: () -> Unit,
    onAddClothing: () -> Unit,
    weatherViewModel: WeatherViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        WardrobeStore.load(context)
        WearLogStore.load(context)
    }
    val weatherState by weatherViewModel.state.collectAsState()
    val wardrobe by WardrobeStore.items.collectAsState()
    val wearLogs by WearLogStore.logs.collectAsState()
    val weatherInfo = (weatherState as? WeatherUiState.Success)?.info

    val tabs = listOf("오늘", "상황별")
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var situation by rememberSaveable { mutableStateOf(Situation.INTERVIEW) }
    val currentSituation = if (tab == 1) situation else null

    // 👍👎를 눌러도 카드가 바로 바뀌지 않도록 취향 점수는 계산할 때의 값만 쓴다
    val recommendation = remember(wardrobe, wearLogs, weatherInfo, currentSituation) {
        OutfitEngine.recommend(wardrobe, weatherInfo, wearLogs, StylePreferenceStore.scores.value, currentSituation)
    }
    var ratings by remember { mutableStateOf(mapOf<String, Boolean>()) }

    Column(modifier = Modifier.fillMaxSize().background(Cream)) {
        // 상단 바
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로", tint = CharcoalBlack)
            }
            Text("코디 추천", style = MaterialTheme.typography.titleLarge, color = CharcoalBlack, fontWeight = FontWeight.Bold)
        }

        // 오늘 날씨 한 줄
        Text(
            when (val s = weatherState) {
                is WeatherUiState.Success -> "${s.info.condition} · ${s.info.temp}°C — ${s.info.advice()}"
                is WeatherUiState.Loading -> "날씨 불러오는 중… (계절 기준으로 먼저 추천해요)"
                is WeatherUiState.Error -> "${s.message} (계절 기준으로 추천해요)"
            },
            style = MaterialTheme.typography.bodySmall, color = MediumGray,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(8.dp))

        TabRow(
            selectedTabIndex = tab,
            containerColor = Cream,
            contentColor = Camel,
            indicator = { positions ->
                TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(positions[tab]), color = Camel)
            }
        ) {
            tabs.forEachIndexed { i, title ->
                Tab(
                    selected = tab == i,
                    onClick = { tab = i },
                    text = { Text(title, fontWeight = if (tab == i) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = Camel,
                    unselectedContentColor = MediumGray
                )
            }
        }

        // 상황 고르기
        if (tab == 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Situation.entries.forEach { s ->
                    FilterChip(
                        selected = situation == s,
                        onClick = { situation = s },
                        label = { Text(s.label) },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Camel, selectedLabelColor = White)
                    )
                }
            }
        }

        when (val r = recommendation) {
            is OutfitEngine.Result.NotEnough -> NotEnoughClothesCard(
                message = r.message,
                onAddClothing = onAddClothing,
                modifier = Modifier.padding(20.dp)
            )
            is OutfitEngine.Result.Ok -> LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                r.notice?.let { notice ->
                    item { Text(notice, style = MaterialTheme.typography.bodySmall, color = Brown) }
                }
                items(r.outfits, key = { it.id }) { outfit ->
                    OutfitCard(
                        outfit = outfit,
                        rated = ratings[outfit.id],
                        onRate = { like ->
                            ratings = ratings + (outfit.id to like)
                            StylePreferenceStore.rate(context, outfit.style, like)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        photoHeight = 300.dp
                    )
                }
            }
        }
    }
}
