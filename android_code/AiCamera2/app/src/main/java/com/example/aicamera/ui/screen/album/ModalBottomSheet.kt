package com.example.aicamera.ui.screen.album

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import coil.compose.AsyncImage
import com.example.aicamera.data.network.aiPs.PictureRequirement
import com.example.aicamera.data.network.copywriter.CopywriterRequirement
import kotlinx.coroutines.flow.distinctUntilChanged

// 定义AI文案选项常量
object CopywritingOptions {
    val platforms = listOf("朋友圈", "小红书", "抖音", "Instagram")
    val emotions = listOf("开心", "伤感", "孤独", "松弛")
    val themes = listOf("风景", "美食", "自拍", "宠物", "生活")
    val styles = listOf("文艺风", "幽默风", "口语风", "可爱风")
    val lengths = listOf("短", "中", "长")
    val scenes = listOf("生日", "跨年", "节日")
}

// 定义AI修图选项常量，方便维护
object AiEditOptions {
    val filters = listOf("原图", "清新", "复古", "胶片", "明艳", "暖调", "冷调", "黑白")
    val portraitOpts = listOf("磨皮", "祛痘", "美白", "大眼", "瘦脸")
    val backgroundOpts = listOf("模糊背景", "去掉人群", "去掉背景")
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun CopywriterRequirementSheet(
    onDismiss: () -> Unit,
    onConfirm: (CopywriterRequirement) -> Unit,
    isProcessing: Boolean,
    //onStartGenerating: (CopywriterRequirement) -> Unit, //开始生成的回调
) {
    // 状态管理
    var selectedType by remember { mutableStateOf("") }
    var selectedEmotion by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("") }
    var lengthIndex by remember { mutableFloatStateOf(1f) } // 0:短, 1:中, 2:长
    var selectedSpecial by remember { mutableStateOf("") }
    var customText by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 限制高度约占一半
        modifier = Modifier.fillMaxHeight(0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("生成文案预设", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            // 1. 平台风格 (Type)
            RequirementSection("平台风格", CopywritingOptions.platforms, selectedType) { selectedType = it }

            // 2. 感情风格 (Emotion)
            RequirementSection("感情风格", CopywritingOptions.emotions, selectedEmotion) { selectedEmotion = it }

            // 3. 写作主题 (Theme)
            RequirementSection("写作主题", CopywritingOptions.themes, selectedTheme) { selectedTheme = it }

            // 4. 写作风格 (Style)
            RequirementSection("写作风格", CopywritingOptions.styles, selectedStyle) { selectedStyle = it }

            // 5. 写作长度 (Length) - 滑动条
            Column {
                Text("写作长度: ${CopywritingOptions.lengths[lengthIndex.toInt()]}", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = lengthIndex,
                    onValueChange = { lengthIndex = it },
                    valueRange = 0f..2f,
                    steps = 1,
                    enabled = !isProcessing
                )
            }

            // 6. 特殊场景 (Special)
            RequirementSection("特殊场景", CopywritingOptions.scenes, selectedSpecial) { selectedSpecial = it }

            // 7. 自定义主题
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it },
                label = { Text("自定义要求/主题") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing
            )

            // 确认按钮
            Button(
                onClick = {

                    val req = CopywriterRequirement(
                        type = selectedType.ifBlank { null },
                        emotion = selectedEmotion.ifBlank { null },
                        theme = selectedTheme.ifBlank { null },
                        style = selectedStyle.ifBlank { null },
                        length = CopywritingOptions.lengths[lengthIndex.toInt()],
                        special = selectedSpecial.ifBlank { null },
                        custom = customText.ifBlank { null }
                    )
                    onConfirm(req)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    //LoadingLottieOverlay(isLoading = isProcessing)
                    //加载动画
                }
                Text("开始生成")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AiEditRequirementSheet(
    onDismiss: () -> Unit,
    onConfirm: (PictureRequirement) -> Unit,
    isProcessing: Boolean
) {
    // 状态管理
    var selectedFilter by remember { mutableStateOf("原图") } // 默认为"原图"
    var selectedPortrait by remember { mutableStateOf("") }
    var selectedBackground by remember { mutableStateOf("") }
    var customText by remember { mutableStateOf("") }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 限制高度约占一半
        modifier = Modifier.fillMaxHeight(0.7f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        //windowInsets = WindowInsets.navigationBars // 处理底部导航栏的 Insets
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                // 支持在小屏幕上滚动
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 标题
            Text(
                text = "AI 修图预设",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            // 1. 滤镜 (Filter) - 单选标签
            EditRequirementSection(
                title = "AI滤镜",
                options = AiEditOptions.filters,
                selected = selectedFilter,
                enabled = !isProcessing,
                // 特殊逻辑：滤镜组不随用户点击取消（必须选一个）
                onSelect = { if (it.isNotBlank()) selectedFilter = it }
            )

            HorizontalDivider() // 分割线

            // 2. 人像 (Portrait) - 单选标签
            EditRequirementSection(
                title = "人像强化",
                options = AiEditOptions.portraitOpts,
                selected = selectedPortrait,
                enabled = !isProcessing
            ) { selectedPortrait = it }

            HorizontalDivider() // 分割线

            // 3. 背景 (Background) - 单选标签
            EditRequirementSection(
                title = "背景处理",
                options = AiEditOptions.backgroundOpts,
                selected = selectedBackground,
                enabled = !isProcessing
            ) { selectedBackground = it }

            HorizontalDivider() // 分割线

            // 4. 自定义要求
            OutlinedTextField(
                value = customText,
                onValueChange = { customText = it },
                label = { Text("自定义修图要求") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("例如：去掉左下角的水印") },
                enabled = !isProcessing,
                minLines = 2,
                maxLines = 4
            )

            // 确认按钮
            Button(
                onClick = {
                    val req = PictureRequirement().apply {
                        // 滤镜：原图传 null，让后端走默认逻辑
                        filter = selectedFilter.takeIf { it != "原图" }
                        portrait = selectedPortrait.ifBlank { null }
                        background = selectedBackground.ifBlank { null }
                        special = customText.ifBlank { null } // 自定义放入 special 字段
                    }

                    // 简单校验：至少选一项
                    val hasReq = !req.filter.isNullOrBlank() ||
                            !req.portrait.isNullOrBlank() ||
                            !req.background.isNullOrBlank() ||
                            !req.special.isNullOrBlank()

                    if (!hasReq) {
                        // 这里理想情况是用本地的 Snackbar 或 Toast
                        // 简化起见，先不传出校验失败
                        return@Button
                    }

                    onConfirm(req)
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                enabled = !isProcessing // 处理中禁用
            ) {
                Text("开始修图")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun RequirementSection(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelect(if (selected == option) "" else option) },
                    label = { Text(option) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopywriterResultSheet(
    generatedText: String, // 生成的文案
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // 结果页通常需要展示更多内容，允许占满 90% 高度
        modifier = Modifier.fillMaxHeight(0.9f),
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        // 使用 Column 包裹滚动内容和底部按钮
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 1. 标题
            Text(
                text = "AI 生成文案",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
            )

            // 2. 可滚动的文案内容区域
            Column(
                modifier = Modifier
                    .weight(1f) // 占满剩余空间
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 文案卡片样式
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = generatedText,
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // 占位，确保内容不会被底部按钮遮挡
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. 固定的底部复制按钮区域
            Surface(
                tonalElevation = 2.dp, // 稍微有点阴影，使其看起来浮在内容上方
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            // 调用一键复制功能
                            context.copyToClipboard(generatedText)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("一键复制到剪贴板")
                    }
                }
            }
        }
    }
}

// 修图结果展示
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiEditResultDialog(
    editedPhotoPath: String?, // 生成图片的本地路径
    onDismiss: () -> Unit,
    //onSaveToGallery: () -> Unit, // 保存到相册的回调
    //isSaving: Boolean = false // 是否正在保存的 busy 状态
) {
    if (editedPhotoPath == null) return

    val uri = remember(editedPhotoPath) { editedPhotoPath.toUri() }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        ) // 不允许点击外部关闭
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 标题
                Text(
                    text = "AI 修图完成",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 图片预览区域
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp) // 限制最大高度，防止撑破屏幕
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (uri != null) {
                        AsyncImage(
                            model = uri,
                            contentDescription = "修图结果预览",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("无法加载预览图", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))

                // 按钮区域
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 关闭按钮
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        //enabled = !isSaving
                    ) {
                        Text("关闭")
                    }

                    // 保存按钮
                    /*
                    Button(
                        onClick = onSaveToGallery,
                        modifier = Modifier.weight(1f),
                        enabled = !isSaving
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(size = 18.dp, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("保存到相册")
                    }*/
                }
            }
        }
    }
}

// 一键复制功能
@SuppressLint("ServiceCast")
fun Context.copyToClipboard(text: String, label: String = "AI文案") {
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    // 可选：在 Android 13+ 以下系统手动弹出 Toast 提示
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
        Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
    }
}

