package com.reviewanything.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.reviewanything.app.data.model.ReviewItem
import com.reviewanything.app.viewmodel.CheckInViewModel
import com.reviewanything.app.viewmodel.ReviewViewModel
import com.reviewanything.app.viewmodel.CheckInViewModelFactory
import androidx.compose.ui.platform.LocalContext

@Composable
fun ReviewScreen(
    viewModel: ReviewViewModel,
    checkInViewModel: CheckInViewModel = viewModel(
        factory = CheckInViewModelFactory(
            (LocalContext.current.applicationContext as com.reviewanything.app.ReviewAnythingApp).database
        )
    )
) {
    val items by viewModel.items.collectAsState()
    val currentIndex by viewModel.currentIndex.collectAsState()
    val finished by viewModel.finished.collectAsState()
    val empty by viewModel.empty.collectAsState()
    val stats by viewModel.stats.collectAsState()
    val dailyCount by viewModel.dailyCount.collectAsState()

    // 复习完成后刷新打卡状态
    LaunchedEffect(finished) {
        if (finished) {
            checkInViewModel.loadCheckIns()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // 打卡日历
        CheckInCalendar(viewModel = checkInViewModel)

        Divider(modifier = Modifier.padding(vertical = 8.dp))

        // 复习区域
        when {
            empty && items.isEmpty() -> ReviewEmpty(onStart = { viewModel.loadItems(dailyCount) })
            finished -> ReviewFinished(stats = stats, onRestart = { viewModel.restart() })
            items.isEmpty() -> ReviewIdle(
                dailyCount = dailyCount,
                onDailyCountChange = { viewModel.setDailyCount(it) },
                onStart = { viewModel.loadItems(dailyCount) }
            )
            else -> {
                val item = items.getOrNull(currentIndex) ?: return@Column
                ReviewCard(
                    item = item,
                    current = currentIndex + 1,
                    total = items.size,
                    onRemembered = { viewModel.onRemembered() },
                    onForgotten = { viewModel.onForgotten() }
                )
            }
        }
    }
}

@Composable
fun ReviewIdle(
    dailyCount: Int,
    onDailyCountChange: (Int) -> Unit,
    onStart: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("📚", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(12.dp))
        Text("准备开始复习", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text("选择每日复习数量后开始", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(modifier = Modifier.height(24.dp))

        // 每日数量
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("每日复习", style = MaterialTheme.typography.bodyLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onDailyCountChange((dailyCount - 5).coerceAtLeast(1)) }) {
                    Text("−", style = MaterialTheme.typography.titleLarge)
                }
                Text(
                    "$dailyCount",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.widthIn(min = 48.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = { onDailyCountChange((dailyCount + 5).coerceAtMost(100)) }) {
                    Text("+", style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
            Text("开始复习")
        }
    }
}

@Composable
fun ReviewEmpty(onStart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📭", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("暂无复习内容", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "请先上传笔记，系统会为你生成复习卡片",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStart) {
            Text("刷新试试")
        }
    }
}

@Composable
fun ReviewCard(
    item: ReviewItem,
    current: Int,
    total: Int,
    onRemembered: () -> Unit,
    onForgotten: () -> Unit
) {
    var showAnswer by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 进度
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$current / $total", style = MaterialTheme.typography.bodyMedium)
            Text("${(current * 100 / total)}%", style = MaterialTheme.typography.bodySmall)
        }
        LinearProgressIndicator(
            progress = { current.toFloat() / total },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 问题
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = item.question,
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 答案
        AnimatedVisibility(visible = showAnswer) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Text(
                    text = item.answer,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 按钮
        if (!showAnswer) {
            Button(
                onClick = { showAnswer = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("查看答案")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        showAnswer = false
                        onForgotten()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("忘记了")
                }
                Button(
                    onClick = {
                        showAnswer = false
                        onRemembered()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("记住了")
                }
            }
        }
    }
}

@Composable
fun ReviewFinished(stats: ReviewViewModel.ReviewStats, onRestart: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("今日复习完成！", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            StatCard(label = "总题数", value = stats.total.toString())
            StatCard(label = "易忘", value = stats.forget.toString())
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRestart) {
            Text("再来一组")
        }
    }
}

@Composable
fun StatCard(label: String, value: String) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}
