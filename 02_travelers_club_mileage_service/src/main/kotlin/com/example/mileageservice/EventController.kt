package com.example.mileageservice

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import javax.servlet.http.HttpServletRequest

@RestController
class EventController(val reviewService: ReviewService) {

  private fun handleReviewEvent(params: EventParams): ReviewEventResult {
    params.validateForReview()

    return when (params.action) {
      EventAction.ADD.name -> reviewService.addReview(params)
      EventAction.MOD.name -> reviewService.modifyReview(params)
      EventAction.DELETE.name -> reviewService.deleteReview(params)
      else -> throw InvalidParamException("invalid action")
    }
  }

  @PostMapping("/events")
  @ResponseBody
  fun handleEvent(@RequestBody params: EventParams): ResponseEntity<BaseResponseBody> {
    val result = when (params.type) {
      "REVIEW" -> handleReviewEvent(params)
      else -> throw InvalidParamException("invalid type")
    }

    return ResponseEntity(
      BaseResponseBody(success=true, response=result),
      HttpStatus.OK,
    )
  }

  @ResponseBody
  @ExceptionHandler
  fun convertErrorResponse(request: HttpServletRequest, ex: Throwable): ResponseEntity<BaseResponseBody> {
    val status = when (ex) {
      is NotReviewAuthorException -> HttpStatus.FORBIDDEN
      is InvalidParamException -> HttpStatus.BAD_REQUEST
      is ParamMissingException -> HttpStatus.BAD_REQUEST
      is ReviewNotFoundException -> HttpStatus.NOT_FOUND
      is DuplicateReviewException -> HttpStatus.CONFLICT
      else -> HttpStatus.INTERNAL_SERVER_ERROR
    }

    return ResponseEntity(
      BaseResponseBody(
        success=false,
        error=ErrorResponse(message=ex.message ?: "Internal server error"),
      ),
      status,
    )
  }

  @GetMapping("/")
  fun index() = "<h1>Welcome!</h1>"
}
