# Contract: Firestore Layout and Security Rules

The only data the app keeps in the cloud. `:core:cloud` owns these shapes, and `:core:data` owns what is inside
`data`.

## Documents

```
users/{uid}                     BackupManifestDocument   (absent: no backup)
users/{uid}/parts/{partId}      BackupPartDocument
```

| Path | Field | Type | Notes |
|---|---|---|---|
| `users/{uid}` | `snapshotId` | string | random, new for every backup |
| | `formatVersion` | int | `BACKUP_FORMAT_VERSION` of the writer |
| | `createdAtEpochMillis` | int | client clock, shown as "Last backup" |
| | `appVersion` | string | the writer's `versionName`, for support only |
| | `listCount`, `gameCount` | int | shown in the summary line |
| | `streams` | array of `{ name, byteLength, partIds[] }` | `name` is `data` or `cover-<listId>`; `data` first |
| `users/{uid}/parts/{partId}` | `snapshotId` | string | lets a sweep tell current from stale |
| | `stream`, `index` | string, int | position within the stream |
| | `data` | string | base64, at most 700,000 characters |

`partId` = `{snapshotId}-{stream}-{index}`.

## Write protocol

1. Read `users/{uid}` from the server (fails fast offline; gives the previous snapshot).
2. Write every part of the new snapshot, one document at a time.
3. Write `users/{uid}`. **The backup now exists.**
4. Delete every part listed under `users/{uid}/parts` that the new manifest does not reference. This is best
   effort, and the next backup repeats it.

A failure before step 3 leaves the previous manifest pointing at the previous parts, which are untouched.

## Read protocol

Read `users/{uid}` from the server, refuse when `formatVersion > BACKUP_FORMAT_VERSION`, then read exactly the
parts its `streams` list. A missing part, or a stream whose decoded length differs from `byteLength`, makes the
backup unreadable.

## Delete protocol

Delete every document under `users/{uid}/parts`, then `users/{uid}`.

## Security rules: `firebase/firestore.rules`

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // A signed-in user reads and writes their own backup and nothing else.
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
    // Everything else is denied by default.
  }
}
```

The file is committed and pasted into the console's Rules tab. The project has no Firebase CLI, so deploying is a
manual step in [quickstart.md](../quickstart.md).

## Quota check (Spark plan)

A 500-game backup with five covers is about 10 parts: ~11 writes and ~10 deletes per backup, and ~11 reads per
restore. Storage is a few MB per user. Each is orders of magnitude inside 20,000 writes, 20,000 deletes and 50,000
reads per day and 1 GiB stored.
