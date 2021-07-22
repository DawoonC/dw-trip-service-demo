package com.example.mileageservice

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@SpringBootTest
@AutoConfigureMockMvc
class EventControllerTests @Autowired constructor(
  val mockMvc: MockMvc,
  val objectMapper: ObjectMapper,
) {

  private fun postEvent(params: EventParams): ResultActions {
    val body = objectMapper.writeValueAsString(params)

    return mockMvc.perform(
      post("/events")
      .content(body)
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
    )
  }

  @Test
  fun `POST events with no "type" field`() {
    // Given: EventParams with type==null
    val params = EventParams()

    // When: POST /events
    // Then: status code == 400
    // Then: error message should be equal to expected
    val expectedStatus = status().isBadRequest
    val expectedMessage = "invalid type"

    postEvent(params)
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.error.message").value(expectedMessage))
  }

  @Test
  fun `POST events with no "action" field`() {
    // Given: EventParams with action==null
    val params = EventParams(type="REVIEW")

    // When: POST /events
    // Then: status code == 400
    // Then: error message should be equal to expected
    val expectedStatus = status().isBadRequest
    val expectedMessage = "action must be provided"

    postEvent(params)
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.error.message").value(expectedMessage))
  }

  @Test
  fun `POST events with no "userId" field`() {
    // Given: EventParams with userId==null
    val params = EventParams(
      type="REVIEW",
      action="ADD",
    )

    // When: POST /events
    // Then: status code == 400
    // Then: error message should be equal to expected
    val expectedStatus = status().isBadRequest
    val expectedMessage = "userId must be provided"

    postEvent(params)
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.error.message").value(expectedMessage))
  }

  @Test
  fun `POST events with invalid "action"`() {
    // Given: EventParams with action==null
    val params = EventParams(
      type="REVIEW",
      action="UNKNOWN",
      userId="foo",
    )

    // When: POST /events
    // Then: status code == 400
    // Then: error message should be equal to expected
    val expectedStatus = status().isBadRequest
    val expectedMessage = "invalid action"

    postEvent(params)
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.error.message").value(expectedMessage))
  }
}

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTests @Autowired constructor(
  val mockMvc: MockMvc,
  val userPointDb: UserPointRepository,
) {

  @AfterEach
  fun cleanUp() {
    userPointDb.deleteAll()
  }

  @Test
  fun `GET user point when UserPoint does not exist in DB`() {
    // Given: UserPoint for given userId does not exist in DB
    val userId = "foo"

    // When: GET /users/{userId}/point
    // Then: status code == 200
    // Then: returned point should be equal to expected
    val expectedStatus = status().isOk
    val expectedPoint = 0

    mockMvc.perform(
      get("/users/${userId}/point")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.response.point").value(expectedPoint))
  }

  @Test
  fun `GET user point when UserPoint exists in DB`() {
    // Given: UserPoint for given userId with point==10
    val userId = "foo"
    userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: GET /users/{userId}/point
    // Then: status code == 200
    // Then: returned point should be equal to expected
    val expectedStatus = status().isOk
    val expectedPoint = 10

    mockMvc.perform(
      get("/users/${userId}/point")
      .contentType(MediaType.APPLICATION_JSON)
      .accept(MediaType.APPLICATION_JSON)
    )
      .andExpect(expectedStatus)
      .andExpect(jsonPath("$.response.point").value(expectedPoint))
  }
}
