package com.example.mileageservice

class ParamMissingException(message: String) : Exception(message)

class InvalidParamException(message: String) : Exception(message)

class DuplicateReviewException : Exception("Review already exists")

class ReviewNotFoundException : Exception("Review does not exist")

class NotReviewAuthorException : Exception("Only author of the review can modify or delete review")
