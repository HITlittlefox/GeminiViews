package com.example.geminiviews.prompttalk.utils

// Logger.kt

import android.util.Log

object Logger {

    // 控制是否启用日志输出的开关
    // 在生产环境中，可以将其设置为 false 或使用 BuildConfig.DEBUG 来控制
    private const val DEBUG_MODE = true // 或者根据 BuildConfig.DEBUG = BuildConfig.DEBUG

    // 默认的日志标签
    private const val DEFAULT_TAG = "GeminiApp"

    /**
     * 打印调试日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     */
    fun d(message: String?, tag: String = DEFAULT_TAG) {
        if (DEBUG_MODE && message != null) {
            Log.d(tag, message)
        }
    }

    /**
     * 打印错误日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     * @param throwable 相关的异常信息
     */
    fun e(message: String?, tag: String = DEFAULT_TAG, throwable: Throwable? = null) {
        if (DEBUG_MODE && message != null) {
            if (throwable != null) {
                Log.e(tag, message, throwable)
            } else {
                Log.e(tag, message)
            }
        }
    }

    /**
     * 打印信息日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     */
    fun i(message: String?, tag: String = DEFAULT_TAG) {
        if (DEBUG_MODE && message != null) {
            Log.i(tag, message)
        }
    }

    /**
     * 打印警告日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     */
    fun w(message: String?, tag: String = DEFAULT_TAG) {
        if (DEBUG_MODE && message != null) {
            Log.w(tag, message)
        }
    }

    /**
     * 打印详细日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     */
    fun v(message: String?, tag: String = DEFAULT_TAG) {
        if (DEBUG_MODE && message != null) {
            Log.v(tag, message)
        }
    }

    /**
     * 打印断言日志
     * @param message 要打印的消息
     * @param tag 日志标签，默认为 DEFAULT_TAG
     */
    fun wtf(message: String?, tag: String = DEFAULT_TAG) {
        if (DEBUG_MODE && message != null) {
            Log.wtf(tag, message)
        }
    }
}