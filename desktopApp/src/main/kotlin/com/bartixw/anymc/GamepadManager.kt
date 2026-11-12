/*
 * Copyright (C) 2026 BartixW
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.bartixw.anymc

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWGamepadState
import kotlinx.coroutines.*
import java.awt.Component
import java.awt.event.KeyEvent
import javax.swing.SwingUtilities
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

object GamepadManager {
    private var pollingJob: Job? = null
    // Key: (joystickId shl 16) or subId
    private val buttonStates = mutableMapOf<Int, Boolean>()
    private val axisStates = mutableMapOf<Int, Int>() 
    private val connectedIds = mutableSetOf<Int>()
    private var isGlfwInitialized = false

    fun startPolling(scope: CoroutineScope, window: Component) {
        pollingJob?.cancel()
        pollingJob = scope.launch(Dispatchers.IO) {
            if (!isGlfwInitialized) {
                if (!glfwInit()) {
                    println("GamepadManager: Failed to initialize GLFW")
                    return@launch
                }
                isGlfwInitialized = true
            }
            
            val state = GLFWGamepadState.calloc()
            try {
                while (isActive) {
                    var anyJoystickFound = false
                    for (i in GLFW_JOYSTICK_1..GLFW_JOYSTICK_16) {
                        if (glfwJoystickPresent(i)) {
                            anyJoystickFound = true
                            if (!connectedIds.contains(i)) {
                                val name = glfwGetJoystickName(i) ?: "Unknown"
                                println("GamepadManager: Connected at index $i: $name")
                                
                                val axes = glfwGetJoystickAxes(i)
                                if (axes != null) {
                                    val values = (0 until minOf(axes.limit(), 6)).joinToString { "Axis $it: ${axes.get(it)}" }
                                    println("GamepadManager: Initial axes for $i: $values")
                                }
                                
                                connectedIds.add(i)
                            }
                            
                            if (glfwJoystickIsGamepad(i)) {
                                if (glfwGetGamepadState(i, state)) {
                                    processGamepad(i, state, window)
                                }
                            } else {
                                processGenericJoystick(i, window)
                            }
                        } else if (connectedIds.contains(i)) {
                            println("GamepadManager: Disconnected at index $i")
                            connectedIds.remove(i)
                            val prefix = i shl 16
                            buttonStates.keys.filter { it and 0xFFFF0000.toInt() == prefix }.forEach { buttonStates.remove(it) }
                            axisStates.keys.filter { it and 0xFFFF0000.toInt() == prefix }.forEach { axisStates.remove(it) }
                        }
                    }
                    delay(if (anyJoystickFound) 16.milliseconds else 1.seconds)
                }
            } finally {
                state.free()
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        buttonStates.clear()
        axisStates.clear()
        connectedIds.clear()
    }

    private fun processGamepad(id: Int, state: GLFWGamepadState, window: Component) {
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_RIGHT).toInt() == GLFW_PRESS, KeyEvent.VK_RIGHT, window, 200)
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_DOWN).toInt() == GLFW_PRESS, KeyEvent.VK_DOWN, window, 201)
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_LEFT).toInt() == GLFW_PRESS, KeyEvent.VK_LEFT, window, 202)
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_DPAD_UP).toInt() == GLFW_PRESS, KeyEvent.VK_UP, window, 203)
        
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_A).toInt() == GLFW_PRESS, KeyEvent.VK_ENTER, window, 100)
        checkButton(id, state.buttons(GLFW_GAMEPAD_BUTTON_B).toInt() == GLFW_PRESS, KeyEvent.VK_ESCAPE, window, 101)
        
        checkAxis(id, state.axes(GLFW_GAMEPAD_AXIS_LEFT_X), KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, window, 0)
        checkAxis(id, state.axes(GLFW_GAMEPAD_AXIS_LEFT_Y), KeyEvent.VK_UP, KeyEvent.VK_DOWN, window, 1)
    }

    private fun processGenericJoystick(joystickId: Int, window: Component) {
        val buttons = glfwGetJoystickButtons(joystickId) ?: return
        val axes = glfwGetJoystickAxes(joystickId) ?: return
        
        checkJoystickButton(joystickId, buttons, 15, KeyEvent.VK_UP, window, 203)
        checkJoystickButton(joystickId, buttons, 17, KeyEvent.VK_DOWN, window, 201)
        checkJoystickButton(joystickId, buttons, 18, KeyEvent.VK_LEFT, window, 202)
        checkJoystickButton(joystickId, buttons, 16, KeyEvent.VK_RIGHT, window, 200)

        checkJoystickButton(joystickId, buttons, 0, KeyEvent.VK_ENTER, window, 100)
        checkJoystickButton(joystickId, buttons, 1, KeyEvent.VK_ESCAPE, window, 101)
        
        if (axes.limit() > 1) {
            checkAxis(joystickId, axes.get(0), KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, window, 0)
            checkAxis(joystickId, axes.get(1), KeyEvent.VK_UP, KeyEvent.VK_DOWN, window, 1)
        }
    }

    private fun checkButton(joystickId: Int, isPressed: Boolean, keyCode: Int, window: Component, subId: Int) {
        val stateId = (joystickId shl 16) or subId
        val wasPressed = buttonStates[stateId] ?: false
        
        if (isPressed && !wasPressed) {
            sendKeyEvent(window, KeyEvent.KEY_PRESSED, keyCode)
        } else if (!isPressed && wasPressed) {
            sendKeyEvent(window, KeyEvent.KEY_RELEASED, keyCode)
        }
        buttonStates[stateId] = isPressed
    }

    private fun checkJoystickButton(joystickId: Int, buttons: java.nio.ByteBuffer, buttonIndex: Int, keyCode: Int, window: Component, subId: Int) {
        if (buttonIndex >= buttons.limit()) return
        val isPressed = buttons.get(buttonIndex).toInt() == GLFW_PRESS
        checkButton(joystickId, isPressed, keyCode, window, subId)
    }

    private fun checkAxis(joystickId: Int, value: Float, negKeyCode: Int, posKeyCode: Int, window: Component, axisIndex: Int) {
        val deadzone = 0.6f
        val currentDir = when {
            value > deadzone -> 1
            value < -deadzone -> -1
            else -> 0
        }
        val stateId = (joystickId shl 16) or axisIndex
        val lastDir = axisStates[stateId] ?: 0
        
        if (currentDir != lastDir) {
            if (currentDir != 0) {
                val keyCode = if (currentDir == 1) posKeyCode else negKeyCode
                sendKeyEvent(window, KeyEvent.KEY_PRESSED, keyCode)
            } else {
                sendKeyEvent(window, KeyEvent.KEY_RELEASED, negKeyCode)
                sendKeyEvent(window, KeyEvent.KEY_RELEASED, posKeyCode)
            }
            axisStates[stateId] = currentDir
        }
    }

    private fun sendKeyEvent(window: Component, id: Int, keyCode: Int) {
        if (!window.isShowing) return

        val char = when (keyCode) {
            KeyEvent.VK_ENTER -> '\n'
            else -> KeyEvent.CHAR_UNDEFINED
        }

        SwingUtilities.invokeLater {
            val kfm = java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
            val target = kfm.focusOwner ?: window
            
            val event = KeyEvent(
                target,
                id,
                System.currentTimeMillis(),
                0, // No modifiers to keep it simple
                keyCode,
                char
            )
            
            // Re-dispatching to focused component
            target.dispatchEvent(event)
            
            if (id == KeyEvent.KEY_PRESSED && char != KeyEvent.CHAR_UNDEFINED) {
                val typedEvent = KeyEvent(
                    target,
                    KeyEvent.KEY_TYPED,
                    System.currentTimeMillis(),
                    0,
                    KeyEvent.VK_UNDEFINED,
                    char
                )
                target.dispatchEvent(typedEvent)
            }
        }
    }
}
