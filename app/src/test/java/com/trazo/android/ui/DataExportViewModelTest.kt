package com.trazo.android.ui

import com.trazo.android.data.api.ExportApi
import com.trazo.android.ui.account.DataExportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

@OptIn(ExperimentalCoroutinesApi::class)
class DataExportViewModelTest {
    private class Api(var answer: () -> Response<ResponseBody>) : ExportApi {
        override suspend fun export(): Response<ResponseBody> = answer()
    }

    private val file = """{"version":"2"}"""
    private val api = Api { Response.success(file.toResponseBody(null)) }
    private val dispatcher = UnconfinedTestDispatcher()
    private fun viewModel() = DataExportViewModel(api, dispatcher)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the downloaded file is written where the person chooses`() {
        val vm = viewModel()
        vm.download()
        assertTrue(vm.state.value.readyToSave)

        val target = ByteArrayOutputStream()
        vm.save { target }

        assertEquals(file, target.toString("UTF-8"))
        assertTrue(vm.state.value.saved)
        assertFalse(vm.state.value.readyToSave)
    }

    @Test
    fun `cancelling the choice drops the data`() {
        val vm = viewModel()
        vm.download()

        vm.save(null)
        val target = ByteArrayOutputStream()
        vm.save { target }

        assertEquals(0, target.size())
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun `a destination that cannot be written is reported`() {
        val vm = viewModel()
        vm.download()

        vm.save { object : OutputStream() { override fun write(b: Int) = throw IOException("full") } }

        assertTrue(vm.state.value.saveFailed)
        assertFalse(vm.state.value.saved)
    }

    @Test
    fun `no destination is reported too`() {
        val vm = viewModel()
        vm.download()

        vm.save { null }

        assertTrue(vm.state.value.saveFailed)
    }

    @Test
    fun `without a connection nothing is offered to save, and it can be tried again`() {
        api.answer = { throw IOException("offline") }
        val vm = viewModel()
        vm.download()
        assertNotNull(vm.state.value.error)
        assertFalse(vm.state.value.readyToSave)

        api.answer = { Response.success(file.toResponseBody(null)) }
        vm.download()

        assertTrue(vm.state.value.readyToSave)
    }
}
