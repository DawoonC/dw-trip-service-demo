package com.example.mileageservice

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException

@SpringBootTest
class ReviewServiceTests @Autowired constructor(
  val reviewDb: ReviewRepository,
  val service: ReviewService,
) {

  @AfterEach
  fun cleanUp() {
    reviewDb.deleteAll()
  }

  @Test
  fun `addReview() should throw exception when review already exists for given userId & placeId`() {
    // Given: params with userId & placeId
    // Given: review already exists for userId & placeId
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )
    reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
      ),
    )

    // When: addReview()
    // Then: exception should be thrown
    assertThrows(DuplicateReviewException::class.java) {
      service.addReview(params)
    }
  }

  @Test
  fun `addReview() should create a new review & is the first review of the place`() {
    // Given: params with userId & placeId
    // Given: no first review exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==true
    // Then: result.increasedPoint == 2 (content 1 point + first review of place 1 point)
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1 + 1

    assertTrue(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
  }

  @Test
  fun `addReview() should create a new review & first review already exists for the place`() {
    // Given: params with userId & placeId
    // Given: first review already exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )
    reviewDb.save(
      Review(
        userId="other-user",
        placeId=placeId,
        content="foobar",
        hasFirstReviewPoint=true,
      ),
    )

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==false
    // Then: result.increasedPoint == 1 (content 1 point)
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1

    assertFalse(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
  }

  @Test
  fun `addReview() should create a new review & receive extra point for attached photo`() {
    // Given: params with userId & placeId
    // Given: no first review exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
      attachedPhotoIds=listOf("photo_id_01"),
    )

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==true
    // Then: result.increasedPoint == 2 (content 1 point + attached photo 1 point + first review of place 1 point)
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1 + 1 + 1

    assertTrue(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
  }

  @Test
  fun `modifyReview() should throw exception when review not found in DB`() {
    // Given: params with not existing reviewId
    val userId = "foo"
    val reviewId = "foobar"
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
      content="hello",
    )

    // When: modifyReview()
    // Then: exception should be thrown
    assertThrows(ReviewNotFoundException::class.java) {
      service.modifyReview(params)
    }
  }

  @Test
  fun `modifyReview() should throw exception when trying to modify review created by other user`() {
    // Given: a review created by other user
    // Given: params with reviewId which was created by other user
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId="other-user",
        placeId=placeId,
        content="foobar",
      ),
    )
    val params = EventParams(
      userId=userId,
      reviewId=review.id,
      content="hello",
    )

    // When: modifyReview()
    // Then: exception should be thrown
    assertThrows(NotReviewAuthorException::class.java) {
      service.modifyReview(params)
    }
  }

  @Test
  fun `modifyReview() should create a new review & receive extra point for attached photo`() {
    // Given: review exists with no photoIds
    // Given: params with attachedPhotoIds
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
      ),
    )
    val params = EventParams(
      userId=userId,
      reviewId=review.id,
      content="hello",
      attachedPhotoIds=listOf("photo_id_01"),
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.increasedPoint == 1 (attached photo 1 point)
    val expectedIncreasedPoint = 1
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
  }

  @Test
  fun `modifyReview() should create a new review & decrease point for removing photo`() {
    // Given: review exists with photoIds
    // Given: params with no attachedPhotoIds
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
        photoIds="photo_id_01",
      ),
    )
    val params = EventParams(
      userId=userId,
      reviewId=review.id,
      content="hello",
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.decreasedPoint == 1 (removed photo -1 point)
    val expectedDecreasedPoint = 1
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
  }

  @Test
  fun `modifyReview() should create a new review & no point change`() {
    // Given: review exists with no photoIds
    // Given: params with no attachedPhotoIds
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
      ),
    )
    val params = EventParams(
      userId=userId,
      reviewId=review.id,
      content="hello",
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.increasedPoint == 0
    // Then: result.decreasedPoint == 0
    val expectedIncreasedPoint = 0
    val expectedDecreasedPoint = 0
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
  }

  @Test
  fun `deleteReview() should throw exception when review not found in DB`() {
    // Given: params with not existing reviewId
    val userId = "foo"
    val reviewId = "foobar"
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
    )

    // When: deleteReview()
    // Then: exception should be thrown
    assertThrows(ReviewNotFoundException::class.java) {
      service.deleteReview(params)
    }
  }

  @Test
  fun `deleteReview() should throw exception when trying to modify review created by other user`() {
    // Given: a review created by other user
    // Given: params with reviewId which was created by other user
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId="other-user",
        placeId=placeId,
        content="foobar",
      ),
    )
    val params = EventParams(
      userId=userId,
      reviewId=review.id,
    )

    // When: deleteReview()
    // Then: exception should be thrown
    assertThrows(NotReviewAuthorException::class.java) {
      service.deleteReview(params)
    }
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease extra point from first review`() {
    // Given: review exists with non-empty photoIds & hasFirstReviewPoint==true
    // Given: params with userId & reviewId
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
        photoIds="photo_id_01",
        hasFirstReviewPoint=true,
      ),
    )
    val reviewId = review.id
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 3 (content 1 point, attached photo 1 point, first review for the place 1 point)
    val expectedDecreasedPoint = 3
    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease extra point from attached photo`() {
    // Given: review exists with non-empty photoIds
    // Given: params with userId & reviewId
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
        photoIds="photo_id_01",
      ),
    )
    val reviewId = review.id
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 2 (content 1 point, attached photo 1 point)
    val expectedDecreasedPoint = 2
    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease point from content`() {
    // Given: review exists with no photoIds
    // Given: params with userId & reviewId
    val userId = "foo"
    val placeId = "bar"
    val review = reviewDb.save(
      Review(
        userId=userId,
        placeId=placeId,
        content="foobar",
      ),
    )
    val reviewId = review.id
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 1 (content 1 point)
    val expectedDecreasedPoint = 1
    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
  }
}

@SpringBootTest
class UserPointServiceTests @Autowired constructor(
  val userPointDb: UserPointRepository,
  val userPointHistoryDb: UserPointHistoryRepository,
  val service: UserPointService,
) {

  @AfterEach
  fun cleanUp() {
    userPointDb.deleteAll()
    userPointHistoryDb.deleteAll()
  }

  @Test
  fun `getOrCreateUserPoint() should create a new row when UserPoint for given userId does not exist`() {
    // Given: userId
    // Given: UserPoint for given userId does not exist
    val userId = "foo"

    // When: getOrCreateUserPoint()
    val result = service.getOrCreateUserPoint(userId)

    // Then: a new row should be created & returned as a result
    assertNotNull(result)
  }

  @Test
  fun `getOrCreateUserPoint() should return existing row when UserPoint for given userId already exists`() {
    // Given: userId
    // Given: UserPoint for given userId already exists
    val userId = "foo"
    val userPoint = userPointDb.save(UserPoint(userId=userId))

    // When: getOrCreateUserPoint()
    val result = service.getOrCreateUserPoint(userId)

    // Then: returned row ID should be equal to existing row ID
    assertEquals(result?.id, userPoint.id)
  }

  @Test
  fun `getOrCreateUserPoint() should catch exception when DataIntegrityViolationException occurs during insert`() {
    // Given: userId
    // Given: UserPoint does not exist when queried from DB
    // Given: exception is thrown when insert
    val userId = "foo"
    val mockDb: UserPointRepository = mock()
    val mockService = UserPointService(mockDb, userPointHistoryDb)
    whenever(mockDb.findByUserId(userId))
      .thenReturn(null)
    whenever(mockDb.save(isA<UserPoint>()))
      .thenThrow(DataIntegrityViolationException(""))

    // When: getOrCreateUserPoint()
    // Then: exception should be caught
    // Then: result should be null (due to mock)
    assertDoesNotThrow {
      val result = mockService.getOrCreateUserPoint(userId)
      assertNull(result)
    }
  }

  @Test
  fun `updateUserPoint() should update existing UserPoint when there is increased point`() {
    // Given: UserPoint with 10 point
    // Given: ReviewEventResult with increasedPoint==5
    val userId = "foo"
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )
    val eventResult = ReviewEventResult(increasedPoint=5)

    // When: updateUserPoint()
    service.updateUserPoint(userId, eventResult)

    // Then: UserPoint should be updated to expected point
    // Then: UserPointHistory should be created for given event
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val history = userPointHistoryDb.findAll().firstOrNull()
    val expectedTotalPoint = 15
    val expectedIncreasedPoint = 5
    val expectedDecreasedPoint = 0

    assertEquals(updatedUserPoint.point, expectedTotalPoint)
    assertNotEquals(updatedUserPoint.modifiedAt, userPoint.modifiedAt)
    assertNotNull(history)
    assertEquals(history?.userId, userId)
    assertEquals(history?.increasedAmount, expectedIncreasedPoint)
    assertEquals(history?.decreasedAmount, expectedDecreasedPoint)
  }

  @Test
  fun `updateUserPoint() should update existing UserPoint when there is decreased point`() {
    // Given: UserPoint with 10 point
    // Given: ReviewEventResult with decreasedPoint==5
    val userId = "foo"
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )
    val eventResult = ReviewEventResult(decreasedPoint=5)

    // When: updateUserPoint()
    service.updateUserPoint(userId, eventResult)

    // Then: UserPoint should be updated to expected point
    // Then: UserPointHistory should be created for given event
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val history = userPointHistoryDb.findAll().firstOrNull()
    val expectedTotalPoint = 5
    val expectedIncreasedPoint = 0
    val expectedDecreasedPoint = 5

    assertEquals(updatedUserPoint.point, expectedTotalPoint)
    assertNotEquals(updatedUserPoint.modifiedAt, userPoint.modifiedAt)
    assertNotNull(history)
    assertEquals(history?.userId, userId)
    assertEquals(history?.increasedAmount, expectedIncreasedPoint)
    assertEquals(history?.decreasedAmount, expectedDecreasedPoint)
  }

  @Test
  fun `updateUserPoint() should NOT update existing UserPoint when there is point change`() {
    // Given: UserPoint with 10 point
    // Given: ReviewEventResult with no point change
    val userId = "foo"
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )
    val eventResult = ReviewEventResult()

    // When: updateUserPoint()
    service.updateUserPoint(userId, eventResult)

    // Then: UserPoint should NOT be updated to expected point
    // Then: UserPointHistory should NOT be created for given event
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val history = userPointHistoryDb.findAll().firstOrNull()
    val expectedTotalPoint = 10

    assertEquals(updatedUserPoint.point, expectedTotalPoint)
    assertEquals(updatedUserPoint.modifiedAt, userPoint.modifiedAt)
    assertNull(history)
  }
}
