# Incoming Call Feature - Implementation Summary

## Overview
Implemented a real-time incoming call notification system that shows a popup dialog when a user receives a video call in their chat.

## How It Works

### 1. **Call Initiation (Caller Side)**
When User A clicks the video call button in PersonaldmActivity:
1. A call request is created in Firebase under `callRequests/{receiverId}/{callRequestId}`
2. The call request contains:
   - `callerId`: User A's ID
   - `callerUsername`: User A's username
   - `receiverId`: User B's ID
   - `chatId`: Current chat ID
   - `status`: "pending"
   - `timestamp`: When the call was initiated
3. User A is immediately taken to the VideocallActivity

### 2. **Call Reception (Receiver Side)**
User B receives the call:
1. Firebase listener in PersonaldmActivity detects the new call request
2. A popup dialog appears showing:
   - Caller's profile picture
   - Caller's username
   - "Incoming video call..." message
   - Accept button (green with video icon)
   - Decline button (red with end call icon)
3. Dialog auto-dismisses after 30 seconds if not answered (marked as "missed")

### 3. **Call Actions**

**Accept:**
- Updates call status to "accepted" in Firebase
- Opens VideocallActivity with caller's information
- Both users join the same Agora channel

**Decline:**
- Updates call status to "declined" in Firebase
- Shows "Call declined" toast
- Removes call request after 5 seconds

**Missed (timeout):**
- Updates call status to "missed" in Firebase
- Removes call request after 10 seconds

## Files Created/Modified

### New Files:
1. **`app/src/main/java/com/salmankhan/i221285/services/CallService.kt`**
   - Handles call request creation, acceptance, decline, cancellation
   - Manages call status in Firebase

2. **`app/src/main/res/layout/dialog_incoming_call.xml`**
   - Beautiful incoming call popup UI
   - Shows caller info, profile picture, accept/decline buttons

### Modified Files:
1. **`app/src/main/java/com/salmankhan/i221285/PersonaldmActivity.kt`**
   - Added incoming call listener
   - Implemented call initiation logic
   - Added accept/decline call methods
   - Shows incoming call dialog when call is received

## Firebase Database Structure

```
callRequests/
  {receiverId}/
    {callRequestId}/
      - id: "callRequestId"
      - callerId: "user_id_1"
      - callerUsername: "john_doe"
      - receiverId: "user_id_2"
      - chatId: "chat_id"
      - status: "pending" | "accepted" | "declined" | "missed"
      - timestamp: 1699999999999
```

## Features

✅ Real-time call notifications using Firebase listeners
✅ Beautiful incoming call popup dialog
✅ Caller profile picture and username display
✅ Accept/Decline buttons with icons
✅ Auto-dismiss after 30 seconds (missed call)
✅ Call status tracking (pending, accepted, declined, missed)
✅ Proper cleanup of listeners and dialogs
✅ Works only in the active chat (contextual)

## User Experience

**Caller's View:**
1. Clicks video call button
2. Immediately enters call screen
3. Waits for other user to join

**Receiver's View:**
1. Popup appears in their current chat
2. Sees who's calling with profile picture
3. Can accept or decline
4. If ignored, call is marked as missed after 30 seconds

## Notes

- The popup only appears if the receiver is in the same chat
- Dialog is non-cancellable (user must click Accept or Decline)
- Automatic cleanup prevents memory leaks
- Call requests are automatically removed after completion

