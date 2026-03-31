package com.example.aicamera.repository.copywriting

import com.example.aicamera.data.repository.CopywritingRepositoryImpl
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CopywritingRepositoryImplValidateTest {

    private fun repo(): CopywritingRepositoryImpl {
        return CopywritingRepositoryImpl(
            database = mockk(relaxed = true),
            albumPhotoDao = mockk(relaxed = true),
            copywritingDao = mockk(relaxed = true),
            relationDao = mockk(relaxed = true)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createCopywriting rejects blank content`() = runTest {
        repo().createCopywriting("   ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createCopywritingForPhoto rejects invalid photo id`() = runTest {
        repo().createCopywritingForPhoto(0, "hi")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `createCopywritingForPhotos rejects empty ids`() = runTest {
        repo().createCopywritingForPhotos(emptyList(), "hi")
    }
}
