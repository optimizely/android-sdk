// Copyright 2022, Optimizely, Inc. and contributors
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.optimizely.ab.android.odp

import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class VuidManagerTest {
    private lateinit var vuidManager: VuidManager
    private val context = getInstrumentation().targetContext!!

    @Before
    fun setUp() {
        // remove vuid storage
        cleanSharedPrefs()
        // remove a singleton instance
        VuidManager.removeSharedForTesting()

        vuidManager = VuidManager.getShared(context)
    }

    @After
    fun cleanSharedPrefs() {
        val sharedPreferences: SharedPreferences = context.getSharedPreferences("optly", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.commit()
    }

    @Test
    fun makeVuid() {
        val vuid = vuidManager.makeVuid()
        assertTrue(vuid.length == 32)
        assertTrue(vuid.startsWith("vuid_", ignoreCase = false))
        assertTrue(
            "generated vuid should be all lowercase",
            vuid.lowercase(Locale.getDefault()).equals(vuid, ignoreCase = false)
        )
    }

    @Test
    fun isVuid() {
        assertTrue(VuidManager.isVuid("vuid_1234"))
        assertTrue(VuidManager.isVuid("VUID_1234"))
        assertFalse(VuidManager.isVuid("vuid1234"))
        assertFalse(VuidManager.isVuid("1234"))
        assertFalse(VuidManager.isVuid(""))
    }

    @Test
    fun loadBeforeSave() {
        val vuid1 = vuidManager.load(context)
        assertTrue("new vuid is created", VuidManager.isVuid(vuid1))
        val vuid2 = vuidManager.load(context)
        assertEquals("old vuid should be returned since load will save a created vuid", vuid1, vuid2)
    }

    @Test
    fun loadAfterSave() {
        vuidManager.save(context, "vuid_1234")
        val vuidLoaded = vuidManager.load(context)
        assertEquals("saved vuid should be returned", vuidLoaded, "vuid_1234")
        val vuidLoaded2 = vuidManager.load(context)
        assertEquals("the same vuid should be returned", vuidLoaded2, "vuid_1234")
    }

    @Test
    fun autoLoaded() {
        val vuidManager1 = VuidManager(context, true)
        val vuid1 = vuidManager1.vuid
        assertTrue("vuid should be auto loaded when constructed", vuid1.startsWith("vuid_"))
        val savedVuid = vuidManager1.load(context)
        assertEquals("Vuid should be same", vuid1, savedVuid)

        val vuidManager2 = VuidManager(context, false)
        val vuid2 = vuidManager2.vuid
        assertTrue(vuid2 == "")
        assertFalse("vuid should be auto loaded when constructed", vuid2.startsWith("vuid_"))



//        val vuid2 = VuidManager.getShared(context).vuid
//        assertEquals("the same vuid should be returned when getting a singleton", vuid1, vuid2)
//
//        // remove shared instance, so will be re-instantiated
//        VuidManager.removeSharedForTesting()
//
//        val vuid3 = VuidManager.getShared(context).vuid
//        assertEquals("the saved vuid should be returned when instantiated again", vuid2, vuid3)
//
//        // remove saved vuid
//        cleanSharedPrefs()
//        // remove shared instance, so will be re-instantiated
//        VuidManager.removeSharedForTesting()
//
//        val vuid4 = VuidManager.getShared(context).vuid
//        assertNotEquals("a new vuid should be returned when storage cleared and re-instantiated", vuid3, vuid4)
//        assertTrue(vuid4.startsWith("vuid_"))
    }
}
