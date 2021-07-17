# 2. Traveler's Club Mileage Service

## Specifications

### POST /events

- Request Body

  ```json
  {
    "type": "REVIEW",
    "action": "ADD",  // or MOD or DELETE
    "reviewId": "bf23a25e-b04e-4e3b-986c-30cac352cb62",
    "content": "like!",
    "attachedPhotoIds": [
      "a4640fcb-43cc-4cca-86e4-0dfcd093dc7a",
      "428828be-7708-4979-8125-00b92f045130"
    ],
    "userId": "84b5c589-8cc3-478c-9f8f-a801b771fc8a",
    "placeId": "2116154c-6178-4ac8-8e82-9606b7b437b0"
  }
  ```

- user can have upto 1 review per a place
- reviews can be modified or deleted
- user gets reward point for following actions:
  - content with length >= 1: 1 point
  - attaching at least 1 photo: 1 point
  - first ever reviewer of a place: 1 point


## Requirements
- DB schema (MySQL 5.7)
- DDL of DB tables and indices
- server application with following APIs
  - put point
  - get point status
- must have history of point transactions
- calculate total score for each user
- avoid whole table scan with appropriate indices
- point added from writing a review must be reduced when user deletes the review
- point must be re-calculated when user modifies review
