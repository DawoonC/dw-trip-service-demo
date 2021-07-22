package com.example.mileageservice

import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional

@Service
class ReviewService(val reviewDb: ReviewRepository) {

  private fun getReviewForUpdate(id: String): Review {
    val review = reviewDb.findByIdForUpdate(id)

    if (review === null) {
      throw ReviewNotFoundException()
    }

    return review
  }

  private fun calculatePointFromReview(review: Review): Int {
    var point = 0

    if (review.content.isNotEmpty()) {
      point += 1
    }
    if (review.photoIds.isNotEmpty()) {
      point += 1
    }

    return point
  }

  fun addReview(params: EventParams): ReviewEventResult {
    val created: Review
    val photoIds = params.attachedPhotoIds?.joinToString(",") ?: ""
    val userId = params.userId ?: ""
    val placeId = params.placeId ?: ""

    try {
      created = reviewDb.save(
        Review(
          userId=userId,
          placeId=placeId,
          content=params.content ?: "",
          photoIds=photoIds,
        ),
      )
    } catch (err: DataIntegrityViolationException) {
      throw DuplicateReviewException()
    }

    var point = calculatePointFromReview(created)
    val firstReview = reviewDb.findFirstByPlaceIdOrderByCreatedAtAsc(placeId)

    if (firstReview.id == created.id) {
      point += 1
      created.hasFirstReviewPoint = true
      reviewDb.save(created)
    }

    return ReviewEventResult(
      reviewId=created.id,
      increasedPoint=point,
    )
  }

  @Transactional
  fun modifyReview(params: EventParams): ReviewEventResult {
    val photoIds = params.attachedPhotoIds?.joinToString(",") ?: ""
    val review = getReviewForUpdate(params.reviewId ?: "")

    if (review.userId != params.userId) {
      throw NotReviewAuthorException()
    }

    val oldPoint = calculatePointFromReview(review)
    review.content = params.content ?: ""
    review.photoIds = photoIds
    reviewDb.save(review)

    val newPoint = calculatePointFromReview(review)
    val diff = newPoint - oldPoint

    return ReviewEventResult(
      reviewId=review.id,
      increasedPoint=(if (diff > 0) diff else 0),
      decreasedPoint=(if (diff < 0) -diff else 0),
    )
  }

  @Transactional
  fun deleteReview(params: EventParams): ReviewEventResult {
    val review = getReviewForUpdate(params.reviewId ?: "")

    if (review.userId != params.userId) {
      throw NotReviewAuthorException()
    }

    var point = calculatePointFromReview(review)
    point += (if (review.hasFirstReviewPoint) 1 else 0)
    reviewDb.delete(review)

    return ReviewEventResult(
      decreasedPoint=point,
    )
  }
}

@Service
class UserPointService(
  val userPointDb: UserPointRepository,
  val userPointHistoryDb: UserPointHistoryRepository,
) {

  fun getOrCreateUserPoint(userId: String): UserPoint? {
    val userPoint = userPointDb.findByUserId(userId)

    if (userPoint !== null) {
      return userPoint
    }

    return try {
      userPointDb.save(UserPoint(userId=userId))
    } catch (err: DataIntegrityViolationException) {
      userPointDb.findByUserId(userId)
    }
  }

  fun updateUserPoint(userId: String, result: ReviewEventResult) {
    if (result.increasedPoint > 0) {
      userPointDb.incrPoint(userId, result.increasedPoint)
      userPointHistoryDb.save(
        UserPointHistory(userId=userId, increasedAmount=result.increasedPoint),
      )
    }

    if (result.decreasedPoint > 0) {
      userPointDb.decrPoint(userId, result.decreasedPoint)
      userPointHistoryDb.save(
        UserPointHistory(userId=userId, decreasedAmount=result.decreasedPoint),
      )
    }
  }
}
