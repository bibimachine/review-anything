package com.reviewanything.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.reviewanything.app.viewmodel.CheckInViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CheckInCalendar(viewModel: CheckInViewModel) {
    val dates by viewModel.checkInDates.collectAsState()
    val streak by viewModel.streak.collectAsState()
    val checkedToday by viewModel.checkedToday.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // 连续打卡
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (checkedToday) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    if (checkedToday) "✅ 今日已打卡" else "⏰ 今日还未打卡",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("🔥 连续打卡 $streak 天", style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!checkedToday) {
            Button(
                onClick = { viewModel.checkInToday() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("今日打卡")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 月历
        CalendarGrid(dates = dates)
    }
}

@Composable
fun CalendarGrid(dates: Set<String>) {
    val calendar = Calendar.getInstance()
    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    calendar.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val today = sdf.format(Date())

    Column {
        Text(
            "${year}年${month + 1}月",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(8.dp))

        // 星期标题
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach {
                Text(
                    text = it,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // 日期网格
        var day = 1
        for (week in 0..5) {
            if (day > daysInMonth) break
            Row(modifier = Modifier.fillMaxWidth()) {
                for (dow in 0..6) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (week == 0 && dow < firstDayOfWeek) {
                            // 空白
                        } else if (day <= daysInMonth) {
                            calendar.set(Calendar.DAY_OF_MONTH, day)
                            val dateStr = sdf.format(calendar.time)
                            val isChecked = dates.contains(dateStr)
                            val isToday = dateStr == today

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isChecked -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            else -> Color.Transparent
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = if (isChecked) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            day++
                        }
                    }
                }
            }
        }
    }
}
