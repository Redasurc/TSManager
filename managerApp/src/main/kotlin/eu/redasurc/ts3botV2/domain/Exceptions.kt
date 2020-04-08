package eu.redasurc.ts3botV2.domain

import org.springframework.security.core.userdetails.UsernameNotFoundException

open class RegistrationException: Exception{
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class EmailAlreadyRegisteredException: RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class UsernameAlreadyRegisteredException: RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class TokenException: RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class BruteForceException: UsernameNotFoundException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
}