package com.example.homework

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.homework.ui.theme.HomeworkTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// --- 1. 数据模型与接口 ---
data class Cat(
    val id: Int,
    val name: String,
    val status: String,
    val bio: String,
    val imageRes: Int
)

interface CatRepository {
    fun getCats(category: String): Flow<List<Cat>>
}

class CatFakeRepository : CatRepository {
    override fun getCats(category: String): Flow<List<Cat>> = flow {
        delay(2000) // 模拟网络延迟
        val cats = List(100) { i ->
            val img = when (category) {
                "蓝猫" -> R.drawable.cat_blue
                "暹罗" -> R.drawable.cat_siamese
                else -> R.drawable.cat_orange
            }
            Cat(i, "$category${i + 1}", if (i % 2 == 0) "Online" else "Offline", "猫咪简介内容 $i", img)
        }
        emit(cats)
    }
}

// --- 2. UI 状态建模 ---
sealed interface CatsUiState {
    object Loading : CatsUiState
    data class Success(val cats: List<Cat>) : CatsUiState
    data class Error(val message: String) : CatsUiState
}

// --- 3. ViewModel 构建 ---
class MainViewModel(private val repository: CatRepository = CatFakeRepository()) : ViewModel() {
    private val _selectedCategory = MutableStateFlow("大橘")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _uiState = MutableStateFlow<CatsUiState>(CatsUiState.Loading)
    val uiState: StateFlow<CatsUiState> = _uiState.asStateFlow()

    private var fetchJob: Job? = null

    init {
        fetchCats(_selectedCategory.value)
    }

    fun updateCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        fetchCats(category)
    }

    private fun fetchCats(category: String) {
        fetchJob?.cancel() // 取消上一次任务
        _uiState.value = CatsUiState.Loading
        fetchJob = viewModelScope.launch {
            repository.getCats(category)
                .catch { _uiState.value = CatsUiState.Error("加载失败") }
                .collect { cats ->
                    _uiState.value = CatsUiState.Success(cats)
                }
        }
    }
}

// --- 4. UI 状态辅助类 ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Contacts : Screen("contacts", "通讯录", Icons.AutoMirrored.Filled.List)
    object Favorites : Screen("favorites", "我的最爱", Icons.Default.Favorite)
    object Profile : Screen("profile", "个人资料", Icons.Default.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HomeworkTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val items = remember { listOf(Screen.Contacts, Screen.Favorites, Screen.Profile) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = Color(0xFF4A5982), contentColor = Color.White) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, null) },
                            label = { Text(screen.label) },
                            selected = isSelected,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.White,
                                selectedTextColor = Color.White,
                                unselectedIconColor = Color.LightGray,
                                unselectedTextColor = Color.LightGray,
                                indicatorColor = Color(0xFF3F4B6E)
                            ),
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = Screen.Contacts.route) {
                    composable(Screen.Contacts.route) { 
                        CatListScreen(viewModel) 
                    }
                    composable(Screen.Favorites.route) { SimpleScreen("我的最爱") }
                    composable(Screen.Profile.route) { SimpleScreen("个人资料") }
                }
            }
        }
    }
}

@Composable
fun CatListScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // 顶部蓝色导航栏
        Row(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF4A5982)).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("大橘", "蓝猫", "暹罗").forEach { category ->
                    Surface(
                        onClick = { viewModel.updateCategory(category) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCategory == category) Color.White.copy(0.25f) else Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.height(32.dp).border(1.dp, Color.White.copy(0.4f), RoundedCornerShape(8.dp))
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(category, fontSize = 13.sp)
                        }
                    }
                }
            }
            Text("学号：202303038129", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        // 分支渲染
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            when (val state = uiState) {
                is CatsUiState.Loading -> CircularProgressIndicator(color = Color(0xFF4A5982))
                is CatsUiState.Error -> Text(state.message, color = Color.Red)
                is CatsUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.cats, key = { it.id }) { cat -> CatItem(cat) }
                    }
                }
            }
        }
    }
}

@Composable
fun CatItem(cat: Cat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(cat.imageRes),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(CircleShape).background(Color(0xFFE0E0E0)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(" · ", color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                    Text(cat.status, style = MaterialTheme.typography.titleMedium, color = if (cat.status == "Online") Color(0xFF4CAF50) else Color.Gray)
                }
                Text(cat.bio, style = MaterialTheme.typography.bodyLarge, color = Color.Black)
            }
        }
    }
}

@Composable
fun SimpleScreen(text: String) {
    Box(Modifier.fillMaxSize().background(Color.White), Alignment.Center) { Text(text) }
}
