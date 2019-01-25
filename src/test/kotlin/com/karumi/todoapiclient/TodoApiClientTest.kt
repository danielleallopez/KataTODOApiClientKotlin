package com.karumi.todoapiclient

import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import todoapiclient.Either
import todoapiclient.TodoApiClient
import todoapiclient.dto.TaskDto
import todoapiclient.exception.ItemNotFoundError
import todoapiclient.exception.NetworkError
import todoapiclient.exception.UnknownApiError
import java.io.File
import java.io.FileReader
import java.lang.reflect.Type


class TodoApiClientTest : MockWebServerTest() {

    private lateinit var apiClient: TodoApiClient

    private val gson by lazy { Gson() }

    @Before
    override fun setUp() {
        super.setUp()
        val mockWebServerEndpoint = baseEndpoint
        apiClient = TodoApiClient(mockWebServerEndpoint)
    }

    @Test
    fun sendsAcceptAndContentTypeHeaders() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertRequestContainsHeader("Accept", "application/json")
    }

    @Test
    fun sendsGetAllTaskRequestToTheCorrectEndpoint() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertGetRequestSentTo("/todos")
    }

    @Test
    fun parsesTasksProperlyGettingAllTheTasks() {
        enqueueMockResponse(200, "getTasksResponse.json")

        val tasks = apiClient.allTasks.right!!

        assertEquals(200, tasks.size.toLong())
        assertTaskContainsExpectedValues(tasks[0])
    }

    @Test
    fun receivesNoTasksWhenGettingNoTasks() {
        enqueueMockResponse(200, "getTasksResponse_empty.json")

        val tasks = apiClient.allTasks.right!!

        assertEquals(0, tasks.size.toLong())
    }

    @Test
    fun returnsUnknownApiErrorWhenCodeIs403() {
        enqueueMockResponse(403)

        val error = apiClient.allTasks.left!!

        assertEquals(UnknownApiError(403), error)
    }

    @Test
    fun returnsUnknownApiErrorWhenCodeIs500() {
        enqueueMockResponse(500)

        val error = apiClient.allTasks.left!!

        assertEquals(UnknownApiError(500), error)
    }

    @Test
    fun sendsProperHeaderWhenFetchingTaskList() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertRequestContainsHeader("Content-type", "application/json")
    }

    @Test
    fun usesMethodGetWhenFetchingTaskList() {
        enqueueMockResponse(200, "getTasksResponse.json")

        apiClient.allTasks

        assertGetRequestSentTo("/todos")
    }

    //Fetching task by Id
    @Test
    fun returnsProperTaskWhenFetchingById() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        val taskById = apiClient.getTaskById(TASK_ID).right

        assertTaskContainsExpectedValues(taskById)
    }

    @Test
    fun returnsErrorWhenFetchingMalformedTaskById() {
        enqueueMockResponse(200, "getTaskByIdResponse_malformed.json")

        val response = apiClient.getTaskById(TASK_ID)

        assertEquals("Cannot parse response", Either.Left(NetworkError), response)
    }

    @Test
    fun returnsUnknownApiErrorWithCodeIs404WhenTaskIdNotFound() {
        enqueueMockResponse(404)

        val error = apiClient.getTaskById(TASK_ID).left!!

        assertEquals(ItemNotFoundError, error)
    }

    @Test
    fun returnsUnknownApiErrorWithCodeIs500WhenFetchingTaskById() {
        enqueueMockResponse(500)

        val error = apiClient.getTaskById(TASK_ID).left!!

        assertEquals(UnknownApiError(500), error)
    }

    @Test
    fun sendsProperAcceptHeaderWhenFetchingTaskById() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        apiClient.getTaskById(TASK_ID)

        assertRequestContainsHeader("Accept", "application/json")
    }

    @Test
    fun sendsProperContentTypeHeaderWhenFetchingTaskById() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        apiClient.getTaskById(TASK_ID)

        assertRequestContainsHeader("Content-type", "application/json")
    }

    @Test
    fun usesMethodGetWhenFetchingTaskById() {
        enqueueMockResponse(200, "getTaskByIdResponse.json")

        apiClient.getTaskById(TASK_ID)

        assertGetRequestSentTo("/todos/$TASK_ID")
    }

    //Task creation
    @Test
    fun returnsUpdatedTaskWithCode200WhenSendingValidTaskData() {
        enqueueMockResponse(200, "addTaskResponse.json")

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest.json", TaskDto::class.java)
        val response = apiClient.addTask(taskToCreate)

        val updatedTask = readJsonFile<TaskDto>("addTaskResponse.json", TaskDto::class.java)
        assertEquals("Updated task when proper data sent", Either.Right(updatedTask), response)
    }

    @Test
    fun returnsErrorWhenSendingInvalidTaskData() {
        enqueueMockResponse(400)

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest_malformed.json", TaskDto::class.java)
        val error = apiClient.addTask(taskToCreate)

        assertEquals(Either.Left(UnknownApiError(400)), error)
    }

    @Test
    fun returnsErrorWhenSendingEmptyTaskData() {
        enqueueMockResponse(400)

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest_empty.json", TaskDto::class.java)
        val error = apiClient.addTask(taskToCreate)

        assertEquals(Either.Left(UnknownApiError(400)), error)
    }

    @Test
    fun sendsProperAcceptHeaderWhenCreatingTask() {
        enqueueMockResponse(200, "addTaskResponse.json")

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest.json", TaskDto::class.java)
        apiClient.addTask(taskToCreate)

        assertRequestContainsHeader("Accept", "application/json")
    }

    @Test
    fun sendsProperContentTypeHeaderWhenCreatingTask() {
        enqueueMockResponse(200, "addTaskResponse.json")

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest.json", TaskDto::class.java)
        apiClient.addTask(taskToCreate)

        assertRequestContainsHeader("Content-type", "application/json; charset=UTF-8")
    }

    @Test
    fun usesMethodPostWhenCreatingTask() {
        enqueueMockResponse(200, "addTaskResponse.json")

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest.json", TaskDto::class.java)
        apiClient.addTask(taskToCreate)

        assertPostRequestSentTo("/todos")
    }

    @Test
    fun sendsProperBodyWhenCreatingTaskWithValidData() {
        enqueueMockResponse(200, "addTaskResponse.json")

        val taskToCreate = readJsonFile<TaskDto>("addTaskRequest.json", TaskDto::class.java)
        apiClient.addTask(taskToCreate)

        assertRequestBodyEquals("addTaskRequest.json")
    }

    private fun assertTaskContainsExpectedValues(task: TaskDto?) {
        assertTrue(task != null)
        assertEquals(task?.id, "1")
        assertEquals(task?.userId, "1")
        assertEquals(task?.title, "delectus aut autem")
        assertFalse(task!!.isFinished)
    }

    private inline fun <reified T> readJsonFile(fileName: String, type: Type): T {
        val file = File(javaClass.getResource("/$fileName").file)
        val reader = JsonReader(FileReader(file))
        return gson.fromJson(reader, type)
    }
}

private const val TASK_ID = "1"
