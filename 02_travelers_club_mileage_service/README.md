# 2. Traveler's Club Mileage Service

## Requirements
- Java >= 11
- Docker


## Run Application
```bash
# build
$ ./gradlew build

# start local server with docker
$ docker compose up
```
- 서버: http://localhost:5000
- DB 관리 페이지: http://localhost:5001
  - username: root
  - password: root


## API Reference

### POST /events
리뷰 이벤트 처리 API

#### Request Body

|name|type|required?|description|
|-|-|:-:|-|
|`type`|string|O|현재 "REVIEW" 값만 지원|
|`action`|string|O|- "ADD": 새로운 리뷰 추가<br>- "MOD": 리뷰 수정<br>- "DELETE": 리뷰 삭제|
|`userId`|string|O|사용자 ID|
|`placeId`|string|X|장소 ID<br>- action=ADD 인 경우 필수<br>- 그 외 action 인 경우 사용되지 않음|
|`content`|string|X|리뷰의 내용. 1자 이상 작성 필요<br>- action=ADD 인 경우 필수|
|`attachedPhotoIds`|string[]|X|리뷰에 첨부된 사진 ID 목록|
|`reviewId`|string|X|리뷰 ID<br>- action=MOD 인 경우 필수<br>- action=DELETE 인 경우 필수<br>- action=ADD 인 경우 사용되지 않음|

#### Examples
```bash
# 새로운 리뷰 추가
$ curl 'http://localhost:5000/events' -i -X POST \
    -H 'Content-Type: application/json;charset=UTF-8' \
    -H 'Accept: application/json' \
    -d '{
  "type": "REVIEW",
  "action": "ADD",
  "userId": "84b5c589-8cc3-478c-9f8f-a801b771fc8a",
  "placeId": "2116154c-6178-4ac8-8e82-9606b7b437b0",
  "content": "like!",
  "attachedPhotoIds": [
    "a4640fcb-43cc-4cca-86e4-0dfcd093dc7a",
    "428828be-7708-4979-8125-00b92f045130"
  ],
  "reviewId": "bf23a25e-b04e-4e3b-986c-30cac352cb62"
}'
```

```bash
# 리뷰 수정
$ curl 'http://localhost:5000/events' -i -X POST \
    -H 'Content-Type: application/json;charset=UTF-8' \
    -H 'Accept: application/json' \
    -d '{
  "type": "REVIEW",
  "action": "MOD",
  "userId": "84b5c589-8cc3-478c-9f8f-a801b771fc8a",
  "content": "like!!!",
  "attachedPhotoIds": [
    "428828be-7708-4979-8125-00b92f045130"
  ],
  "reviewId": "bf23a25e-b04e-4e3b-986c-30cac352cb62"
}'
```

```bash
# 리뷰 삭제
$ curl 'http://localhost:5000/events' -i -X POST \
    -H 'Content-Type: application/json;charset=UTF-8' \
    -H 'Accept: application/json' \
    -d '{
  "type": "REVIEW",
  "action": "DELETE",
  "userId": "84b5c589-8cc3-478c-9f8f-a801b771fc8a",
  "reviewId": "bf23a25e-b04e-4e3b-986c-30cac352cb62"
}'
```

#### Response Body

|name|type|description|
|-|-|-|
|`success`|boolean|요청의 성공 여부|
|`response`|object|응답 상세 내용. 실패한 요청의 경우에는 `null` 반환|
|`response.reviewId`|string|리뷰 ID. 삭제 요청의 경우 `null` 반환|
|`response.increasedPoint`|number|event 를 통해 증가된 사용자 포인트|
|`response.decreasedPoint`|number|event 를 통해 감소된 사용자 포인트|
|`error`|object|에러 객체. 성공한 요청의 경우에는 `null` 반환|
|`error.message`|string|에러 메시지|

#### Examples
```json
// 성공한 요청의 응답
{
  "success": true,
  "response": {
    "reviewId": "bf23a25e-b04e-4e3b-986c-30cac352cb62",
    "increasedPoint": 3,
    "decreasedPoint": 0
  },
  "error": null
}
```

```json
// 실패한 요청의 응답
{
  "success":false,
  "response":null,
  "error": {
    "message": "Review already exists"
  }
}
```


### GET /users/:user_id/point
사용자 포인트 조회 API

#### Examples
```bash
# 사용자 포인트 조회
$ curl 'http://localhost:5000/users/84b5c589-8cc3-478c-9f8f-a801b771fc8a/point' -i \
    -H 'Accept: application/json'
```

#### Response Body

|name|type|description|
|-|-|-|
|`success`|boolean|요청의 성공 여부|
|`response`|object|응답 상세 내용. 실패한 요청의 경우에는 `null` 반환|
|`response.point`|number|사용자의 현재 누적 포인트|
|`error`|object|에러 객체. 성공한 요청의 경우에는 `null` 반환|
|`error.message`|string|에러 메시지|

#### Examples
```json
// 성공한 요청의 응답
{
  "success": true,
  "response": {
    "point": 3
  },
  "error": null
}
```
