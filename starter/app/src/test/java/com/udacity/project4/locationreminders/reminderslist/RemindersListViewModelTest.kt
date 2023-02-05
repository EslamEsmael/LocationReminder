package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.DummyReminderData
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert
import org.hamcrest.Matchers
import org.junit.After
import org.hamcrest.Matchers.`is`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @ExperimentalCoroutinesApi
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var viewModel: RemindersListViewModel

    @Before
    fun setup() {

        stopKoin()

        fakeDataSource = FakeDataSource()

        viewModel = RemindersListViewModel(ApplicationProvider.getApplicationContext(), fakeDataSource)
    }

    @After
    fun clearData() = runBlockingTest{

        fakeDataSource.deleteAll()
    }

    @Test
    fun loadReminders_loading() = runBlockingTest{
        // WHEN load reminders
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()

        // THEN the progress indicator is shown
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), Matchers.`is`(true))

        // Execute stop indicator
        mainCoroutineRule.resumeDispatcher()

        // THEN: the progress indicator is hidden
        MatcherAssert.assertThat(viewModel.showLoading.getOrAwaitValue(), Matchers.`is`(false))
    }

    @Test
    fun loadReminders_success() = runBlockingTest{
        // GIVEN insert a reminders
        DummyReminderData.reminders.forEach {
            fakeDataSource.saveReminder(it)
        }

        // WHEN Load reminders
        viewModel.loadReminders()


        // THEN The loaded data contains the expected values
        val result = viewModel.remindersList.getOrAwaitValue()
        MatcherAssert.assertThat(result.size, `is`(DummyReminderData.reminders.size))
        result.indices.forEach {
            MatcherAssert.assertThat(result[it].title, `is`(DummyReminderData.reminders[it].title))
        }
        MatcherAssert.assertThat(viewModel.showNoData.getOrAwaitValue(), Matchers.`is`(false))
    }

    @Test
    fun loadReminders_failure() = runBlockingTest{
        // GIVEN Return an error
        fakeDataSource.setShouldReturnError(true)

        // WHEN Load reminders
        viewModel.loadReminders()

        // THEN The error message is shown
        MatcherAssert.assertThat(viewModel.showSnackBar.getOrAwaitValue(), Matchers.`is`("Reminders not found!"))
        MatcherAssert.assertThat(viewModel.showNoData.getOrAwaitValue(), Matchers.`is`(true))
    }

    @Test
    fun loadReminders_empty() = runBlockingTest{
        // GIVEN Empty list of Reminders
        fakeDataSource.deleteAll()

        // WHEN Load reminders
        viewModel.loadReminders()

        // THEN Size of Reminders is zero
        val result = viewModel.remindersList.getOrAwaitValue()
        MatcherAssert.assertThat(result.size, Matchers.`is`(0))
        MatcherAssert.assertThat(viewModel.showNoData.getOrAwaitValue(), Matchers.`is`(true))
    }
}