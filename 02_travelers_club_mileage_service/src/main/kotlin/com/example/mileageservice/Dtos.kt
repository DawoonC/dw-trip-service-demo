package com.example.mileageservice

enum class EventAction {
  ADD,
  MOD,
  DELETE,
}

data class EventParams(
  val type: String? = null,
  val action: String? = null,
  val reviewId: String? = null,
  val userId: String? = null,
  val placeId: String? = null,
  val content: String? = null,
  val attachedPhotoIds: List<String>? = emptyList(),
) {
  fun validateForReview() {
    if (action === null) throw ParamMissingException("action must be provided")
    if (userId === null) throw ParamMissingException("userId must be provided")

    when (action) {
      EventAction.ADD.name -> {
        if (placeId === null) throw ParamMissingException("placeId must be provided")
        if (content === null) throw ParamMissingException("content must be provided")
        if (content == "") throw InvalidParamException("content must not be empty")
      }
      EventAction.MOD.name -> {
        if (reviewId === null) throw ParamMissingException("reviewId must be provided")
        if (content == "") throw InvalidParamException("content must not be empty")
      }
      EventAction.DELETE.name -> {
        if (reviewId === null) throw ParamMissingException("reviewId must be provided")
      }
    }
  }
}

data class ReviewEventResult(
  val reviewId: String? = null,
  val increasedPoint: Int = 0,
  val decreasedPoint: Int = 0,
)

data class ErrorResponse(
  val message: String,
)

data class BaseResponseBody(
  val success: Boolean,
  val response: Any? = null,
  val error: ErrorResponse? = null,
)
