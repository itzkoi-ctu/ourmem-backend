# Video processing deployment

The frontend accepts MP4, MOV, WebM, MKV, AVI and M4V up to 100 MiB. Cloudinary validates the actual media and prepares an authenticated MP4 with H.264/AAC, constrained within 1920 × 1920 without cropping or upscaling. Local preview failure does not prevent uploading an otherwise supported container. Codec/account limitations can still cause conversion failure.

## Required before release

1. Apply `src/main/resources/db/migration/V4__video_upload_jobs.sql` to the existing Supabase database before starting the new backend (`ddl-auto=validate`). Flyway is disabled locally: run the migration through your normal migration process, or run this specific SQL in Supabase SQL Editor. Do not rerun V1–V3 or enable baseline version 0 blindly against an existing database. The new table has RLS enabled and is accessed only by the backend database role.
2. Set `VIDEO_CALLBACK_BASE_URL=https://YOUR-BACKEND.fly.dev` on Fly. Use the backend origin, without `/api`. The server supplies a per-job callback URL automatically. No unsigned Cloudinary preset or frontend secret is needed.
3. Webhook signatures use `CLOUDINARY_API_SECRET`. Ensure Cloudinary notification signing uses the matching API key (especially if the account has multiple keys). Keep the standard notification payload and legacy/default signature mode supported by the installed Java SDK. The webhook route is public but verifies the signature over the exact request body, timestamp, and asset ID before making changes.
4. Deploy backend and frontend together. POST `/api/sessions/{id}/video` now returns HTTP 202 with a processing job, not a session. GET `/api/sessions/{id}/video/status` returns the latest persisted job. The UI polls while processing and recovers after reload.
5. Keep frontend video uploads pointed directly to the backend origin; a frontend serverless proxy may have a smaller request-body limit. The backend streams a temporary file to Cloudinary; allow temporary disk space for concurrent uploads and keep the configured multipart limit.

## Behavior and operational checks

- The existing video remains active during upload/conversion. A valid eager-completion callback atomically swaps it; the old asset is deleted only after database commit. Failed conversion does not replace the current video.
- One processing job per session is enforced with a database lock and a partial unique index. Duplicate successful callbacks are idempotent. Timed-out/cancelled callbacks cannot overwrite a newer video.
- After 30 minutes, checking status or starting a new upload marks an unfinished job failed. There is no in-memory worker to lose on a Fly restart. If webhook delivery fails, the current video is preserved; inspect backend logs/Cloudinary notifications, fix delivery, and retry the upload. Late callbacks clean up their unused asset.
- Cloudinary quotas, maximum video size/duration and transformation allowances still apply. This integration does not remove plan limits. Check the account before release with representative phone videos, including a silent clip, portrait MOV/HEVC, landscape MP4, WebM and an invalid file.
- A crash or lost callback can leave an unused Cloudinary upload; failed provider deletions are logged. Periodically reconcile assets under `ourmemory/videos/` with active session IDs and processing jobs. Automatic durable garbage collection is not implemented.
- Existing videos are normalized on first delivery; reupload any legacy video whose on-demand transformation exceeds the account limit. New uploads are prepared before activation.
- Local end-to-end webhook tests require a public HTTPS tunnel to the backend and that origin in `VIDEO_CALLBACK_BASE_URL`; localhost cannot receive Cloudinary callbacks.

## Smoke test before production traffic

Upload a MOV and wait for READY; verify MP4 playback on Safari and Chrome. Replace an existing video with an invalid file and confirm the old video still plays. Reload while processing, replay a signed callback, and verify an altered/unsigned callback gets 403. Exercise conversion failure, a 30-minute timeout, and retry. These checks need the real Supabase/Cloudinary environment; unit tests do not prove provider connectivity or available quota.

References: https://cloudinary.com/documentation/eager_and_incoming_transformations and https://cloudinary.com/documentation/notification_signatures
