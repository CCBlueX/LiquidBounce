package net.ccbluex.liquidbounce.api.models.pagination

data class PaginatedResponse<T>(
    val items: List<T>,
    val pagination: Pagination
)
