# Agora Video Call Setup Instructions

## Current Issue: "Invalid Token" Error

The "Invalid Token" error occurs because your Agora project is configured to require token authentication, but we're trying to join without a valid token.

## Solution Options:

### Option 1: Disable Token Authentication (For Testing Only)

1. Go to [Agora Console](https://console.agora.io)
2. Select your project: `8d4dbd260094458fa2994eab70c587be`
3. Go to **Project Management** → **Config**
4. Under **Authentication**, change from **"Secure mode: Token"** to **"Testing mode: App ID"**
5. Save changes
6. Wait 1-2 minutes for changes to propagate

**Note:** This is only for testing. Do NOT use this in production as it's insecure.

### Option 2: Generate Tokens (Recommended for Production)

If you want to keep token authentication (recommended), you need to generate tokens for each call.

#### Quick Fix: Use Agora Token Generator

1. Go to: https://webdemo.agora.io/token-builder/
2. Enter your App ID: `8d4dbd260094458fa2994eab70c587be`
3. Enter your App Certificate (from Agora Console)
4. Enter Channel Name: any test name like "test123"
5. Enter UID: 0 (allows any user ID)
6. Set expiration time (e.g., 24 hours)
7. Click "Generate"
8. Copy the generated token

Then update the code to use this token (temporarily for testing):

```kotlin
// In VideocallActivity.kt, line 285
val result = AgoraService.joinChannel("YOUR_GENERATED_TOKEN_HERE", channelName, uid)
```

**Important:** This is also just for testing. The token will expire.

#### Long-term Solution: Build a Token Server

For production, you need a backend server that generates tokens on-demand. See:
https://docs.agora.io/en/video-calling/develop/authentication-workflow

## Current Configuration

- App ID: `8d4dbd260094458fa2994eab70c587be`
- Current Setup: Trying to use empty string as token
- Issue: Project requires valid tokens

## Recommendation

For immediate testing: Use **Option 1** (disable token auth in Agora Console)
For production: Implement **Option 2** with a proper token server

