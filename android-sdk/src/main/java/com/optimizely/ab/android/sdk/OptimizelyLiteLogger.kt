/****************************************************************************
 * Copyright 2017-2018, Optimizely, Inc. and contributors                   *
 * *
 * Licensed under the Apache License, Version 2.0 (the "License");          *
 * you may not use this file except in compliance with the License.         *
 * You may obtain a copy of the License at                                  *
 * *
 * http://www.apache.org/licenses/LICENSE-2.0                            *
 * *
 * Unless required by applicable law or agreed to in writing, software      *
 * distributed under the License is distributed on an "AS IS" BASIS,        *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. *
 * See the License for the specific language governing permissions and      *
 * limitations under the License.                                           *
 */
package com.optimizely.ab.android.sdk

import android.util.Log
import org.slf4j.Logger
import org.slf4j.Marker

class OptimizelyLiteLogger(private val tag: String) : Logger {
    override fun getName(): String {
        return "OptimizelyLiteLogger"
    }

    override fun isTraceEnabled(): Boolean {
        return false
    }

    override fun trace(msg: String) {}
    override fun trace(format: String, arg: Any) {}
    override fun trace(format: String, arg1: Any, arg2: Any) {}
    override fun trace(format: String, vararg arguments: Any) {}
    override fun trace(msg: String, t: Throwable) {}
    override fun isTraceEnabled(marker: Marker): Boolean {
        return false
    }

    override fun trace(marker: Marker, msg: String) {}
    override fun trace(marker: Marker, format: String, arg: Any) {}
    override fun trace(marker: Marker, format: String, arg1: Any, arg2: Any) {}
    override fun trace(marker: Marker, format: String, vararg argArray: Any) {}
    override fun trace(marker: Marker, msg: String, t: Throwable) {}
    override fun isDebugEnabled(): Boolean {
        return if (BuildConfig.DEBUG) {
            true
        } else false
    }

    override fun debug(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, msg)
        }
    }

    override fun debug(format: String, arg: Any) {
        Log.d(tag, formatMessage(format, arg))
    }

    override fun debug(format: String, arg1: Any, arg2: Any) {
        Log.d(tag, formatMessage(format, arg1, arg2))
    }

    override fun debug(format: String, vararg arguments: Any) {
        Log.d(tag, formatMessage(format, *arguments))
    }

    override fun debug(msg: String, t: Throwable) {
        Log.d(tag, formatThrowable(msg, t))
    }

    override fun isDebugEnabled(marker: Marker): Boolean {
        return false
    }

    override fun debug(marker: Marker, msg: String) {
        Log.d(tag, msg)
    }

    override fun debug(marker: Marker, format: String, arg: Any) {
        Log.d(tag, formatMessage(format, arg))
    }

    override fun debug(marker: Marker, format: String, arg1: Any, arg2: Any) {
        Log.d(tag, formatMessage(format, arg1, arg2))
    }

    override fun debug(marker: Marker, format: String, vararg arguments: Any) {
        Log.d(tag, formatMessage(format, *arguments))
    }

    override fun debug(marker: Marker, msg: String, t: Throwable) {
        Log.d(tag, formatThrowable(msg, t))
    }

    override fun isInfoEnabled(): Boolean {
        return if (BuildConfig.DEBUG) {
            true
        } else false
    }

    override fun info(msg: String) {
        Log.i(tag, msg)
    }

    override fun info(format: String, arg: Any) {
        Log.i(tag, formatMessage(format, arg))
    }

    override fun info(format: String, arg1: Any, arg2: Any) {
        Log.i(tag, formatMessage(format, arg1, arg2))
    }

    override fun info(format: String, vararg arguments: Any) {
        Log.i(tag, formatMessage(format, *arguments))
    }

    override fun info(msg: String, t: Throwable) {
        Log.i(tag, formatThrowable(msg, t))
    }

    override fun isInfoEnabled(marker: Marker): Boolean {
        return false
    }

    override fun info(marker: Marker, msg: String) {
        Log.i(tag, msg)
    }

    override fun info(marker: Marker, format: String, arg: Any) {
        Log.i(tag, formatMessage(format, arg))
    }

    override fun info(marker: Marker, format: String, arg1: Any, arg2: Any) {
        Log.i(tag, formatMessage(format, arg1, arg2))
    }

    override fun info(marker: Marker, format: String, vararg arguments: Any) {
        Log.i(tag, formatMessage(format, *arguments))
    }

    override fun info(marker: Marker, msg: String, t: Throwable) {
        Log.i(tag, formatThrowable(msg, t))
    }

    override fun isWarnEnabled(): Boolean {
        return true
    }

    override fun warn(msg: String) {
        Log.w(tag, msg)
    }

    override fun warn(format: String, arg: Any) {
        Log.w(tag, formatMessage(format, arg))
    }

    override fun warn(format: String, vararg arguments: Any) {
        Log.w(tag, formatMessage(format, *arguments))
    }

    override fun warn(format: String, arg1: Any, arg2: Any) {
        Log.w(tag, formatMessage(format, arg1, arg2))
    }

    override fun warn(msg: String, t: Throwable) {
        Log.w(tag, formatThrowable(msg, t))
    }

    override fun isWarnEnabled(marker: Marker): Boolean {
        return true
    }

    override fun warn(marker: Marker, msg: String) {
        Log.w(tag, msg)
    }

    override fun warn(marker: Marker, format: String, arg: Any) {
        Log.w(tag, formatMessage(format, arg))
    }

    override fun warn(marker: Marker, format: String, arg1: Any, arg2: Any) {
        Log.w(tag, formatMessage(format, arg1, arg2))
    }

    override fun warn(marker: Marker, format: String, vararg arguments: Any) {
        Log.w(tag, formatMessage(format, *arguments))
    }

    override fun warn(marker: Marker, msg: String, t: Throwable) {
        Log.w(tag, formatThrowable(msg, t))
    }

    override fun isErrorEnabled(): Boolean {
        return true
    }

    override fun error(msg: String) {
        Log.e(tag, msg)
    }

    override fun error(format: String, arg: Any) {
        Log.e(tag, formatMessage(format, arg))
    }

    override fun error(format: String, arg1: Any, arg2: Any) {
        Log.e(tag, formatMessage(format, arg1, arg2))
    }

    override fun error(format: String, vararg arguments: Any) {
        Log.e(tag, formatMessage(format, *arguments))
    }

    override fun error(msg: String, t: Throwable) {
        Log.e(tag, formatThrowable(msg, t))
    }

    override fun isErrorEnabled(marker: Marker): Boolean {
        return true
    }

    override fun error(marker: Marker, msg: String) {
        Log.e(tag, msg)
    }

    override fun error(marker: Marker, format: String, arg: Any) {
        Log.e(tag, formatMessage(format, arg))
    }

    override fun error(marker: Marker, format: String, arg1: Any, arg2: Any) {
        Log.e(tag, formatMessage(format, arg1, arg2))
    }

    override fun error(marker: Marker, format: String, vararg arguments: Any) {
        Log.e(tag, formatMessage(format, *arguments))
    }

    override fun error(marker: Marker, msg: String, t: Throwable) {
        Log.e(tag, formatThrowable(msg, t))
    }

    private fun formatMessage(format: String, vararg arguments: Any): String {
        var format = format
        for (arg in arguments) {
            format = format.replaceFirst("\\{\\}".toRegex(), arg.toString())
        }
        return format
    }

    private fun formatThrowable(msg: String, t: Throwable): String {
        return StringBuilder(msg).append("\n").append(t.message).toString()
    }
}