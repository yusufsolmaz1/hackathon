package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.config.newToken
import com.hackathon.config.newUserId
import com.hackathon.config.sha256
import com.hackathon.model.AuthResponse
import com.hackathon.model.LoginRequest
import com.hackathon.model.RegisterRequest
import com.hackathon.model.UpdateProfileRequest
import com.hackathon.model.UserProfile
import com.hackathon.model.UserRow
import com.hackathon.repository.UserRepository

class AuthService(private val userRepo: UserRepository) {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    suspend fun login(req: LoginRequest): AuthResponse {
        if (req.email.isBlank() || req.password.isBlank()) {
            throw TrendException.badRequest("E-posta ve sifre gereklidir.")
        }
        val user = userRepo.findByEmail(req.email.trim().lowercase())
            ?: throw TrendException.unauthorized("E-posta veya sifre hatali.")
        if (user.passwordHash != sha256(req.password)) {
            throw TrendException.unauthorized("E-posta veya sifre hatali.")
        }
        val token = newToken()
        userRepo.createSession(token, user.id)
        return AuthResponse(token = token, user = user.toProfile())
    }

    suspend fun register(req: RegisterRequest): AuthResponse {
        val name = req.name.trim()
        val email = req.email.trim().lowercase()
        if (name.length < 2) throw TrendException.badRequest("Isim en az 2 karakter olmali.")
        if (!emailRegex.matches(email)) throw TrendException.badRequest("Gecersiz e-posta formati.")
        if (req.password.length < 6) throw TrendException.badRequest("Sifre en az 6 karakter olmali.")

        if (userRepo.findByEmail(email) != null) throw TrendException.emailExists()

        val user = UserRow(
            id = newUserId(),
            email = email,
            passwordHash = sha256(req.password),
            name = name,
            avatarUrl = null,
            avatarColorName = pickColor(name),
        )
        val saved = userRepo.create(user)
        val token = newToken()
        userRepo.createSession(token, saved.id)
        return AuthResponse(token = token, user = saved.toProfile())
    }

    suspend fun getProfile(userId: String): UserProfile {
        val user = userRepo.findById(userId) ?: throw TrendException.unauthorized()
        return user.toProfile()
    }

    suspend fun updateProfile(userId: String, req: UpdateProfileRequest): UserProfile {
        if (req.email != null && !emailRegex.matches(req.email)) {
            throw TrendException.badRequest("Gecersiz e-posta formati.")
        }
        if (req.email != null) {
            val existing = userRepo.findByEmail(req.email.lowercase())
            if (existing != null && existing.id != userId) throw TrendException.emailExists()
        }
        val updated = userRepo.updateProfile(
            id = userId,
            name = req.name?.takeIf { it.isNotBlank() }?.trim(),
            email = req.email?.lowercase(),
            avatarUrl = req.avatarUrl,
        )
        return updated.toProfile()
    }

    private fun UserRow.toProfile() = UserProfile(id, name, email, avatarUrl)

    private fun pickColor(seed: String): String {
        val palette = listOf("orange", "blue", "green", "purple", "pink", "brown", "red", "teal")
        return palette[(seed.hashCode().let { if (it < 0) -it else it }) % palette.size]
    }
}
