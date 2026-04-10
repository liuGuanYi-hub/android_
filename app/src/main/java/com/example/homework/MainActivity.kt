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
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.homework.ui.theme.HomeworkTheme

// 第一步：定义数据模型 (Model)
data class Cat(
    val id: Int,
    val name: String,
    val status: String, // "Online" 或 "Offline"
    val bio: String,
    val imageRes: Int
)

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
fun MainScreen() {
    val navController = rememberNavController()
    val items = remember { listOf(Screen.Contacts, Screen.Favorites, Screen.Profile) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = Color(0xFF4A5982),
                    contentColor = Color.White
                ) {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    items.forEach { screen ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = null) },
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
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
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
                NavHost(
                    navController = navController,
                    startDestination = Screen.Contacts.route
                ) {
                    composable(Screen.Contacts.route) { CatListScreen() }
                    composable(Screen.Favorites.route) { SimpleScreen("我的最爱") }
                    composable(Screen.Profile.route) { SimpleScreen("个人资料") }
                }
            }
        }
    }
}

@Composable
fun CatListScreen() {
    var selectedCategory by remember { mutableStateOf("大橘") }

    val allCats = remember {
        listOf(
            Cat(1, "大橘1", "Offline", "专心干饭中，勿扰", R.drawable.cat_orange),
            Cat(2, "大橘2", "Online", "爱吃小鱼干", R.drawable.cat_orange),
            Cat(3, "大橘3", "Offline", "阳光开朗", R.drawable.cat_orange),
            Cat(4, "大橘4", "Online", "温柔粘人", R.drawable.cat_orange),
            Cat(5, "大橘5", "Offline", "活泼好动", R.drawable.cat_orange),
            Cat(6, "大橘6", "Online", "喜欢睡觉", R.drawable.cat_orange),
            Cat(7, "蓝猫1", "Online", "我是高冷的蓝猫", R.drawable.cat_blue),
            Cat(8, "暹罗1", "Offline", "挖煤工人上线", R.drawable.cat_siamese)
        )
    }

    val filteredCats = remember(selectedCategory) {
        allCats.filter { it.name.startsWith(selectedCategory) }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {
        // 蓝色顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF4A5982)) // 设置深蓝色背景
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("大橘", "蓝猫", "暹罗").forEach { category ->
                    Surface(
                        onClick = { selectedCategory = category },
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedCategory == category) Color.White.copy(alpha = 0.25f) else Color.Transparent,
                        contentColor = Color.White,
                        modifier = Modifier.height(32.dp).border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 12.dp)) {
                            Text(category, fontSize = 13.sp)
                        }
                    }
                }
            }
            Text(
                text = "学号：202303038129",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White // 文字设为白色
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredCats, key = { it.id }) { cat ->
                CatItem(cat)
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
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = cat.imageRes),
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " · ",
                        color = Color.Gray,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Text(
                        text = cat.status,
                        style = MaterialTheme.typography.titleMedium,
                        color = if (cat.status == "Online") Color(0xFF4CAF50) else Color.Gray
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = cat.bio,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
fun SimpleScreen(text: String) {
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Text(text = text)
    }
}
