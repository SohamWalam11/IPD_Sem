package com.tyreguard.repository

import com.tyreguard.model.Notification
import com.tyreguard.model.ServiceCenter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationRepository : JpaRepository<Notification, String> {
    fun findByUserId(userId: String): List<Notification>
    fun findByUserIdAndReadAtIsNull(userId: String): List<Notification>
}

@Repository
interface ServiceCenterRepository : JpaRepository<ServiceCenter, String> {
    fun findAll(): List<ServiceCenter>
}
