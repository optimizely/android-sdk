package com.optimizely.ab.android.datafile_handler

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.optimizely.ab.android.shared.DatafileConfig
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito

/**
 * Tests for [DefaultDatafileHandler]
 */
@RunWith(AndroidJUnit4::class)
class DefaultDatafileHandlerTest {
    var datafileHandler: DatafileHandler = Mockito.mock(DefaultDatafileHandler::class.java)
    @Before
    fun setup() {
        datafileHandler = DefaultDatafileHandler()
    }

    @Test
    @Throws(Exception::class)
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        Assert.assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.packageName)
    }

    @Test
    @Throws(Exception::class)
    fun testSaveExistsRemoveWithoutEnvironment() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val projectId = DatafileConfig("1", null)
        datafileHandler.saveDatafile(appContext, projectId, "{}")
        Assert.assertTrue(datafileHandler.isDatafileSaved(appContext, projectId)!!)
        Assert.assertNotNull(datafileHandler.loadSavedDatafile(appContext, projectId))
        datafileHandler.removeSavedDatafile(appContext, projectId)
        Assert.assertFalse(datafileHandler.isDatafileSaved(appContext, projectId)!!)
        Assert.assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.packageName)
    }

    @Test
    @Throws(Exception::class)
    fun testSaveExistsRemoveWithEnvironments() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val projectId = DatafileConfig("1", "2")
        datafileHandler.saveDatafile(appContext, projectId, "{}")
        Assert.assertTrue(datafileHandler.isDatafileSaved(appContext, projectId)!!)
        Assert.assertNotNull(datafileHandler.loadSavedDatafile(appContext, projectId))
        datafileHandler.removeSavedDatafile(appContext, projectId)
        Assert.assertFalse(datafileHandler.isDatafileSaved(appContext, projectId)!!)
        Assert.assertEquals("com.optimizely.ab.android.datafile_handler.test", appContext.packageName)
    }

    @Test
    @Throws(Exception::class)
    fun testDownload() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val datafile = datafileHandler.downloadDatafile(appContext, DatafileConfig("1", null))
        Assert.assertNull(datafile)
    }

    @Test
    @Throws(Exception::class)
    fun testDownloadWithEnvironmemt() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        val datafile = datafileHandler.downloadDatafile(appContext, DatafileConfig("1", "2"))
        Assert.assertNull(datafile)
    }

    @Test
    @Throws(Exception::class)
    fun testAsyncDownloadWithoutEnvironment() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        datafileHandler.downloadDatafile(appContext, DatafileConfig("1", null), object : DatafileLoadedListener {
            override fun onDatafileLoaded(dataFile: String?) {
                Assert.assertNull(dataFile)
            }
        })
    }

    @Test
    @Throws(Exception::class)
    fun testAsyncDownloadWithEnvironment() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        datafileHandler.downloadDatafile(appContext, DatafileConfig("1", "2"), object : DatafileLoadedListener {
            override fun onDatafileLoaded(dataFile: String?) {
                Assert.assertNull(dataFile)
            }
        })
    }

    @Test
    @Throws(Exception::class)
    fun testBackgroundWithoutEnvironment() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        datafileHandler.startBackgroundUpdates(appContext, DatafileConfig("1", null), 24 * 60 * 60L, null)
        Assert.assertTrue(true)
        datafileHandler.stopBackgroundUpdates(appContext, DatafileConfig("1", null))
        Assert.assertTrue(true)
    }

    @Test
    @Throws(Exception::class)
    fun testBackgroundWithEnvironment() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        datafileHandler.startBackgroundUpdates(appContext, DatafileConfig("1", "2"), 24 * 60 * 60L, null)
        Assert.assertTrue(true)
        datafileHandler.stopBackgroundUpdates(appContext, DatafileConfig("1", "2"))
        Assert.assertTrue(true)
    }
}