package com.example.addictionreductionapp.utils

import android.content.pm.ApplicationInfo
import android.os.Build

object AppCategoryResolver {

    fun resolveCategory(packageName: String, info: ApplicationInfo? = null): String {
        if (info != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val apiCategory = when (info.category) {
                ApplicationInfo.CATEGORY_SOCIAL       -> "Social"
                ApplicationInfo.CATEGORY_VIDEO        -> "Video"
                ApplicationInfo.CATEGORY_NEWS         -> "News"
                ApplicationInfo.CATEGORY_MAPS         -> "Maps"
                ApplicationInfo.CATEGORY_IMAGE        -> "Photos"
                ApplicationInfo.CATEGORY_GAME         -> "Games"
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> "Productivity"
                else                                  -> null
            }
            if (apiCategory != null) return apiCategory
        }

        return resolveByPackageName(packageName)
    }

    private fun resolveByPackageName(packageName: String): String {
        return when {
            packageName.contains("instagram") ||
            packageName.contains("facebook") ||
            packageName.contains("twitter") ||
            packageName.contains("snapchat") ||
            packageName.contains("tiktok") ||
            packageName.contains("musically") ||
            packageName.contains("linkedin") ||
            packageName.contains("pinterest") ||
            packageName.contains("reddit") ||
            packageName.contains("discord")     -> "Social"

            packageName.contains("youtube") ||
            packageName.contains("netflix") ||
            packageName.contains("hotstar") ||
            packageName.contains("primevideo") ||
            packageName.contains("hulu") ||
            packageName.contains("twitch")      -> "Entertainment"

            packageName.contains("whatsapp") ||
            packageName.contains("telegram") ||
            packageName.contains("signal") ||
            packageName.contains("messenger")   -> "Messaging"

            packageName.contains("chrome") ||
            packageName.contains("firefox") ||
            packageName.contains("brave") ||
            packageName.contains("opera") ||
            packageName.contains("samsung.internet") -> "Browser"

            packageName.contains("gmail") ||
            packageName.contains("outlook") ||
            packageName.contains("email")       -> "Productivity"

            packageName.contains("game") ||
            packageName.contains("play.games")  -> "Games"

            else                                -> "Other"
        }
    }
}
