package com.example.aicamera.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.aicamera.ui.screen.album.AlbumListScreen
import com.example.aicamera.ui.screen.album.PhotoDetailScreen
import com.example.aicamera.ui.screen.camera.CameraScreen
import com.example.aicamera.ui.screen.copywriting.CopywritingDetailScreen
import com.example.aicamera.ui.screen.copywriting.CopywritingListScreen
import com.example.aicamera.ui.viewmodel.album.AlbumListViewModel
import com.example.aicamera.ui.viewmodel.album.PhotoDetailViewModel
import com.example.aicamera.ui.viewmodel.camera.CameraViewModel
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingDetailViewModel
import com.example.aicamera.ui.viewmodel.copywriting.CopywritingListViewModel

/**
 * AppNavGraph：应用内页面导航。
 */
@Composable
fun AppNavGraph(
    cameraViewModel: CameraViewModel,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Routes.Camera,
        modifier = modifier
    ) {
        composable(Routes.Camera) {
            CameraScreen(
                viewModel = cameraViewModel,
                lifecycleOwner = lifecycleOwner,
                onNavigateToAlbum = { navController.navigate(Routes.AlbumList) },
            )
        }

        composable(Routes.AlbumList) {
            val albumVm: AlbumListViewModel = viewModel()
            AlbumListScreen(
                viewModel = albumVm,
                onBack = { navController.popBackStack() },
                onPhotoClick = { photoId ->
                    navController.navigate(Routes.photoDetail(photoId))
                },
                onNavigateToCopywriting = { navController.navigate(Routes.CopywritingList) }
            )
        }

        composable(
            route = Routes.PhotoDetail,
            arguments = listOf(navArgument("photoId") { type = NavType.LongType })
        ) { backStackEntry ->
            val photoId = backStackEntry.arguments?.getLong("photoId") ?: 0L
            val detailVm: PhotoDetailViewModel = viewModel()
            PhotoDetailScreen(
                viewModel = detailVm,
                photoId = photoId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CopywritingList) {
            val vm: CopywritingListViewModel = viewModel()
            CopywritingListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
                onItemClick = { id ->
                    navController.navigate(Routes.copywritingDetail(id))
                }
            )
        }

        composable(
            route = Routes.CopywritingDetail,
            arguments = listOf(navArgument("copywritingId") { type = NavType.LongType })
        ) { backStackEntry ->
            val copywritingId = backStackEntry.arguments?.getLong("copywritingId") ?: 0L
            val vm: CopywritingDetailViewModel = viewModel()
            CopywritingDetailScreen(
                viewModel = vm,
                copywritingId = copywritingId,
                onBack = { navController.popBackStack() },
                onPhotoClick = { photoId ->
                    navController.navigate(Routes.photoDetail(photoId))
                }
            )
        }
    }
}
