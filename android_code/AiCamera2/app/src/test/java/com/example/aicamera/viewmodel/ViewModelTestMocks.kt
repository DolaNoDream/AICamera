package com.example.aicamera.viewmodel

import android.app.Application
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
open class ViewModelTestBase {

    protected val dispatcher = StandardTestDispatcher()

    @Before
    fun setUpMainDispatcher() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDownMainDispatcher() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    protected fun fakeApplication(): Application = io.mockk.mockk(relaxed = true)
}
