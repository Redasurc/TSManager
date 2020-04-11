package eu.redasurc.tsm.manager.security

import javax.servlet.http.HttpServletRequest

fun getClientIP(request: HttpServletRequest): String {
    val xfHeader: String = request.getHeader("X-Forwarded-For") ?: return request.remoteAddr
    // TODO: filter for XF headers only from trusted IP's
    return xfHeader.split(",").toTypedArray()[0]
}