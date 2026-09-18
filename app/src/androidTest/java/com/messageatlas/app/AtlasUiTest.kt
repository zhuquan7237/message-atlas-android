package com.messageatlas.app

import android.Manifest
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.messageatlas.app.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/** Synthetic fixtures only. This source set is never packaged in the distributed APK. */
@RunWith(AndroidJUnit4::class)
class AtlasUiTest {
    // Pre-grant so the system permission dialog never covers the Compose hierarchy under test.
    @get:Rule(order = 0)
    val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    @get:Rule(order = 1)
    val compose = createAndroidComposeRule<MainActivity>()
    private val app get() = compose.activity.application as MessageAtlasApp

    @Before fun prepare() {
        runBlocking {
            app.settings.setOnboardingSeen()
            app.settings.setInspectionEnabled(false)
            app.settings.setUpdateCheckEnabled(false)
            app.repository.clearAll()
            val now = System.currentTimeMillis()
            val samples = listOf(
                Triple("微信", "林小满", "周末去看展吗？我找到一家很棒的美术馆，等你一起。"),
                Triple("飞书", "设计协作组", "新版首页评审定在下午 3 点，记得带上你的灵感。"),
                Triple("邮件", "一封值得慢读的信", "本周创意简报已送达。留一点时间，看看世界的新鲜事。"),
                Triple("日历", "给自己留点时间", "18:30 · 散步 30 分钟，今天也要好好休息。")
            )
            samples.forEachIndexed { index, (name, title, body) ->
                app.repository.insert(CapturedMessage(notificationKey = "fixture-"+index, packageName = "qa.app"+index, appName = name, title = title, content = body, postedAt = now - index * 600_000, isImportant = index == 1))
            }
        }
        compose.waitUntil(10_000) { compose.onAllNodesWithText("全部 4").fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun inboxSearchStarAndDateSelection() {
        capture("01-inbox")
        compose.onNodeWithText("搜索消息、联系人或应用").performTextInput("不存在的搜索内容")
        compose.onNodeWithText("没有找到匹配的消息").assertExists()
        capture("02-search-empty")
        compose.onNodeWithContentDescription("清空搜索").performClick()
        compose.onNodeWithText("重点 1").performClick()
        compose.onNodeWithText("设计协作组").performScrollTo().assertIsDisplayed()
        compose.onNodeWithContentDescription("取消重点").performClick()
        compose.onNodeWithText("没有找到匹配的消息").assertExists()
        compose.onNodeWithText("清除筛选").performScrollTo().performClick()
        compose.onNodeWithContentDescription("选择日期").performClick()
        compose.onNodeWithText("查看这一天").assertIsDisplayed()
        compose.onNodeWithText("取消").performClick()
    }

    @Test fun navigationSettingsRulesAndAiConfiguration() {
        compose.onNodeWithText("设置", useUnmergedTree = true).performClick()
        compose.onNodeWithText("收录规则").assertExists()
        capture("03-settings")
        compose.onNodeWithText("收录规则").performClick()
        compose.onNodeWithText("仅收录选中应用").assertExists()
        capture("04-rules")
        compose.onNodeWithContentDescription("返回设置").performClick()
        compose.onNodeWithText("AI 接口与模型").performClick()
        compose.onNodeWithText("你的 AI 助手").assertExists()
        capture("05-ai")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithText("日报", useUnmergedTree = true).performClick()
        compose.onNodeWithText("每一天，都有重点").assertExists()
        capture("06-reports")
    }

    @Test fun deletionRequiresExplicitConfirmation() {
        compose.onNodeWithText("林小满").performScrollTo().performClick()
        compose.onNodeWithText("删除消息").performScrollTo().performClick()
        compose.onNodeWithText("删除这条消息？").assertIsDisplayed()
        compose.onNodeWithText("保留").performClick()
        compose.onNodeWithText("林小满").assertExists()
        compose.onNodeWithText("删除消息").performScrollTo().performClick()
        compose.onNodeWithText("确认删除").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("林小满").fetchSemanticsNodes().isEmpty() }
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val folder = File(compose.activity.getExternalFilesDir(null), "qa").apply { mkdirs() }
        File(folder, name+".png").outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
