#!/bin/bash

# OrderRoom GitHub Issues 一括作成
# 実行前に:
# gh auth login
# gh repo set-default sisicity4/OrderRoom

gh issue create \
  --title "[PM] MVP完成条件をREADMEに整理する" \
  --body "## やること

オーダールームのMVP完成条件をREADMEに整理する。

## 完成条件

- ホストがルームを作成できる
- 参加者がURLから参加できる
- 参加者が注文を追加できる
- ホストが採用・却下できる
- 採用されたものが買い物リストになる
- 合計金額が表示される

## 担当

taiga"

gh issue create \
  --title "[PM] 画面一覧と優先順位を整理する" \
  --body "## やること

MVPで作る画面一覧と優先順位をREADMEまたはdocsに整理する。

## 対象画面

- トップ画面
- ルーム作成画面
- 参加画面
- ルーム画面
- ホスト管理画面
- 買い物リスト画面

## 完成条件

- MVPで必須の画面が分かる
- 後回しにする画面が分かる
- フロント担当が実装順を判断できる

## 担当

taiga"

gh issue create \
  --title "[PM] API一覧をREADMEに整理する" \
  --body "## やること

フロントとバックエンドが実装しやすいように、REST API一覧をREADMEまたはdocsに整理する。

## 対象API

- ルーム作成
- ルーム取得
- 参加者登録
- アイテム追加
- アイテム一覧取得
- 採用・却下
- 購入チェック
- 集計取得

## 完成条件

- APIのURLが書かれている
- HTTPメソッドが書かれている
- リクエスト例がある
- レスポンス例がある

## 担当

taiga"

gh issue create \
  --title "[BE] Spring Bootの初期構成を作る" \
  --body "## やること

バックエンド用のSpring Bootプロジェクトを作成する。

## 完成条件

- Spring Bootが起動できる
- /api/health にアクセスできる
- READMEに起動方法が書かれている

## 確認方法

- ローカルで起動する
- GET /api/health が成功する

## 担当

えのもと"

gh issue create \
  --title "[BE] PostgreSQL接続設定を作る" \
  --body "## やること

Spring BootからPostgreSQLに接続できるようにする。

## 完成条件

- PostgreSQLに接続できる
- application.properties または application.yml に接続設定がある
- ローカル起動時にDB接続エラーが出ない

## 担当

えのもと"

gh issue create \
  --title "[BE] roomsテーブルとRoom Entityを作る" \
  --body "## やること

ルーム情報を保存するroomsテーブルとRoom Entityを作る。

## カラム案

- id
- title
- date
- memo
- host_key
- created_at

## 完成条件

- Room Entityがある
- RoomRepositoryがある
- roomsテーブルに保存できる

## 担当

えのもと"

gh issue create \
  --title "[BE] participantsテーブルとParticipant Entityを作る" \
  --body "## やること

参加者情報を保存するparticipantsテーブルとParticipant Entityを作る。

## カラム案

- id
- room_id
- name
- created_at

## 完成条件

- Participant Entityがある
- ParticipantRepositoryがある
- room_idでルームと紐づく

## 担当

えのもと"

gh issue create \
  --title "[BE] itemsテーブルとItem Entityを作る" \
  --body "## やること

提案アイテムを保存するitemsテーブルとItem Entityを作る。

## カラム案

- id
- room_id
- participant_id
- name
- price
- quantity
- memo
- status
- purchased
- created_at

## status

- proposed
- accepted
- rejected

## 完成条件

- Item Entityがある
- ItemRepositoryがある
- 参加者とルームに紐づく
- statusとpurchasedを持つ

## 担当

えのもと"

gh issue create \
  --title "[BE] ルーム作成APIを作る" \
  --body "## やること

ホストがイベント用のルームを作成できるAPIを作る。

## API案

POST /api/rooms

## 完成条件

- ルームを作成できる
- host_keyが自動生成される
- 参加用URLとホスト用管理URLに必要な情報を返せる

## 担当

えのもと"

gh issue create \
  --title "[BE] 参加者登録APIを作る" \
  --body "## やること

参加者が名前だけでルームに参加できるAPIを作る。

## API案

POST /api/rooms/{roomId}/participants

## 完成条件

- 名前を登録できる
- ログインなしで参加できる
- room_idと参加者が紐づく

## 担当

えのもと"

gh issue create \
  --title "[BE] アイテム追加APIを作る" \
  --body "## やること

参加者が欲しいものを提案できるAPIを作る。

## API案

POST /api/rooms/{roomId}/items

## 入力項目

- participantId
- name
- price
- quantity
- memo

## 完成条件

- アイテムを追加できる
- 追加者が紐づく
- 初期statusがproposedになる

## 担当

えのもと"

gh issue create \
  --title "[BE] アイテム一覧取得APIを作る" \
  --body "## やること

ルーム内の提案アイテム一覧を取得できるAPIを作る。

## API案

GET /api/rooms/{roomId}/items

## 完成条件

- ルーム内のアイテム一覧を取得できる
- 商品名、価格、数量、参加者名、status、purchasedが分かる
- フロントで一覧表示しやすい形式で返す

## 担当

えのもと"

gh issue create \
  --title "[BE] host_keyによるホスト確認を作る" \
  --body "## やること

ホスト用管理URLに含まれるhost_keyで、ホスト操作を判定する仕組みを作る。

## 完成条件

- host_keyが一致した場合だけホスト操作できる
- host_keyが違う場合は403を返す
- 採用・却下・購入チェックAPIで使える

