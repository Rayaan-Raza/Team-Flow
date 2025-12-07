# TeamFlow Push Notification Service

Push notification backend for TeamFlow app, hosted on Vercel.

## Setup

1. Install Vercel CLI:
```bash
npm i -g vercel
```

2. Login to Vercel:
```bash
vercel login
```

3. Deploy:
```bash
cd vercel-notifications
vercel
```

4. Add Environment Variable in Vercel Dashboard:
   - Name: `FIREBASE_SERVICE_ACCOUNT`
   - Value: Your Firebase service account JSON (as a string)

## API Endpoints

### POST /api/send-notification
Send notification to a specific FCM token.

```json
{
  "token": "FCM_DEVICE_TOKEN",
  "title": "New Message",
  "body": "You have a new message",
  "data": { "type": "message" }
}
```

### POST /api/notify-user
Send notification to a user by Firebase UID (looks up FCM token from RTDB).

```json
{
  "uid": "FIREBASE_USER_UID",
  "title": "Task Completed",
  "body": "John marked task as done",
  "type": "task_done",
  "data": { "taskId": "123" }
}
```
