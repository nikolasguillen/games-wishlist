# Contract: Cloud Account and Backup over REST (desktop)

Desktop reaches the same Firebase project as Android and iOS through its REST APIs (research R3). The data, paths
and rules are those of `specs/016-cloud-backup-restore/contracts/firestore.md` and `firebase/firestore.rules`,
unchanged. This file fixes only the wire format desktop uses.

Configuration (research R6): `API_KEY` = `FIREBASE_WEB_API_KEY`, `PROJECT` = `FIREBASE_PROJECT_ID`, `CLIENT_ID` /
`CLIENT_SECRET` = the Desktop app OAuth client.

## Google sign-in (installed-app flow)

1. Generate `code_verifier` (43–128 chars, unreserved set) and `state`; `code_challenge = BASE64URL(SHA256(verifier))`.
2. Listen on `http://127.0.0.1:<ephemeral port>/` (JDK `HttpServer`).
3. Open the system browser at `https://accounts.google.com/o/oauth2/v2/auth?client_id=CLIENT_ID&redirect_uri=…&response_type=code&scope=openid%20email%20profile&code_challenge=…&code_challenge_method=S256&state=…&prompt=select_account`.
4. The redirect carries `code` and `state` (a mismatched `state` → `Unknown`; `error=access_denied` →
   `SignInCancelled`). The page answers with a short "You can return to QuestLog" HTML page, and the server closes.
   `onAccountPicked` is called here.
5. `POST https://oauth2.googleapis.com/token` (form): `code`, `client_id`, `client_secret`, `redirect_uri`,
   `grant_type=authorization_code`, `code_verifier` → `id_token`.
6. No redirect within **5 minutes**, or the caller's coroutine cancelled → the server stops; `SignInCancelled`.

## Firebase Auth (Identity Toolkit)

| Operation | Request | Used fields |
|---|---|---|
| Sign in | `POST https://identitytoolkit.googleapis.com/v1/accounts:signInWithIdp?key=API_KEY` `{"requestUri":"http://localhost","postBody":"id_token=<google id token>&providerId=google.com","returnSecureToken":true,"returnIdpCredential":true}` | `localId`, `email`, `idToken`, `refreshToken`, `expiresIn`, `needConfirmation` |
| Refresh | `POST https://securetoken.googleapis.com/v1/token?key=API_KEY` (form) `grant_type=refresh_token&refresh_token=…` | `id_token`, `refresh_token`, `expires_in`, `user_id` |
| Providers | `POST …/v1/accounts:lookup?key=API_KEY` `{"idToken":…}` | `users[0].providerUserInfo[].providerId`, `users[0].email` |
| Delete | `POST …/v1/accounts:delete?key=API_KEY` `{"idToken":…}` | — |

Errors (`error.message`, prefix before ` : `) → `CloudException`:

| Firebase message | `CloudException` |
|---|---|
| `needConfirmation: true` in a 200 response, `FEDERATED_USER_ID_ALREADY_LINKED` | `CredentialCollision` |
| `CREDENTIAL_TOO_OLD_LOGIN_AGAIN` | `RecentLoginRequired` |
| `TOKEN_EXPIRED`, `INVALID_REFRESH_TOKEN`, `INVALID_ID_TOKEN`, `USER_DISABLED`, `USER_NOT_FOUND` | `Unauthenticated` (the session is cleared) |
| `INVALID_IDP_RESPONSE`, `INVALID_API_KEY`, anything else | `Unknown` |
| Transport failure (`java.net` types) | `Connectivity` |
| Over `CLOUD_CALL_TIMEOUT` | `Timeout` |

## Firestore

Base: `https://firestore.googleapis.com/v1/projects/PROJECT/databases/(default)/documents`, header
`Authorization: Bearer <Firebase ID token>`.

| `CloudBackupStore` | Request |
|---|---|
| `readManifest(uid)` | `GET /users/{uid}`; 404 → `null` |
| `writeManifest(uid, m)` | `PATCH /users/{uid}` with the full document (no `updateMask`: replaces it, as `set` does) |
| `readPart(uid, id)` | `GET /users/{uid}/parts/{id}`; 404 → `null` |
| `writePart(uid, id, p)` | `PATCH /users/{uid}/parts/{id}` |
| `listPartIds(uid)` | `GET /users/{uid}/parts?pageSize=300&mask.fieldPaths=snapshotId`, following `nextPageToken`; id = last path segment of `name` |
| `deleteParts(uid, ids)` | `POST …/documents:commit` with up to 400 `{"delete": "projects/PROJECT/databases/(default)/documents/users/{uid}/parts/{id}"}` writes per call |
| `deleteManifest(uid)` | `DELETE /users/{uid}` |

HTTP 401/403 → `Unauthenticated`. Other non-2xx → `Unknown`. Every call is bounded by `CLOUD_CALL_TIMEOUT`, as in
`FirestoreBackupStore`.

### Value encoding

Fields are those the GitLive serializer writes on mobile, so documents are interchangeable:

| Kotlin | Firestore REST value |
|---|---|
| `String` | `{"stringValue": "…"}` |
| `Int`, `Long` | `{"integerValue": "123"}` (a decimal string, as the API requires) |
| `List<BackupStreamDocument>` | `{"arrayValue": {"values": [{"mapValue": {"fields": {…}}}]}}` |
| `List<String>` | `{"arrayValue": {"values": [{"stringValue": …}]}}` |

Reading accepts `integerValue` and, defensively, `doubleValue` for numbers. An empty array may come back as
`{"arrayValue": {}}`. The codec is a pure function pair with a round-trip test against a document captured from a
phone backup.
