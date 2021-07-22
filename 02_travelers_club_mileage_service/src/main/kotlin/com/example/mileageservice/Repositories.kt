package com.example.mileageservice

import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import javax.persistence.LockModeType

interface ReviewRepository : CrudRepository<Review, String> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT r FROM Review r WHERE r.id = :id")
  fun findByIdForUpdate(id: String): Review?

  fun findFirstByPlaceIdOrderByCreatedAtAsc(placeId: String): Review
}

interface UserPointRepository : CrudRepository<UserPoint, String> {

  fun findByUserId(userId: String): UserPoint?

  @Transactional
  @Modifying
  @Query("UPDATE UserPoint SET point = point + :point, modifiedAt = CURRENT_TIMESTAMP WHERE userId = :userId")
  fun incrPoint(@Param("userId") userId: String, @Param("point") point: Int): Int

  @Transactional
  @Modifying
  @Query("UPDATE UserPoint SET point = point - :point, modifiedAt = CURRENT_TIMESTAMP WHERE userId = :userId")
  fun decrPoint(@Param("userId") userId: String, @Param("point") point: Int): Int
}

interface UserPointHistoryRepository : CrudRepository<UserPointHistory, String>
