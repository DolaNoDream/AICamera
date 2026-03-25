package com.example.aicamera.ui.screen.copywriting

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role.Companion.Checkbox
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.aicamera.ui.uistate.copywriting.CopywritingSort
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CopywritingListScreen(
    viewModel: CopywritingListViewModel,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    onItemClick: ((copywritingId: Long) -> Unit)? = null,
    onCreateClick: (() -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsState()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // 日期选择弹窗状态
    var showCreateFromPicker by remember { mutableStateOf(false) }
    var showCreateToPicker by remember { mutableStateOf(false) }
    var showUpdateFromPicker by remember { mutableStateOf(false) }
    var showUpdateToPicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    fun formatDate(dayStartMillis: Long?): String = if (dayStartMillis == null) "未选择" else dateFormatter.format(Date(dayStartMillis))

    val view = LocalView.current
    val density = LocalDensity.current
    val statusBarTopPaddingDp = androidx.compose.runtime.remember(view, density) {
        val insets = ViewCompat.getRootWindowInsets(view)
        val px = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        with(density) { px.toDp() }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(text = "删除文案") },
            text = { Text(text = "确定删除选中的 ${state.selectedCount} 条文案吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteSelected()
                    },
                    enabled = state.selectedCount > 0
                ) {
                    Text(text = "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "取消")
                }
            }
        )
    }

    // Create From/To pickers
    if (showCreateFromPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.createDateFrom)
        DatePickerDialog(
            onDismissRequest = { showCreateFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateFromPicker = false
                        viewModel.setCreateDateFrom(pickerState.selectedDateMillis)
                    }
                ) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFromPicker = false }) { Text(text = "取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showCreateToPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.createDateTo)
        DatePickerDialog(
            onDismissRequest = { showCreateToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCreateToPicker = false
                        viewModel.setCreateDateTo(pickerState.selectedDateMillis)
                    }
                ) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateToPicker = false }) { Text(text = "取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showUpdateFromPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.updateDateFrom)
        DatePickerDialog(
            onDismissRequest = { showUpdateFromPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateFromPicker = false
                        viewModel.setUpdateDateFrom(pickerState.selectedDateMillis)
                    }
                ) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateFromPicker = false }) { Text(text = "取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showUpdateToPicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = state.updateDateTo)
        DatePickerDialog(
            onDismissRequest = { showUpdateToPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUpdateToPicker = false
                        viewModel.setUpdateDateTo(pickerState.selectedDateMillis)
                    }
                ) { Text(text = "确定") }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateToPicker = false }) { Text(text = "取消") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(statusBarTopPaddingDp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左侧：返回 or 退出选择模式
                when {
                    state.isSelectionMode -> {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "退出选择",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    onBack != null -> {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    else -> {
                        Spacer(modifier = Modifier.width(48.dp))
                    }
                }

                Text(
                    text = if (state.isSelectionMode) "已选择 ${state.selectedCount}" else "文案列表",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                // 右侧：选择/全选/删除/新建
                when {
                    state.isSelectionMode -> {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(
                                imageVector = Icons.Filled.SelectAll,
                                contentDescription = "全选",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            enabled = state.selectedCount > 0
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = "批量删除",
                                tint = if (state.selectedCount > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    else -> {
                        if (onCreateClick != null) {
                            IconButton(onClick = onCreateClick) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "新建文案",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(
                                imageVector = Icons.Filled.DoneAll,
                                contentDescription = "批量选择",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // 搜索区域（显式搜索按钮 + 多条件）
            if (!state.isSelectionMode) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 内容搜索 + 清除(X) + 搜索按钮
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = state.contentQuery,
                            onValueChange = viewModel::onContentQueryChange,
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text(text = "输入关键词") },
                            trailingIcon = {
                                if (state.contentQuery.isNotBlank()) {
                                    IconButton(onClick = { viewModel.clearContentQuery() }) {
                                        Icon(imageVector = Icons.Filled.Clear, contentDescription = "清除")
                                    }
                                }
                            },
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { viewModel.applySearch() })
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(onClick = { viewModel.applySearch() }) {
                            Icon(imageVector = Icons.Filled.Search, contentDescription = "搜索")
                        }
                    }

                    // 条件选择（可组合多选）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = state.enableContentFilter,
                            onCheckedChange = { viewModel.toggleContentFilter(it) }
                        )
                        Text(text = "内容")

                        Spacer(modifier = Modifier.width(12.dp))

                        Checkbox(
                            checked = state.enableCreateTimeFilter,
                            onCheckedChange = { viewModel.toggleCreateTimeFilter(it) }
                        )
                        Text(text = "创建时间")

                        Spacer(modifier = Modifier.width(12.dp))

                        Checkbox(
                            checked = state.enableUpdateTimeFilter,
                            onCheckedChange = { viewModel.toggleUpdateTimeFilter(it) }
                        )
                        Text(text = "修改时间")
                    }

                    // 创建时间范围
                    if (state.enableCreateTimeFilter) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showCreateFromPicker = true }) {
                                Text(text = "创建从：${formatDate(state.createDateFrom)}")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showCreateToPicker = true }) {
                                Text(text = "到：${formatDate(state.createDateTo)}")
                            }
                        }
                    }

                    // 修改时间范围
                    if (state.enableUpdateTimeFilter) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { showUpdateFromPicker = true }) {
                                Text(text = "修改从：${formatDate(state.updateDateFrom)}")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showUpdateToPicker = true }) {
                                Text(text = "到：${formatDate(state.updateDateTo)}")
                            }
                        }
                    }

                    // 排序（修复：点击后立即 applySearch 才能看见变化）
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.wrapContentSize()) {
                            TextButton(onClick = { showSortMenu = true }) {
                                Text(text = when (state.sort) {
                                    CopywritingSort.CreateTimeDesc -> "按创建时间倒序"
                                    CopywritingSort.UpdateTimeDesc -> "按修改时间倒序"
                                })
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text(text = "按创建时间倒序") },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSort(CopywritingSort.CreateTimeDesc)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text(text = "按修改时间倒序") },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSort(CopywritingSort.UpdateTimeDesc)
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        TextButton(
                            onClick = {
                                viewModel.clearContentQuery()
                                viewModel.clearTimeFilters()
                            }
                        ) {
                            Text(text = "清空条件")
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }

                    !state.errorMessage.isNullOrBlank() && state.items.isEmpty() -> {
                        Text(
                            text = state.errorMessage ?: "加载失败",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    state.items.isEmpty() -> {
                        Text(
                            text = "暂无文案",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }

                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            item { Spacer(modifier = Modifier.height(6.dp)) }

                            items(state.items, key = { it.id }) { item ->
                                val selected = state.selectedIds.contains(item.id)

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(
                                            enabled = true,
                                            indication = LocalIndication.current,
                                            interactionSource = remember { MutableInteractionSource() },
                                            onClick = {
                                                if (state.isSelectionMode) {
                                                    viewModel.toggleSelect(item.id)
                                                } else {
                                                    onItemClick?.invoke(item.id)
                                                }
                                            }
                                        ),
                                    shape = MaterialTheme.shapes.large,
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                                    ),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        if (state.isSelectionMode) {
                                            Icon(
                                                imageVector = if (selected) Icons.Filled.Check else Icons.Filled.Close,
                                                contentDescription = if (selected) "已选中" else "未选中",
                                                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(end = 10.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "关联照片：${item.photoCount} 张",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = item.content,
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                maxLines = 3,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            item { Spacer(modifier = Modifier.height(16.dp)) }
                        }
                    }
                }
            }
        }
    }
}
