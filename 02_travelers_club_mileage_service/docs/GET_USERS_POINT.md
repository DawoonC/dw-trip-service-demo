# GET /users/:user_id/point
사용자 포인트 조회 API

## Request

### Examples

```bash
$ curl 'http://localhost:5000/users/84b5c589-8cc3-478c-9f8f-a801b771fc8a/point' -i \
    -H 'Accept: application/json'
```

## Response

### Response Body

|name|type|description|
|:-|:-|:-|
|`success`|boolean|요청의 성공 여부|
|`response`|object|응답 상세 내용. 실패한 요청의 경우에는 `null` 반환|
|`response.point`|number|사용자의 현재 누적 포인트|
|`error`|object|에러 객체. 성공한 요청의 경우에는 `null` 반환|
|`error.message`|string|에러 메시지|

### Examples

```json
{
  "success": true,
  "response": {
    "point": 3
  },
  "error": null
}
```
