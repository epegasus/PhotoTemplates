package dev.pegasus.regret.helper

import dev.pegasus.regret.enums.CaseType

class UndoRedoList {
    private var head: Node? = null
    private var pointer: Node? = null
    private var pointerIndex = 0

    /**
     * @return the size of the list
     */
    var size = 0
        private set

    private class Node(var action: Action) {
        var next: Node? = null
        var prev: Node? = null
    }

    /**
     * Adds an key-values pair data to the collection.
     * Both currentValue and newValue should be of the same key identifier
     */
    fun add(key: CaseType, currentValue: Any, newValue: Any) {
        val oldNode = Node(Action(key, currentValue))
        val newNode = Node(Action(key, newValue))
        if (head == null || pointer === head) {
            oldNode.next = newNode
            newNode.prev = oldNode
            head = oldNode
            pointerIndex = 2
        } else {
            if (pointer!!.action.key == key || pointer!!.prev!!.action.key == key) {
                newNode.prev = pointer
                pointer!!.next = newNode
                pointerIndex++
            } else {
                oldNode.next = newNode
                newNode.prev = oldNode
                pointer!!.next = oldNode
                oldNode.prev = pointer
                pointerIndex += 2
            }
        }
        size = pointerIndex
        pointer = newNode
    }

    /**
     * @return the previous [Action] object without moving the pointer
     * @throws NoSuchElementException if the previous object doesn't exist
     */
    val previous: Action
        get() {
            if (pointer == null) {
                throw NoSuchElementException()
            }
            return pointer!!.prev!!.action
        }

    /**
     * @return the next [Action] object without moving the pointer
     * @throws NoSuchElementException if the next object doesn't exist
     */
    val next: Action
        get() {
            if (pointer == null) {
                throw NoSuchElementException()
            }
            return pointer!!.next!!.action
        }

    /**
     * @return the current [Action] object which the pointer is pointing at
     * @throws NoSuchElementException if the current object doesn't exist because the list is empty
     */
    val current: Action
        get() {
            if (pointer == null) {
                throw NoSuchElementException()
            }
            return pointer!!.action
        }

    /**
     * Moves the pointer one step forward
     *
     * @return Returns the next [Action] object
     * @throws NoSuchElementException if the next object doesn't exist
     */
    fun redo(): Action? {
        if (pointer!!.next != null) {
            val tempPointer = pointer
            pointer = pointer!!.next
            pointerIndex++
            if (tempPointer!!.action.key == pointer!!.action.key) {
                return pointer!!.action
            } else if (pointer!!.next != null) {
                pointerIndex++
                pointer = pointer!!.next
                return pointer!!.action
            }
        }
        throw NoSuchElementException()
    }

    /**
     * Moves the pointer one step backwards
     *
     * @return Returns the previous [Action] object or null if next object doesn't exists
     * @throws NoSuchElementException if the previous object doesn't exist
     */
    fun undo(): Action {
        if (pointer!!.prev != null) {
            val tempPointer = pointer
            pointer = pointer!!.prev
            pointerIndex--
            if (tempPointer!!.action.key == pointer!!.action.key) {
                return pointer!!.action
            } else if (pointer!!.prev != null) {
                pointerIndex--
                pointer = pointer!!.prev
                return pointer!!.action
            }
        }
        throw NoSuchElementException()
    }

    /**
     * @return a boolean for whether a next element exists
     */
    fun canRedo(): Boolean {
        return pointer != null && pointer!!.next != null
    }

    /**
     * @return a boolean for whether a previous element exists
     */
    fun canUndo(): Boolean {
        return pointer != null && pointer!!.prev != null
    }

    /**
     * @return a boolean for whether the collection is empty or not
     */
    val isEmpty: Boolean
        get() = size == 0

    /**
     * Deletes all elements in the collection and sets the size to 0
     */
    fun clear() {
        head = null
        pointer = null
        size = 0
        pointerIndex = 0
    }

    /**
     * @return a string representation of all elements in the collection
     */
    override fun toString(): String {
        val sb = StringBuilder().append('{')
        var tempNode = head
        while (tempNode != null) {
            sb.append(String.format("%s=%s", tempNode.action.key, tempNode.action.value))
            tempNode = tempNode.next
            if (tempNode != null) {
                sb.append(',').append(' ')
            }
        }
        return sb.append('}').toString()
    }
}