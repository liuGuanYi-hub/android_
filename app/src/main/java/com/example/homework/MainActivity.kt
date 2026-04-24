package com.example.homework

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homework.ui.theme.HomeworkTheme
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- 1. 数据模型与接口 ---
data class Cat(
    val id: Int,
    val name: String,
    val status: String,
    val bio: String,
    val imageRes: Int,
    var isLiked: Boolean = false,
    var isFavorite: Boolean = false,
    var likes: Int = 0
)

interface CatRepository {
    fun getCats(category: String): Flow<List<Cat>>
}

class CatFakeRepository @Inject constructor() : CatRepository {
    override fun getCats(category: String): Flow<List<Cat>> = flow {
        delay(1000) // 模拟网络延迟
        val cats = List(50) { i ->
            val img = when (category) {
                "蓝猫" -> R.drawable.cat_blue
                "暹罗" -> R.drawable.cat_siamese
                else -> R.drawable.cat_orange
            }
            Cat(i, "$category${i + 1}", if (i % 2 == 0) "Online" else "Offline", 
                "圆滚滚的干饭小能手，性格温顺粘人，自带携带福气buff，走到哪都像攒着小太阳，是公认的暖心招财猫。", img,
                likes = (0..10).random())
        }
        emit(cats)
    }
}

// --- 2. UI 状态建模 ---
sealed interface CatsUiState {
    object Idle : CatsUiState
    object Loading : CatsUiState
    data class Success(val cats: List<Cat>) : CatsUiState
    data class Error(val message: String) : CatsUiState
}

// --- 3. ViewModel 构建 ---
@HiltViewModel
class MainViewModel @Inject constructor(private val repository: CatRepository) : ViewModel() {
    private val _selectedCategory = MutableStateFlow("大橘")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _uiState = MutableStateFlow<CatsUiState>(CatsUiState.Idle)
    val uiState: StateFlow<CatsUiState> = _uiState.asStateFlow()

    private var allCats: List<Cat> = emptyList()
    private var fetchJob: Job? = null

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        filterCats()
    }

    fun updateCategory(category: String) {
        if (_selectedCategory.value == category) return
        _selectedCategory.value = category
        fetchCats(category)
    }

    fun fetchCats(category: String = _selectedCategory.value) {
        fetchJob?.cancel()
        _uiState.value = CatsUiState.Loading
        fetchJob = viewModelScope.launch {
            repository.getCats(category)
                .catch { _uiState.value = CatsUiState.Error("加载失败") }
                .collect { cats ->
                    allCats = cats
                    filterCats()
                }
        }
    }
    
    private fun filterCats() {
        val query = _searchQuery.value
        val filtered = if (query.isEmpty()) {
            allCats
        } else {
            allCats.filter { it.name.contains(query, ignoreCase = true) }
        }
        _uiState.value = CatsUiState.Success(filtered)
    }

    fun getCatById(id: Int): Cat? {
        return allCats.find { it.id == id }
    }
}

// --- 4. 路由定义 ---
sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Contacts : Screen("contacts", "通讯录", Icons.AutoMirrored.Filled.List)
    object Favorites : Screen("favorites", "我的最爱", Icons.Default.Favorite)
    object Profile : Screen("profile", "个人资料", Icons.Default.Person)
}

