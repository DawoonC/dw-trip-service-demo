package com.example.mileageservice

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class EventParamsTests {

  @Test
  fun `validateForReview() should throw exception when "action" property has null value`() {
    // Given: EventParams with aciton==null
    val params = EventParams(action=null)

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "userId" property has null value`() {
    // Given: EventParams with userId==null
    val params = EventParams(
      action="FOO",
      userId=null,
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "ADD" and "placeId" property has null value`() {
    // Given: EventParams with action=="ADD" & placeId==null
    val params = EventParams(
      action="ADD",
      userId="foo",
      placeId=null,
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "ADD" and "content" property has null value`() {
    // Given: EventParams with action=="ADD" & content==null
    val params = EventParams(
      action="ADD",
      userId="foo",
      placeId="bar",
      content=null,
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "ADD" and "content" property is empty`() {
    // Given: EventParams with action=="ADD" & content is empty
    val params = EventParams(
      action="ADD",
      userId="foo",
      placeId="bar",
      content="",
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(InvalidParamException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validate() should NOT throw exception when "action" == "ADD" and required properties are valid`() {
    // Given: EventParams with action=="ADD" & valid property values
    val params = EventParams(
      action="ADD",
      userId="foo",
      placeId="bar",
      content="hello",
    )

    // When: validate()
    // Then: exception should NOT be thrown
    assertDoesNotThrow {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "MOD" and "reviewId" property has null value`() {
    // Given: EventParams with action=="MOD" & reviewId==null
    val params = EventParams(
      action="MOD",
      userId="foo",
      reviewId=null,
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "MOD" and "content" property is empty`() {
    // Given: EventParams with action=="MOD" & content is empty
    val params = EventParams(
      action="MOD",
      userId="foo",
      reviewId="bar",
      content="",
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(InvalidParamException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validate() should NOT throw exception when "action" == "MOD" and required properties are valid`() {
    // Given: EventParams with action=="MOD" & valid property values
    val params = EventParams(
      action="MOD",
      userId="foo",
      reviewId="bar",
      content="hello",
    )

    // When: validate()
    // Then: exception should NOT be thrown
    assertDoesNotThrow {
      params.validateForReview()
    }
  }

  @Test
  fun `validateForReview() should throw exception when "action" == "DELETE" and "reviewId" property has null value`() {
    // Given: EventParams with action=="DELETE" & reviewId==null
    val params = EventParams(
      action="DELETE",
      userId="foo",
      reviewId=null,
    )

    // When: validateForReview()
    // Then: exception should be thrown
    assertThrows(ParamMissingException::class.java) {
      params.validateForReview()
    }
  }

  @Test
  fun `validate() should NOT throw exception when "action" == "DELETE" and required properties are valid`() {
    // Given: EventParams with action=="DELETE" & valid property values
    val params = EventParams(
      action="DELETE",
      userId="foo",
      reviewId="bar",
    )

    // When: validate()
    // Then: exception should NOT be thrown
    assertDoesNotThrow {
      params.validateForReview()
    }
  }
}
