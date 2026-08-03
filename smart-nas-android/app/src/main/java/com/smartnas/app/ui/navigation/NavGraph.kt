package com.smartnas.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.smartnas.app.ui.screens.aichat.AiChatScreen
import com.smartnas.app.ui.screens.aichat.ConversationScreen
import com.smartnas.app.ui.screens.book.BookScreen
import com.smartnas.app.ui.screens.face.FaceScreen
import com.smartnas.app.ui.screens.family.FamilyScreen
import com.smartnas.app.ui.screens.file.FileScreen
import com.smartnas.app.ui.screens.friend.FriendScreen
import com.smartnas.app.ui.screens.home.HomeScreen
import com.smartnas.app.ui.screens.login.LoginScreen
import com.smartnas.app.ui.screens.photo.PhotoDetailScreen
import com.smartnas.app.ui.screens.photo.PhotoGalleryScreen
import com.smartnas.app.ui.screens.photo.PhotoUploadScreen
import com.smartnas.app.ui.screens.profile.ProfileScreen
import com.smartnas.app.ui.screens.recycle.RecycleScreen
import com.smartnas.app.ui.screens.settings.SettingsScreen
import com.smartnas.app.ui.screens.tags.TagsScreen
import com.smartnas.app.ui.screens.video.VideoScreen

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val PHOTO_GALLERY = "photo_gallery"
    const val PHOTO_UPLOAD = "photo_upload"
    const val PHOTO_DETAIL = "photo_detail/{photoId}"
    const val VIDEO = "video"
    const val AI_CHAT = "ai_chat"
    const val AI_CONVERSATION = "ai_conversation/{conversationId}"
    const val FILE = "file"
    const val BOOK = "book"
    const val FAMILY = "family"
    const val FRIEND = "friend"
    const val FACE = "face"
    const val TAGS = "tags"
    const val RECYCLE = "recycle"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"

    fun photoDetail(id: Long) = "photo_detail/$id"
    fun conversation(id: Long) = "ai_conversation/$id"
}

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startDestination: String = Routes.LOGIN
) {
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.LOGIN) {
            LoginScreen(navController = navController)
        }
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(Routes.PHOTO_GALLERY) {
            PhotoGalleryScreen(navController = navController)
        }
        composable(Routes.PHOTO_UPLOAD) {
            PhotoUploadScreen(navController = navController)
        }
        composable(
            Routes.PHOTO_DETAIL,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            PhotoDetailScreen(navController = navController, photoId = photoId)
        }
        composable(Routes.VIDEO) {
            VideoScreen(navController = navController)
        }
        composable(Routes.AI_CHAT) {
            AiChatScreen(navController = navController)
        }
        composable(
            Routes.AI_CONVERSATION,
            arguments = listOf(navArgument("conversationId") { type = NavType.LongType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getLong("conversationId") ?: 0L
            ConversationScreen(navController = navController, conversationId = id)
        }
        composable(Routes.FILE) {
            FileScreen(navController = navController)
        }
        composable(Routes.BOOK) {
            BookScreen(navController = navController)
        }
        composable(Routes.FAMILY) {
            FamilyScreen(navController = navController)
        }
        composable(Routes.FRIEND) {
            FriendScreen(navController = navController)
        }
        composable(Routes.FACE) {
            FaceScreen(navController = navController)
        }
        composable(Routes.TAGS) {
            TagsScreen(navController = navController)
        }
        composable(Routes.RECYCLE) {
            RecycleScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
 