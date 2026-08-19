package com.sudhanshu.tva.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sudhanshu.tva.data.ProfileRepository
import com.sudhanshu.tva.data.UserProfile
import com.sudhanshu.tva.ui.screens.AIAnalysisScreen
import com.sudhanshu.tva.ui.screens.AnomaliesScreen
import com.sudhanshu.tva.ui.screens.ControlRoomScreen
import com.sudhanshu.tva.ui.screens.MultiverseScreen
import com.sudhanshu.tva.ui.screens.PeopleScreen
import com.sudhanshu.tva.ui.screens.PlaceholderScreen
import com.sudhanshu.tva.ui.screens.TemporalSearchScreen
import com.sudhanshu.tva.ui.screens.DeviceSyncScreen
import com.sudhanshu.tva.ui.screens.AIChatScreen
import com.sudhanshu.tva.ui.screens.CameraVisionScreen
import com.sudhanshu.tva.ui.screens.ContactsSyncScreen
import com.sudhanshu.tva.ui.screens.UsageInsightsScreen
import com.sudhanshu.tva.ui.screens.TimelineScreen
import com.sudhanshu.tva.ui.screens.onboarding.ConsentScreen
import com.sudhanshu.tva.ui.screens.onboarding.PermissionRequestScreen
import com.sudhanshu.tva.ui.screens.onboarding.ProfileSetupScreen
import kotlinx.coroutines.launch

@Composable
fun TvaNavGraph(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val repository = remember(context) { ProfileRepository(context) }
    val profile by repository.profileFlow.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    val startDestination = TvaDestinations.CONSENT

    NavHost(navController = navController, startDestination = startDestination) {

        composable(TvaDestinations.CONSENT) {
            // If onboarding is already done, skip straight to Control Room.
            LaunchedEffect(profile) {
                if (profile?.onboardingComplete == true) {
                    navController.navigate(TvaDestinations.CONTROL_ROOM) {
                        popUpTo(TvaDestinations.CONSENT) { inclusive = true }
                    }
                }
            }
            ConsentScreen { _, deviceSignals, calendar, historical, continuousSync ->
                navController.navigate(
                    "${TvaDestinations.PROFILE_SETUP}?device=$deviceSignals&cal=$calendar&hist=$historical&sync=$continuousSync"
                )
            }
        }

        composable("${TvaDestinations.PROFILE_SETUP}?device={device}&cal={cal}&hist={hist}&sync={sync}") { backStackEntry ->
            val device = backStackEntry.arguments?.getString("device")?.toBoolean() ?: false
            val cal = backStackEntry.arguments?.getString("cal")?.toBoolean() ?: false
            val hist = backStackEntry.arguments?.getString("hist")?.toBoolean() ?: false
            val sync = backStackEntry.arguments?.getString("sync")?.toBoolean() ?: false

            ProfileSetupScreen { name, dob, bio ->
                scope.launch {
                    repository.saveProfile(
                        UserProfile(
                            name = name,
                            dateOfBirth = dob,
                            bio = bio,
                            consentBasicProfile = true,
                            consentDeviceSignals = device,
                            consentCalendarActivity = cal,
                            consentHistoricalImport = hist,
                            consentContinuousSync = sync,
                            onboardingComplete = true,
                            profileCreatedAtEpochMillis = System.currentTimeMillis()
                        )
                    )
                    navController.navigate(
                        "${TvaDestinations.PERMISSION_REQUEST}?device=$device&cal=$cal"
                    ) {
                        popUpTo(TvaDestinations.CONSENT) { inclusive = true }
                    }
                }
            }
        }

        composable("${TvaDestinations.PERMISSION_REQUEST}?device={device}&cal={cal}") { backStackEntry ->
            val device = backStackEntry.arguments?.getString("device")?.toBoolean() ?: false
            val cal = backStackEntry.arguments?.getString("cal")?.toBoolean() ?: false

            // Step 7: if neither category was consented to, skip straight through —
            // there's nothing to request.
            if (!device && !cal) {
                LaunchedEffect(Unit) {
                    navController.navigate(TvaDestinations.DEVICE_NAME) {
                        popUpTo(TvaDestinations.PERMISSION_REQUEST) { inclusive = true }
                    }
                }
            } else {
                PermissionRequestScreen(
                    needsCalendarActivity = cal,
                    needsDeviceSignals = device,
                    onDone = {
                        navController.navigate(TvaDestinations.DEVICE_NAME) {
                            popUpTo(TvaDestinations.PERMISSION_REQUEST) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(TvaDestinations.DEVICE_NAME) {
            val deviceIdentity = remember(context) { com.sudhanshu.tva.data.DeviceIdentity(context) }
            var defaultName by remember { mutableStateOf("My Device") }
            LaunchedEffect(Unit) {
                defaultName = deviceIdentity.getDeviceNameOrDefault()
            }
            com.sudhanshu.tva.ui.screens.onboarding.DeviceNameScreen(defaultName = defaultName) { chosenName ->
                scope.launch {
                    deviceIdentity.setDeviceName(chosenName)
                    navController.navigate(TvaDestinations.CONTROL_ROOM) {
                        popUpTo(TvaDestinations.DEVICE_NAME) { inclusive = true }
                    }
                }
            }
        }

        composable(TvaDestinations.CONTROL_ROOM) {
            ControlRoomScreen(onNavigate = { route -> navController.navigate(route) })
        }
        composable(TvaDestinations.TIMELINE) {
            TimelineScreen()
        }
        composable(TvaDestinations.MULTIVERSE) {
            MultiverseScreen()
        }
        composable(TvaDestinations.PEOPLE) {
            PeopleScreen()
        }
        composable(TvaDestinations.VARIANTS) {
            PlaceholderScreen("Variants", "Open a person from the People tab, then tap 'View / Add Variants' — variants are per-person (Step 11)")
        }
        composable(TvaDestinations.EVENTS) {
            PlaceholderScreen("Events", "Events live in the Timeline tab — this section was folded into Timeline/Multiverse (Steps 9-10-12) rather than getting its own screen")
        }
        composable(TvaDestinations.ANOMALIES) {
            AnomaliesScreen()
        }
        composable(TvaDestinations.AI_ANALYSIS) {
            AIAnalysisScreen()
        }
        composable(TvaDestinations.TEMPORAL_SEARCH) {
            TemporalSearchScreen()
        }
        composable(TvaDestinations.DEVICE_SYNC) {
            DeviceSyncScreen()
        }
        composable(TvaDestinations.AI_CHAT) {
            AIChatScreen()
        }
        composable(TvaDestinations.CAMERA_VISION) {
            CameraVisionScreen()
        }
        composable(TvaDestinations.CONTACTS_SYNC) {
            ContactsSyncScreen()
        }
        composable(TvaDestinations.USAGE_INSIGHTS) {
            UsageInsightsScreen()
        }
    }
}
