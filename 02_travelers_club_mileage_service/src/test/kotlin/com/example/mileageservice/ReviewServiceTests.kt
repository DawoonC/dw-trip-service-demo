package com.example.mileageservice

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ReviewServiceTests @Autowired constructor(
  val reviewDb: ReviewRepository,
  val userPointDb: UserPointRepository,
  val userPointHistoryDb: UserPointHistoryRepository,
  val service: ReviewService,
) {

  @AfterEach
  fun cleanUp() {
    reviewDb.deleteAll()
    userPointDb.deleteAll()
    userPointHistoryDb.deleteAll()
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
  fun `addReview() should create a new review & update existing UserPoint if UserPoint already exists`() {
    // Given: params with userId & placeId
    // Given: UserPoint already exists for userId with point==10
    // Given: no first review exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==true
    // Then: result.increasedPoint == 2 (content 1 point + first review of place 1 point)
    // Then: UserPoint.point should be updated to 12
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1 + 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 + expectedIncreasedPoint

    assertTrue(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `addReview() should create a new review & create new UserPoint if UserPoint does not exist`() {
    // Given: params with userId & placeId
    // Given: UserPoint does not exist for userId
    // Given: no first review exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )
    assertNull(userPointDb.findByUserId(userId))

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==true
    // Then: result.increasedPoint == 2 (content 1 point + first review of place 1 point)
    // Then: new UserPoint should be created & has point==2
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1 + 1
    val createdUserPoint = userPointDb.findByUserId(userId)
    val expectedTotalPoint = expectedIncreasedPoint

    assertTrue(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(createdUserPoint?.point, expectedTotalPoint)
  }

  @Test
  fun `addReview() should create a new review & first review already exists for the place`() {
    // Given: params with userId & placeId
    // Given: UserPoint already exists for userId with point==10
    // Given: first review already exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
    )
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
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
    // Then: UserPoint.point should be updated to 11
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 + expectedIncreasedPoint

    assertFalse(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `addReview() should create a new review & receive extra point for attached photo`() {
    // Given: params with userId & placeId
    // Given: UserPoint already exists for userId with point==10
    // Given: no first review exists for the place
    val userId = "foo"
    val placeId = "bar"
    val params = EventParams(
      userId=userId,
      placeId=placeId,
      content="hello",
      attachedPhotoIds=listOf("photo_id_01"),
    )
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: addReview()
    val result = service.addReview(params)

    // Then: new review should be created & hasFirstReviewPoint==true
    // Then: result.increasedPoint == 2 (content 1 point + attached photo 1 point + first review of place 1 point)
    // Then: UserPoint.point should be updated to 13
    val createdReview = reviewDb.findById(result.reviewId ?: "").get()
    val expectedIncreasedPoint = 1 + 1 + 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 + expectedIncreasedPoint

    assertTrue(createdReview.hasFirstReviewPoint)
    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `modifyReview() should throw exception when review not found in DB`() {
    // Given: params with not existing reviewId
    // Given: UserPoint already exists for userId with point==10
    val userId = "foo"
    val placeId = "bar"
    val reviewId = "foobar"
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
      content="hello",
    )
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
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
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
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
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.increasedPoint == 1 (attached photo 1 point)
    // Then: UserPoint.point should be updated to 11
    val expectedIncreasedPoint = 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 + expectedIncreasedPoint

    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `modifyReview() should create a new review & decrease existing point for removing photo`() {
    // Given: review exists with photoIds
    // Given: params with no attachedPhotoIds
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.decreasedPoint == 1 (removed photo -1 point)
    // Then: UserPoint.point should be updated to 9
    val expectedDecreasedPoint = 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 - expectedDecreasedPoint

    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `modifyReview() should create a new review & no point change when no extra point`() {
    // Given: review exists with no photoIds
    // Given: params with no attachedPhotoIds
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: modifyReview()
    val result = service.modifyReview(params)

    // Then: result.increasedPoint == 0
    // Then: result.decreasedPoint == 0
    // Then: UserPoint.point should be updated to 10
    val expectedIncreasedPoint = 0
    val expectedDecreasedPoint = 0
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10

    assertEquals(result.increasedPoint, expectedIncreasedPoint)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `deleteReview() should throw exception when review not found in DB`() {
    // Given: params with not existing reviewId
    // Given: UserPoint already exists for userId with point==10
    val userId = "foo"
    val placeId = "bar"
    val reviewId = "foobar"
    val params = EventParams(
      userId=userId,
      reviewId=reviewId,
    )
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
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
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: deleteReview()
    // Then: exception should be thrown
    assertThrows(NotReviewAuthorException::class.java) {
      service.deleteReview(params)
    }
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease UserPoint by 3`() {
    // Given: review exists with non-empty photoIds & hasFirstReviewPoint==true
    // Given: params with userId & reviewId
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 3 (content 1 point, attached photo 1 point, first review for the place 1 point)
    // Then: UserPoint.point should be updated to 7
    val expectedDecreasedPoint = 3
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 - expectedDecreasedPoint

    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease UserPoint by 2`() {
    // Given: review exists with non-empty photoIds
    // Given: params with userId & reviewId
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 2 (content 1 point, attached photo 1 point)
    // Then: UserPoint.point should be updated to 8
    val expectedDecreasedPoint = 2
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 - expectedDecreasedPoint

    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }

  @Test
  fun `deleteReview() should delete review in DB & decrease UserPoint by 1`() {
    // Given: review exists with no photoIds
    // Given: params with userId & reviewId
    // Given: UserPoint already exists for userId with point==10
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
    val userPoint = userPointDb.save(
      UserPoint(
        userId=userId,
        point=10,
      ),
    )

    // When: deleteReview()
    val result = service.deleteReview(params)

    // Then: review should be deleted
    // Then: result.decreasedPoint == 1 (content 1 point)
    // Then: UserPoint.point should be updated to 9
    val expectedDecreasedPoint = 1
    val updatedUserPoint = userPointDb.findById(userPoint.id).get()
    val expectedTotalPoint = 10 - expectedDecreasedPoint

    assertTrue(reviewDb.findById(reviewId).isEmpty)
    assertEquals(result.decreasedPoint, expectedDecreasedPoint)
    assertEquals(updatedUserPoint.point, expectedTotalPoint)
  }
}
