package eu.redasurc.tsm.manager.domain

import org.springframework.security.core.userdetails.UsernameNotFoundException

open class RegistrationException: Exception{
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class EmailAlreadyRegisteredException: _root_ide_package_.eu.redasurc.tsm.manager.domain.RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class UsernameAlreadyRegisteredException: _root_ide_package_.eu.redasurc.tsm.manager.domain.RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

open class TokenException: _root_ide_package_.eu.redasurc.tsm.manager.domain.RegistrationException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}
class TokenExpiredException: _root_ide_package_.eu.redasurc.tsm.manager.domain.TokenException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}

class BruteForceException: UsernameNotFoundException {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
}