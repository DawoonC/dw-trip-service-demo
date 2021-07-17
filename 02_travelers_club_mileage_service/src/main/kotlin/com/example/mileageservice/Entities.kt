package com.example.mileageservice

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import javax.persistence.*

@Entity
@Table(
  name="reviews",
  indexes=[
    Index(name="idx_user_id_place_id", columnList="user_id, place_id", unique=true),
    Index(name="idx_place_id_created_at", columnList="place_id, created_at"),
  ],
)
class Review(
  @Column(name="user_id", length=255, nullable=false) var userId: String,
  @Column(name="place_id", length=255, nullable=false) var placeId: String,
  @Lob @Column(name="content", nullable=false) var content: String = "",
  @Lob @Column(name="photo_ids", nullable=false) var photoIds: String = "",
  @CreatedDate @Column(name="created_at", nullable=false) var createdAt: LocalDateTime = LocalDateTime.now(),
  @LastModifiedDate @Column(name="modified_at", nullable=false) var modifiedAt: LocalDateTime = LocalDateTime.now(),
  @Column(name="is_deleted", length=1, nullable=false) var isActive: Boolean = false,
  @Id @GeneratedValue(strategy=GenerationType.AUTO) var id: String? = null,
)

@Entity
@Table(
  name="user_points",
  indexes=[Index(name="idx_user_id", columnList="user_id", unique=true)],
)
class UserPoint(
  @Column(name="user_id", length=255, nullable=false) var userId: String,
  @Column(name="point", nullable=false) var point: Int = 0,
  @CreatedDate @Column(name="created_at", nullable=false) var createdAt: LocalDateTime = LocalDateTime.now(),
  @LastModifiedDate @Column(name="modified_at", nullable=false) var modifiedAt: LocalDateTime = LocalDateTime.now(),
  @Id @GeneratedValue(strategy=GenerationType.AUTO) var id: String? = null,
)

@Entity
@Table(
  name="user_point_histories",
  indexes=[Index(name = "idx_user_id_created_at", columnList = "user_id, created_at")],
)
class UserPointHistory(
  @Column(name="user_id", length=255, nullable=false) var userId: String,
  @CreatedDate @Column(name="created_at", nullable=false) var createdAt: LocalDateTime = LocalDateTime.MIN,
  @Column(name="increased_amount", nullable=false) var increasedAmount: Int = 0,
  @Column(name="decreased_amount", nullable=false) var decreasedAmount: Int = 0,
  @Id @GeneratedValue(strategy=GenerationType.AUTO) var id: String? = null,
)
