# バックエンド セットアップ手順

OrderRoom のバックエンド（Spring Boot）をローカルで起動するための手順です。

## 技術スタック

| 項目 | 内容 |
| --- | --- |
| 言語 | Java 21 |
| フレームワーク | Spring Boot 4.1.0（Spring MVC / Spring Data JPA / Validation） |
| ビルドツール | Maven（Maven Wrapper `mvnw` 同梱 / Maven 3.9.16） |
| データベース | PostgreSQL 13 以上 |
| その他 | Lombok, Spring Boot DevTools |

プロジェクトのルートは `OrderRoom/backend/orderroom-backend/` です。

## 1. 前提ツールのインストール

以下がインストール済みであることを確認します。

```bash
java -version   # 21 以上であること
psql --version  # PostgreSQL クライアント
```

- **JDK 21**: 未インストールの場合は [Temurin (Adoptium)](https://adoptium.net/) などから導入してください。
- **PostgreSQL 13+**: ローカルに PostgreSQL サーバーを用意します（Homebrew の場合 `brew install postgresql@16`）。
- **Maven**: ラッパー（`./mvnw`）を使うため個別インストールは不要です。

## 2. データベースの準備

PostgreSQL に接続用のデータベースとユーザーを作成します。

```bash
# PostgreSQL に接続
psql -U postgres

# データベース作成（例）
CREATE DATABASE orderroom;
CREATE USER orderroom WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE orderroom TO orderroom;
\q
```

> テーブルは JPA の `ddl-auto=update` によりアプリ起動時に自動生成されます。
> `gen_random_uuid()` を利用するため、必要に応じて `CREATE EXTENSION IF NOT EXISTS pgcrypto;` を実行してください（PostgreSQL 13+ では標準で利用可能）。

## 3. 環境変数（.env）の設定

`application.properties` は接続情報を `.env` から読み込みます（`spring.config.import=optional:file:.env[.properties]`）。

`OrderRoom/backend/orderroom-backend/` 直下に `.env` を作成し、以下を設定します。

```properties
DB_URL=jdbc:postgresql://localhost:5432/orderroom
DB_USERNAME=orderroom
DB_PASSWORD=yourpassword
```

| 変数 | 説明 |
| --- | --- |
| `DB_URL` | JDBC 接続 URL（`jdbc:postgresql://<host>:<port>/<db名>`） |
| `DB_USERNAME` | DB 接続ユーザー名 |
| `DB_PASSWORD` | DB 接続パスワード |

> `.env` は機密情報のため Git 管理対象外です。雛形として `.env.sample` を参照してください。

## 4. ビルド

初回はプロジェクトルートで依存関係の取得とビルドを行います。

```bash
cd OrderRoom/backend/orderroom-backend

# 依存取得＆ビルド（テストをスキップする場合は -DskipTests）
./mvnw clean package
```

Windows の場合は `mvnw.cmd clean package` を使用します。

## 5. アプリケーションの起動

```bash
# 開発起動（ホットリロード有効：DevTools）
./mvnw spring-boot:run
```

または、ビルド済みの JAR を直接実行します。

```bash
java -jar target/orderroom-backend-0.0.1-SNAPSHOT.jar
```

起動後、デフォルトで `http://localhost:8080` で待ち受けます。

## 6. 動作確認

- 起動ログに Hibernate が発行する SQL（`spring.jpa.show-sql=true`）が表示されること。
- 起動時にエラーなく DB へ接続でき、テーブルが自動生成されること。
- API の詳細は [テーブル_API設計書.md](テーブル_API設計書.md) を参照してください。

## 7. テストの実行

```bash
./mvnw test
```

## トラブルシューティング

| 症状 | 対処 |
| --- | --- |
| `Connection refused` / DB 接続失敗 | PostgreSQL が起動しているか、`.env` の `DB_URL` / ユーザー / パスワードが正しいか確認 |
| `Unknown database` | 手順 2 でデータベースを作成したか確認 |
| `gen_random_uuid() does not exist` | `CREATE EXTENSION IF NOT EXISTS pgcrypto;` を実行 |
| Java バージョンエラー | `java -version` が 21 以上であることを確認 |
| ポート競合（8080） | `application.properties` に `server.port=<別ポート>` を追加 |
