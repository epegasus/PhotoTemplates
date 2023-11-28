package dev.pegasus.regret.interfaces

import dev.pegasus.regret.enums.CaseType

interface RegretListener {
    /**
     * Returns a key-value pair when [.undo] or [.redo] is called
     *
     * @param key   the key to identify the returned value
     * @param value the value associated with the key
     */
    fun onDo(key: CaseType, value: Any?)

    /**
     * onCanDo() updates for every call to [.undo], [.redo] or [.add].
     * This callback is specifically useful for updating the states of undo and redo buttons.
     */
    fun onCanDo(canUndo: Boolean, canRedo: Boolean)
}