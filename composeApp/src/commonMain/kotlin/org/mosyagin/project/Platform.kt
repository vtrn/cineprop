package org.mosyagin.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform