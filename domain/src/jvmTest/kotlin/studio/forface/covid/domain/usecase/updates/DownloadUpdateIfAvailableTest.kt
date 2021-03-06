@file:Suppress("EXPERIMENTAL_API_USAGE")

package studio.forface.covid.domain.usecase.updates

import com.soywiz.klock.DateTime
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.test.runBlockingTest
import studio.forface.covid.domain.*
import studio.forface.covid.domain.entity.BaseVersion
import studio.forface.covid.domain.entity.InstallableUpdateVersion
import studio.forface.covid.domain.entity.UpdateVersion
import studio.forface.covid.domain.gateway.UpdatesApi
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * __Unit__ test suite for [DownloadUpdateIfAvailable]
 * @author Davide Farella
 */
internal class DownloadUpdateIfAvailableTest {

    private val repository = MockUpdatesRepository()

    private fun buildDownloadUpdateIfAvailable(
        appVersion: BaseVersion,
        lastUpdateVersion: UpdateVersion,
        installableUpdateVersion: UpdateVersion,
        api: UpdatesApi? = null
    ): DownloadUpdateIfAvailable = DownloadUpdateIfAvailable(
        api = api ?: MockUpdatesApi(lastUpdateVersion = lastUpdateVersion),
        repository = repository,
        getAppVersion = MockGetAppVersion(appVersion),
        getInstallableUpdate = MockGetInstallableUpdate(version = InstallableUpdateVersion(mockk(), installableUpdateVersion)),
        buildDownloadableUpdateFileName = MockBuildDownloadableUpdateFileName()
    )

    private val time = DateTime.EPOCH

    @Test
    fun `update is downloaded if version is greater than the installed and saved in storage`() = runBlockingTest {

        // GIVEN
        val run = buildDownloadUpdateIfAvailable(
            appVersion = BaseVersion(1, "1"),
            lastUpdateVersion = UpdateVersion(2, "2", time),
            installableUpdateVersion = UpdateVersion(1, "1", time)
        )

        // WHEN
        run().collect()

        // THEN
        coVerify(exactly = 1) { repository.storeUpdate(any(), any()) }
    }

    @Test
    fun `update is NOT downloaded if in storage version is greater`() = runBlockingTest {

        // GIVEN
        val run = buildDownloadUpdateIfAvailable(
            appVersion = BaseVersion.Empty,
            lastUpdateVersion = UpdateVersion(2, "2", time),
            installableUpdateVersion = UpdateVersion(3, "3", time)
        )

        // WHEN
        run().collect()

        // THEN
        coVerify(exactly = 0) { repository.storeUpdate(any(), any()) }
    }

    @Test
    fun `update is NOT downloaded if installed version is greater`() = runBlockingTest {

        // GIVEN
        val run = buildDownloadUpdateIfAvailable(
            appVersion = BaseVersion(3, "3"),
            lastUpdateVersion = UpdateVersion(2, "2", time),
            installableUpdateVersion = UpdateVersion.Empty
        )

        // WHEN
        run().collect()

        // THEN
        coVerify(exactly = 0) { repository.storeUpdate(any(), any()) }
    }

    @Test
    fun `throw Exception if download fails`() = runBlockingTest {
        // GIVEN
        val message = "some exception is throws"
        val run = buildDownloadUpdateIfAvailable(
            appVersion = BaseVersion(1, "1"),
            lastUpdateVersion = UpdateVersion(2, "2", time),
            installableUpdateVersion = UpdateVersion(1, "1", time),
            api = spyk(MockUpdatesApi(lastUpdateVersion = UpdateVersion(2, "2", time))) {
                coEvery { getUpdateFile(any()) } answers { throw Exception(message) }
            }
        )

        // WHEN - THEN
        assertFailsWith<Exception>(message) {
            run().collect()
        }
    }

    @Test
    fun `can throw Exception in a Channel`() = runBlockingTest {
        // GIVEN
        val message = "some exception is throws"
        val run = buildDownloadUpdateIfAvailable(
            appVersion = BaseVersion(1, "1"),
            lastUpdateVersion = UpdateVersion(2, "2", time),
            installableUpdateVersion = UpdateVersion(1, "1", time),
            api = spyk(MockUpdatesApi(lastUpdateVersion = UpdateVersion(2, "2", time))) {
                coEvery { getUpdateFile(any()) } answers { throw Exception(message) }
            }
        )

        // WHEN - THEN
        assertFailsWith<Exception>(message) {
            for (nothing in run().produceIn(this)) {

            }
        }
    }
}