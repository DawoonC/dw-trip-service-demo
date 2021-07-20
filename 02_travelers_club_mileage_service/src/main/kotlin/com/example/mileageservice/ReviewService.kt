package com.example.mileageservice

import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.TransactionCallbackWithoutResult
import org.springframework.transaction.support.TransactionTemplate

@Service
class ReviewService(
  val reviewDb: ReviewRepository,
  val userPointDb: UserPointRepository,
  val userPointHistoryDb: UserPointHistoryRepository,
  val transactionManager: PlatformTransactionManager,
) {

  private val transactionTemplate = TransactionTemplate(transactionManager)

  private fun getReview(id: String): Review {
    val reviewOptional = reviewDb.findById(id)

    if (reviewOptional.isEmpty) {
      throw ReviewNotFoundException()
    }

    return reviewOptional.get()
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

  private fun getOrCreateUserPoint(userId: String): UserPoint? {
    return try {
      userPointDb.save(UserPoint(userId))
    } catch (err: DataIntegrityViolationException) {
      userPointDb.findByUserId(userId)
    }
  }

  fun addReview(params: EventParams): ReviewEventResult {
    var created: Review
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
    getOrCreateUserPoint(userId)

    if (firstReview.id == created.id) {
      point += 1
      created.hasFirstReviewPoint = true
      reviewDb.save(created)
    }

    userPointDb.incrPoint(userId, point)
    userPointHistoryDb.save(
      UserPointHistory(userId=userId, increasedAmount=point),
    )

    return ReviewEventResult(
      reviewId=created.id,
      increasedPoint=point,
    )
  }

  fun modifyReview(params: EventParams): ReviewEventResult {
    val photoIds = params.attachedPhotoIds?.joinToString(",") ?: ""
    var review = getReview(params.reviewId ?: "")

    if (review.userId != params.userId) {
      throw NotReviewAuthorException()
    }

    val oldPoint = transactionTemplate.execute {
      review = reviewDb.findByIdForUpdate(review.id)
      val oldPoint = calculatePointFromReview(review)
      review.content = params.content ?: ""
      review.photoIds = photoIds
      reviewDb.save(review)
      oldPoint
    } ?: 0

    val newPoint = calculatePointFromReview(review)
    val diff = newPoint - oldPoint

    if (diff > 0) {
      userPointDb.incrPoint(params.userId, diff)
      userPointHistoryDb.save(
        UserPointHistory(userId=params.userId, increasedAmount=diff),
      )
    } else if (diff < 0) {
      userPointDb.decrPoint(params.userId, -diff)
      userPointHistoryDb.save(
        UserPointHistory(userId=params.userId, decreasedAmount=-diff),
      )
    }

    return ReviewEventResult(
      reviewId=review.id,
      increasedPoint=(if (diff > 0) diff else 0),
      decreasedPoint=(if (diff < 0) -diff else 0),
    )
  }

  fun deleteReview(params: EventParams): ReviewEventResult {
    var review = getReview(params.reviewId ?: "")

    if (review.userId != params.userId) {
      throw NotReviewAuthorException()
    }

    val point = transactionTemplate.execute {
      review = reviewDb.findByIdForUpdate(review.id)
      var point = calculatePointFromReview(review)
      point += (if (review.hasFirstReviewPoint) 1 else 0)
      reviewDb.delete(review)
      point
    } ?: 0

    userPointDb.decrPoint(params.userId, point)
    userPointHistoryDb.save(
      UserPointHistory(userId=params.userId, decreasedAmount=point),
    )

    return ReviewEventResult(
      decreasedPoint=point,
    )
  }
}
