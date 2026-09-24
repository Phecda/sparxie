package me.phecda.sparxie.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object ClientHome : NavKey

@Serializable
data object ServerHome : NavKey

@Serializable
data object ServerBindAddress : NavKey

@Serializable
data object MoreHome : NavKey

@Serializable
data object Settings : NavKey

@Serializable
data object Licenses : NavKey

@Serializable
data class LicenseDetail(val licenseId: String) : NavKey
