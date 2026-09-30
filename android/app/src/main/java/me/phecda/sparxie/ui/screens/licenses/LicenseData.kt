package me.phecda.sparxie.ui.screens.licenses

import me.phecda.sparxie.R

data class LicenseEntry(
    val id: String,
    val name: String,
    val licenseName: String,
    val licenseTextRes: Int,
)

val licenseEntries = listOf(
    LicenseEntry(
        id = "mmkv",
        name = "MMKV",
        licenseName = "BSD 3-Clause",
        licenseTextRes = R.raw.mmkv_license,
    ),
)
