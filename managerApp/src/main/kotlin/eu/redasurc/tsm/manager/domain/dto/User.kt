package eu.redasurc.tsm.manager.domain.dto

data class User (var username: String?, var email: String?, var pw: String?, var pwConfirm: String?) {
    // No args constructor for Spring
    constructor():this("","","","")
}