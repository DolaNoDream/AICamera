package com.example.aicamera.repository.album

import com.example.aicamera.data.db.dao.AlbumPhotoDao
import com.example.aicamera.data.db.entity.AlbumPhotoEntity
import com.example.aicamera.data.repository.AlbumRepositoryImpl
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlbumRepositoryImplTest {

    @Test
    fun `observeAllPhotos delegates to dao`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        val expected = listOf(AlbumPhotoEntity(id = 1, filePath = "content://1"))
        every { dao.observeAll() } returns flowOf(expected)

        val repo = AlbumRepositoryImpl(dao)

        val result = repo.observeAllPhotos()
        // Just verify it is the same flow by collecting once
        assertEquals(expected, result.firstValue())
    }

    @Test
    fun `insertPhoto delegates to dao insertIgnore`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        coEvery { dao.insertIgnore(any()) } returns 7L

        val repo = AlbumRepositoryImpl(dao)
        val id = repo.insertPhoto(AlbumPhotoEntity(filePath = "content://x"))

        assertEquals(7L, id)
        coVerify(exactly = 1) { dao.insertIgnore(any()) }
    }

    @Test
    fun `getPhotosByIds returns empty and does not hit dao when ids empty`() = runTest {
        val dao = mockk<AlbumPhotoDao>(relaxed = true)
        val repo = AlbumRepositoryImpl(dao)

        val result = repo.getPhotosByIds(emptyList())

        assertEquals(emptyList<AlbumPhotoEntity>(), result)
        coVerify(exactly = 0) { dao.getByIds(any()) }
    }

    @Test
    fun `getPhotosByIds delegates to dao when ids not empty`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        val expected = listOf(AlbumPhotoEntity(id = 1, filePath = "content://1"))
        coEvery { dao.getByIds(listOf(1L, 2L)) } returns expected

        val repo = AlbumRepositoryImpl(dao)
        val result = repo.getPhotosByIds(listOf(1L, 2L))

        assertEquals(expected, result)
        coVerify(exactly = 1) { dao.getByIds(listOf(1L, 2L)) }
    }

    @Test
    fun `updateTextByIds delegates to dao`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        coEvery { dao.updateTextByIds(any(), any()) } returns 3

        val repo = AlbumRepositoryImpl(dao)
        val count = repo.updateTextByIds(listOf(1L, 2L), "hi")

        assertEquals(3, count)
        coVerify(exactly = 1) { dao.updateTextByIds(listOf(1L, 2L), "hi") }
    }

    @Test
    fun `deletePhotosByIds delegates to dao`() = runTest {
        val dao = mockk<AlbumPhotoDao>()
        coEvery { dao.deleteByIds(any()) } returns 2

        val repo = AlbumRepositoryImpl(dao)
        val count = repo.deletePhotosByIds(listOf(1L, 2L))

        assertEquals(2, count)
        coVerify(exactly = 1) { dao.deleteByIds(listOf(1L, 2L)) }
    }
}

// helper: collect first emission
private fun <T> kotlinx.coroutines.flow.Flow<T>.firstValue(): T = kotlinx.coroutines.runBlocking { this@firstValue.first() }
