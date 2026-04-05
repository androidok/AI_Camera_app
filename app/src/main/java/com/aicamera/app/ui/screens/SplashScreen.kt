package com.aicamera.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicamera.app.ui.components.FeatureCard
import com.aicamera.app.ui.components.PrimaryButton
import com.aicamera.app.ui.theme.*
import com.aicamera.app.ui.theme.getColorScheme

/**
 * ============================================
 * 加载界面（Splash Screen）
 * ============================================
 *
 * 功能说明：
 * - 应用启动时的欢迎页面
 * - 展示应用的三大核心功能
 * - 用户点击"点击启动"进入主相机界面
 *
 * 后端开发者注意：
 * - onNavigateToCamera: 点击启动后的回调函数
 * - 这个界面是纯展示，不需要任何业务逻辑
 * - 如果需要在启动时加载数据，可以在这个界面添加 LaunchedEffect
 */
@Composable
fun SplashScreen(
    themeType: ThemeType,
    onNavigateToCamera: () -> Unit
) {
    val colorScheme = getColorScheme(themeType)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.15f))

            // Logo 图标
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "相机图标",
                    modifier = Modifier.size(50.dp),
                    tint = colorScheme.onPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "智能相机",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题
            Text(
                text = "自然增强移动端摄影体验\n在你拍摄的一刻生效",
                fontSize = 14.sp,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.weight(0.1f))

            // 功能卡片 1：智能辅助
            FeatureCard(
                icon = Icons.Default.AutoAwesome,
                title = "智能辅助",
                description = "实时场景识别，自动优化拍摄参数",
                themeType = themeType
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 功能卡片 2：构图优化
            FeatureCard(
                icon = Icons.Default.GridOn,
                title = "构图优化",
                description = "智能裁剪和三分法构图辅助线",
                themeType = themeType
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 功能卡片 3：AI 美化
            FeatureCard(
                icon = Icons.Default.Palette,
                title = "AI 美化",
                description = "一键 AI 色彩增强，保持自然观感",
                themeType = themeType
            )

            Spacer(modifier = Modifier.weight(0.15f))

            // 主按钮
            PrimaryButton(
                text = "点击启动",
                onClick = onNavigateToCamera,
                icon = Icons.Default.CameraAlt,
                themeType = themeType
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * ============================================
 * 后端开发者对接说明
 * ============================================
 *
 * 1. 如何使用这个界面：
 *    在 MainActivity.kt 中已经配置好导航：
 *    ```kotlin
 *    SplashScreen(
 *        onNavigateToCamera = {
 *            currentScreen = Screen.Camera
 *        }
 *    )
 *    ```
 *
 * 2. 如何添加启动时的数据加载：
 *    ```kotlin
 *    @Composable
 *    fun SplashScreen(onNavigateToCamera: () -> Unit) {
 *        // 添加这段代码
 *        LaunchedEffect(Unit) {
 *            // 在这里加载数据
 *            loadInitialData()
 *            // 延迟跳转（可选）
 *            delay(2000)
 *            onNavigateToCamera()
 *        }
 *
 *        // 原有 UI 代码...
 *    }
 *    ```
 *
 * 3. 如何修改功能卡片内容：
 *    直接修改 FeatureCard 的参数即可：
 *    - icon: 图标（从 Icons.Default 中选择）
 *    - title: 标题文字
 *    - description: 描述文字
 *
 * 4. 注意事项：
 *    - 不要修改布局结构和间距
 *    - 颜色使用主题中定义的颜色常量
 *    - 所有文字应该是可配置的（考虑国际化）
 */
