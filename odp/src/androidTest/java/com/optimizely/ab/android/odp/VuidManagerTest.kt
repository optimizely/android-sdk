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
import com.optimizely.ab.android.shared.OptlyStorage
import android.content.Context
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

        vuidManager = VuidManager(context, true)
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
    fun autoLoadedWhenVuidEnable() {
        // Save vuid for context
        val storage = OptlyStorage(context)
        storage.saveString("vuid", "vuid_123")

        val vuidManager = VuidManager(context, true)
        val vuid = vuidManager.getVuid()
        assertTrue("vuid should be auto loaded when constructed", vuid!!.startsWith("vuid_"))
        assertEquals("Vuid should be same", vuid, "vuid_123")
    }

    @Test
    fun autoRemoveWhenVuidDisable() {
        val vuidManager1 = VuidManager(context, true)
        val vuid1 = vuidManager1.getVuid()
        assertTrue("Vuid is created succesfully", vuid1!!.startsWith("vuid_"))
        val storage = OptlyStorage(context)
        val cachedVuid = storage.getString("vuid", null)
        assertEquals("Vuid should be same", vuid1, cachedVuid)

        /// Remove previous vuid when disable
        val vuidManager2 = VuidManager(context, false)
        assertNull(vuidManager2.getVuid());
        val cachedVuid2 = storage.getString("vuid", null)
        assertNull(cachedVuid2);

    }
}
