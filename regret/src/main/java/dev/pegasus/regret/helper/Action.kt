package dev.pegasus.regret.helper

import dev.pegasus.regret.enums.CaseType

class Action(val key: CaseType, val value: Any) {

    override fun equals(other: Any?): Boolean {
        return other is Action && other.key == key && other.value == value
    }

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
}