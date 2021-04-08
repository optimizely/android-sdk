package com.optimizely.ab.android.datafile_handler

import android.content.Context
import com.optimizely.ab.android.datafile_handler.DefaultDatafileHandler
import com.optimizely.ab.android.shared.DatafileConfig
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
@RunWith(PowerMockRunner::class)
@PrepareForTest(DefaultDatafileHandler::class)
class DefaultDatafileHandlerUnitTest {
    var handler: DatafileHandler = PowerMockito.mock(DefaultDatafileHandler::class.java)
    @Test
    @Throws(Exception::class)
    fun testHandler() {
        handler = DefaultDatafileHandler()
        val context = PowerMockito.mock(Context::class.java)
        PowerMockito.`when`(context.applicationContext).thenReturn(context)
        Assert.assertFalse(handler.isDatafileSaved(context, DatafileConfig("1", null))!!)
    }
}