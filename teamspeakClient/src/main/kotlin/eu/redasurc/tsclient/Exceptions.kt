package eu.redasurc.tsclient

open class TSException: Exception{
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}


class TSChannelNotFoundException: TSException{
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}
class TSClientNotFoundException: TSException{
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
    constructor(): super()
}