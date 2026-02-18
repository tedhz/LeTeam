package com.leteam.locked

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.leteam.locked.ui.screens.camera.CameraViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraViewModelTest {

    @Test
    fun photoUri_starts_null() {
        val vm = CameraViewModel()
        assertNull(vm.photoUri.value)
    }

    @Test
    fun setPhotoUri_updates_value() {
        val vm = CameraViewModel()
        val uri = Uri.parse("content://test/image.jpg")

        vm.setPhotoUri(uri)

        assertEquals(uri, vm.photoUri.value)
    }

    @Test
    fun setPhotoUri_can_clear_value() {
        val vm = CameraViewModel()
        val uri = Uri.parse("content://test/image.jpg")

        vm.setPhotoUri(uri)
        vm.setPhotoUri(null)

        assertNull(vm.photoUri.value)
    }
}