## 担当

えのもと"

gh issue create \
  --title "[BE] 採用・却下APIを作る" \
  --body "## やること

ホストが提案アイテムを採用・却下できるAPIを作る。

## API案

PATCH /api/rooms/{roomId}/items/{itemId}/status

## 完成条件

- proposed / accepted / rejected を更新できる
- host_keyが必要
- 不正なstatusは更新できない

## 担当

えのもと"

gh issue create \
  --title "[BE] 購入チェックAPIを作る" \
  --body "## やること

ホストが採用済みアイテムに購入済みチェックを付けられるAPIを作る。

## API案

PATCH /api/rooms/{roomId}/items/{itemId}/purchased

## 完成条件

- purchasedをtrue/falseで更新できる
- host_keyが必要
- ホスト管理画面から使える

## 担当

えのもと"

gh issue create \
  --title "[BE] 集計APIを作る" \
  --body "## やること

ルーム内の提案データから集計値を計算して返すAPIを作る。

## API案

GET /api/rooms/{roomId}/summary

## 集計内容

- 全体の合計金額
- 個人別の合計金額
- 商品ごとの合計数量

## 注意

集計値はDBに保存しない。
提案データから計算して返す。

## 担当

えのもと"

gh issue create \
  --title "[BE] バリデーションとエラー処理を入れる" \
  --body "## やること

APIの入力チェックとエラー応答を整理する。

## チェック項目

- 名前が空ではない
- 商品名が空ではない
- 価格が0以上
- 数量が1以上
- 存在しないroomIdは404
- host_key不一致は403

## 完成条件

- 不正な入力で保存されない
- フロントがエラー内容を判断できる

## 担当

えのもと"

gh issue create \
  --title "[FE] React + TypeScriptの初期構成を作る" \
  --body "## やること

フロントエンド用のReact + TypeScript環境を作成する。

## 完成条件

- ViteでReactアプリが起動できる
- トップ画面が表示される
- READMEに起動方法が書かれている

## 確認方法

- npm install
- npm run dev

## 担当

なり"

gh issue create \
  --title "[FE] ルーム作成画面を作る" \
  --body "## やること

ホストがイベント用のルームを作成する画面を作る。

## 入力項目

- イベント名
- 日付
- メモ

## 完成条件

- 入力フォームがある
- ルーム作成APIを呼べる
- 作成後に参加URLまたはホストURLを確認できる

## 担当

なり"

gh issue create \
  --title "[FE] 参加画面を作る" \
  --body "## やること

参加者がURLからアクセスし、名前を入力して参加できる画面を作る。

## 完成条件

- 名前入力フォームがある
- 参加者登録APIを呼べる
- 登録後にルーム画面へ進める

## 担当

なり"

gh issue create \
  --title "[FE] 注文入力フォームを作る" \
  --body "## やること

参加者が欲しいものを提案できる入力フォームを作る。

## 入力項目

- 商品名
- 価格
- 数量
- メモ

## 完成条件

- アイテム追加APIを呼べる
- 追加後に一覧へ反映できる
- スマホで入力しやすい

## 担当

なり"

gh issue create \
  --title "[FE] 注文一覧画面を作る" \
  --body "## やること

ルーム内の提案アイテムを一覧表示する画面を作る。

## 表示項目

- 商品名
- 価格
- 数量
- 追加した人
- status
- purchased

## 完成条件

- アイテム一覧APIから表示できる
- 誰が何を提案したか分かる
- スマホで見やすい

## 担当

なり"

gh issue create \
  --title "[FE] ホスト管理画面を作る" \
  --body "## やること

ホストが提案の採用・却下・購入チェックを操作できる画面を作る。

## 操作

- 採用
- 却下
- 購入済みチェック

## 完成条件

- host_key付きURLで開ける
- 採用・却下APIを呼べる
- 購入チェックAPIを呼べる

## 担当

なり"

gh issue create \
  --title "[FE] 集計表示を作る" \
  --body "## やること

ルーム内の合計金額や商品別数量を表示する。

## 表示内容

- 全体の合計金額
- 個人別の合計金額
- 商品ごとの合計数量

## 完成条件

- 集計APIを呼べる
- 代表者が1画面で確認できる
- 一覧画面と同じ画面に出してもよい

## 担当

なり"

gh issue create \
  --title "[QA] MVPの手動テストリストを作る" \
  --body "## やること

MVPの完成確認に使う手動テストリストを作る。

## 確認項目

- ルーム作成
- 参加者登録
- 注文追加
- 注文一覧表示
- 採用・却下
- 購入チェック
- 集計表示

## 完成条件

- docs/test-list.md にテスト項目がある
- PMと開発担当が確認に使える

## 担当

tomoya"

gh issue create \
  --title "[QA] 参加者URLとホストURLの導線を確認する" \
  --body "## やること

参加者用URLとホスト用管理URLの導線を確認する。

## 確認内容

- 参加者URLから名前入力に進める
- ホストURLから管理画面に進める
- host_keyがない場合に管理操作できない
- スマホで迷わない

## 完成条件

- 導線の問題点がIssueまたはdocsに整理されている

## 担当

tomoya"

gh issue create \
  --title "[QA] 競合との差分を整理する" \
  --body "## やること

オーダールームと近いサービスとの差分を整理する。

## 比較対象

- モバイルオーダー
- 割り勘アプリ
- 買い物リストアプリ

## 完成条件

- 発表で説明できる差分が整理されている
- オーダールームの強みが一言で説明できる

## 担当

tomoya
