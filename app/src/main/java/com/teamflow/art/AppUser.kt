package com.teamflow.art

data class AppUser(
    val uid: String? = null,
    val name: String? = null,
    val email: String? = null,
    val photoUrl: String? = null,
    val fcmToken: String? = null,
    val createdAt: Long? = null,
    val updatedAt: Long? = null,
    val bio: String? = null,
    val phone: String? = null
)
