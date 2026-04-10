package com.florian.waypoint

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private data class OnboardPage(val emoji: String, val title: String, val description: String)

private val OnboardPages = listOf(
    OnboardPage(
        emoji = "\uD83C\uDFD4\uFE0F",
        title = "Welcome to Piste",
        description = "Your ski companion for tracking runs, saving spots, and exploring the mountain."
    ),
    OnboardPage(
        emoji = "\uD83D\uDCCD",
        title = "Mark your spots",
        description = "Long-press anywhere on the map to drop a waypoint. Add photos, notes, colors, and ski-themed icons."
    ),
    OnboardPage(
        emoji = "\uD83D\uDD34",
        title = "Track your runs",
        description = "Tap the red record button to start tracking. Your vertical, max speed, and runs are detected automatically."
    ),
    OnboardPage(
        emoji = "\uD83D\uDCCA",
        title = "Live dashboard",
        description = "Tap the coordinate header for a full-screen speedometer with altitude and vertical descended."
    ),
    OnboardPage(
        emoji = "\uD83C\uDFC2",
        title = "Explore the mountain",
        description = "Switch between Standard, Piste, Terrain, and Satellite maps. Download tiles to use the app offline."
    )
)

/**
 * Full-screen onboarding overlay. Place inside a Box that fills the screen.
 * Blocks back-press while visible so users can't accidentally dismiss it.
 */
@Composable
fun OnboardingOverlay(onDone: () -> Unit) {
    BackHandler { /* swallow — must complete or skip */ }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val pagerState = rememberPagerState(pageCount = { OnboardPages.size })
        val scope = rememberCoroutineScope()

        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
        ) {
            // Skip button row
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                if (pagerState.currentPage < OnboardPages.size - 1) {
                    Text(
                        "Skip",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.W500,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .iosClickable(onDone)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Swipeable pages
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { idx ->
                val page = OnboardPages[idx]
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(page.emoji, fontSize = 80.sp)
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        page.title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.W700,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        page.description,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline,
                        lineHeight = 24.sp
                    )
                }
            }

            // Pagination dots
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(OnboardPages.size) { i ->
                    val selected = i == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (selected) 10.dp else 8.dp)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                CircleShape
                            )
                    )
                }
            }

            // Next / Done button
            Surface(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp).height(54.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().iosClickable {
                        if (pagerState.currentPage < OnboardPages.size - 1) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (pagerState.currentPage < OnboardPages.size - 1) "Next" else "Let's ride",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.W600,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}