// 滚轮组件
//@OptIn(ExperimentalSnappingApi::class) // 使用吸附效果需此注解
@Composable
fun FilterWheelPicker(
    options: List<String>,
    onItemSelected: (Int) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val itemWidthDp = 80.dp // 每个选项的基准宽度
    val screenWidthDp = with(density) { LocalView.current.width.toDp() }

    // 计算内容两侧的 Padding，使选项能居中显示
    val contentPadding = (screenWidthDp - itemWidthDp) / 2

    val lazyListState = rememberLazyListState()

    // 核心逻辑：监听滚动完成，计算处于中心的选项索引
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { firstIndex ->
                // 基于简单的可见项目索引作为选择（适合吸附效果）
                // 这里的逻辑可以做得更复杂（比如加上偏移量判断），
                // 但配合下面的 FlingBehavior 基本够用。
                onItemSelected(firstIndex)
            }
    }

    // 实现简单的吸附停止效果 (SnapFlingBehavior)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = lazyListState)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp),
        contentAlignment = Alignment.Center
    ) {
        // --- 1. 背景选择框 ---
        // 这是固定不动的层，位于 LazyRow 的下层中间
        Surface(
            modifier = Modifier
                .width(itemWidthDp + 16.dp) // 比选项稍微宽一点点
                .height(44.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), // 背景色，暗示选择区域
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        ) {}

        // --- 2. 可滑动的滤镜选项 ---
        LazyRow(
            state = lazyListState,
            flingBehavior = flingBehavior, // 启用吸附行为
            //enabled = enabled,
            contentPadding = PaddingValues(0.dp), // 关键：两侧的 Padding
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(options) { index, option ->
                val isSelected = index == lazyListState.firstVisibleItemIndex

                Box(
                    modifier = Modifier
                        .width(itemWidthDp)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = option,
                        style = if (isSelected) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        // 动态透明度/缩放增加视觉聚焦（可选）
                        modifier = Modifier.alpha(if (isSelected) 1f else 0.7f)
                    )
                }
            }
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EditRequirementSection(
    title: String,
    options: List<String>,
    selected: String,
    enabled: Boolean,
    onSelect: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { option ->
                FilterChip(
                    enabled = enabled,
                    selected = selected == option,
                    onClick = { onSelect(if (selected == option) "" else option) }, // 点击取消选择
                    label = { Text(option) }
                )
            }
        }
    }
}