@AndroidEntryPoint
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
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val items = remember { listOf(Screen.Contacts, Screen.Favorites, Screen.Profile) }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                if (currentRoute in items.map { it.route }) {
                    NavigationBar(containerColor = Color(0xFF4A5982), contentColor = Color.White) {
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
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(navController = navController, startDestination = Screen.Contacts.route) {
                    composable(Screen.Contacts.route) { 
                        CatListScreen(viewModel, snackbarHostState) { catId ->
                            navController.navigate("catDetail/$catId")
                        }
                    }
                    composable(Screen.Favorites.route) { SimpleScreen("我的最爱") }
                    composable(Screen.Profile.route) { SimpleScreen("个人资料") }
                    composable(
                        route = "catDetail/{catId}",
                        arguments = listOf(navArgument("catId") { type = NavType.IntType })
                    ) { backStackEntry ->
                        val catId = backStackEntry.arguments?.getInt("catId") ?: 0
                        val cat = viewModel.getCatById(catId)
                        if (cat != null) {
                            CatDetailScreen(cat, snackbarHostState) {
                                navController.popBackStack()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CatListScreen(viewModel: MainViewModel, snackbarHostState: SnackbarHostState, onCatClick: (Int) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // 监听网络状态
    var isNetworkAvailable by remember { mutableStateOf(true) }
    
    DisposableEffect(context) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isNetworkAvailable = true
            }
            override fun onLost(network: Network) {
                isNetworkAvailable = false
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, callback)
        
        // 初始化检查
        val activeNetwork = connectivityManager.activeNetwork
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork)
        isNetworkAvailable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        
        onDispose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
    
    var searchText by remember { mutableStateOf("") }
    
    LaunchedEffect(searchText) {
        snapshotFlow { searchText }
            .debounce(500)
            .distinctUntilChanged()
            .collect { query ->
                viewModel.updateSearchQuery(query)
            }
    }

    LaunchedEffect(Unit) {
        if (uiState is CatsUiState.Idle) {
            viewModel.fetchCats()
        }
    }

    val listState = rememberLazyListState()
    val showFAB by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }

    Scaffold(
        floatingActionButton = {
            AnimatedVisibility(
                visible = showFAB,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    containerColor = Color(0xFF4A5982),
                    contentColor = Color.White,
                    shape = CircleShape
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "回到顶部")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).background(Color(0xFFF8F9FA))) {
            // 顶部标题 - 修复为图片样式
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF4A5982))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    text = "猫咪通讯录",
                    modifier = Modifier.align(Alignment.CenterStart),
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // 网络状态横幅
            AnimatedVisibility(
                visible = !isNetworkAvailable,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFDADA))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFB71C1C),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "网络已断开，部分功能不可用",
                        color = Color(0xFFB71C1C),
                        fontSize = 14.sp
                    )
                }
            }

            Column(modifier = Modifier.background(Color.White)) {
                // 筛选栏 - 修复为图片样式
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf("大橘", "蓝猫", "暹罗").forEach { category ->
                            val isSelected = selectedCategory == category
                            Card(
                                onClick = { viewModel.updateCategory(category) },
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF4A5982) else Color.White
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.height(44.dp).width(64.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = category,
                                        fontSize = 16.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else Color.Black
                                    )
                                }
                            }
                        }
                    }
                    Text("学号：202303038129", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
                
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    modifier = Modifier.fillMaxWidth().padding(8.dp).background(Color.White, RoundedCornerShape(8.dp)),
                    placeholder = { Text("搜索猫咪名字...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF4A5982),
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (val state = uiState) {
                    is CatsUiState.Loading -> CircularProgressIndicator(color = Color(0xFF4A5982))
                    is CatsUiState.Error -> Text(state.message, color = Color.Red)
                    is CatsUiState.Success -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.cats, key = { it.id }) { cat -> 
                                CatItem(
                                    cat = cat, 
                                    onClick = { onCatClick(cat.id) },
                                    onCallClick = {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                message = "正在呼叫 ${cat.name}...",
                                                actionLabel = "取消",
                                                duration = SnackbarDuration.Short
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun CatItem(cat: Cat, onClick: () -> Unit, onCallClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
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
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(cat.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(" · ", color = Color.Gray, style = MaterialTheme.typography.titleLarge)
                    Text(cat.status, style = MaterialTheme.typography.titleMedium, color = if (cat.status == "Online") Color(0xFF4CAF50) else Color.Gray)
                }
                Text(cat.bio, style = MaterialTheme.typography.bodyMedium, color = Color.Black, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.LightGray)
                Text("${cat.likes}", fontSize = 10.sp, color = Color.Gray)
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onCallClick) {
                Icon(Icons.Default.Call, contentDescription = "呼叫", tint = Color(0xFF4CAF50))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatDetailScreen(cat: Cat, snackbarHostState: SnackbarHostState, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var isLiked by remember { mutableStateOf(cat.isLiked) }
    var isFavorite by remember { mutableStateOf(cat.isFavorite) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        TopAppBar(
            title = { Text("猫猫详情", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            },
            actions = {
                Text(
                    text = "202303038129",
                    modifier = Modifier.padding(end = 16.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.DarkGray
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFC5CAE9))
        )
        
        Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(cat.imageRes),
                contentDescription = null,
                modifier = Modifier.size(200.dp).clip(CircleShape).border(4.dp, Color(0xFFC5CAE9), CircleShape),
                contentScale = ContentScale.Crop
            )
            
            Spacer(Modifier.height(24.dp))
            
            Text(cat.name, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3F51B5))
            
            Spacer(Modifier.height(16.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("签名：专心干饭中，勿扰", color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Text(cat.bio, style = MaterialTheme.typography.bodyLarge, color = Color.DarkGray)
            }
            
            Spacer(Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isLiked = !isLiked
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isLiked) "点赞成功！" else "已取消点赞")
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "点赞",
                        tint = if (isLiked) Color.Red else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isLiked) "1" else "0", color = Color.Gray)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        isFavorite = !isFavorite
                        scope.launch {
                            snackbarHostState.showSnackbar(if (isFavorite) "已加入收藏" else "已取消收藏")
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Star else Icons.Outlined.Star,
                        contentDescription = "收藏",
                        tint = if (isFavorite) Color(0xFFFFB300) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (isFavorite) "已收藏" else "收藏", color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun SimpleScreen(text: String) {
    Box(Modifier.fillMaxSize().background(Color.White), Alignment.Center) { Text(text) }
}
