# API設計メモ

## 方針

- REST APIで作る
- レスポンスはJSON
- MVPではログインを作らない
- ホスト操作は `host_key` で判定する
- 集計値はDBに保存せず、アイテム情報から計算して返す

---

## ルーム

### ルーム作成

`POST /api/rooms`

```json
{
  "title": "七夕パーティー",
  "date": "2026-07-20",
  "memo": "買い出し用"
}
```

### ルーム取得

`GET /api/rooms/{roomId}`

---

## 参加者

### 参加者登録

`POST /api/rooms/{roomId}/participants`

```json
{
  "name": "たいが"
}
```

---

## アイテム

### アイテム追加

`POST /api/rooms/{roomId}/items`

```json
{
  "participantId": 1,
  "name": "コーラ",
  "price": 180,
  "quantity": 3,
  "memo": "ゼロもあり"
}
```

### アイテム一覧取得

`GET /api/rooms/{roomId}/items`

### 採用・却下

`PATCH /api/rooms/{roomId}/items/{itemId}/status?hostKey={hostKey}`

```json
{
  "status": "accepted"
}
```

### 購入チェック

`PATCH /api/rooms/{roomId}/items/{itemId}/purchased?hostKey={hostKey}`

```json
{
  "purchased": true
}
```

---

## 集計

### 集計取得

`GET /api/rooms/{roomId}/summary`

返す内容:

- 全体の合計金額
- 個人別の合計金額
- 商品ごとの合計数量

---

## エラー方針

- 存在しない `roomId`: 404
- `host_key` 不一致: 403
- 入力値不正: 400
