# Firebase Realtime Database Schema for Instagram-like App

## 📊 Database Structure

```
socially-app/
├── users/
│   └── {userId}/
│       ├── username: string
│       ├── firstName: string
│       ├── lastName: string
│       ├── email: string
│       ├── bio: string
│       ├── profileImage: string (base64)
│       ├── profileCompleted: boolean
│       ├── createdAt: timestamp
│       ├── followersCount: number
│       ├── followingCount: number
│       └── postsCount: number
│
├── posts/
│   └── {postId}/
│       ├── id: string
│       ├── userId: string
│       ├── username: string
│       ├── userProfileImage: string (base64)
│       ├── imageBase64: string
│       ├── caption: string
│       ├── createdAt: timestamp
│       ├── likesCount: number
│       └── commentsCount: number
│
├── stories/
│   └── {storyId}/
│       ├── id: string
│       ├── userId: string
│       ├── username: string
│       ├── userProfileImage: string (base64)
│       ├── imageBase64: string
│       ├── createdAt: timestamp
│       ├── expiresAt: timestamp (24 hours)
│       └── isActive: boolean
│
├── likes/
│   └── {postId}/
│       └── {userId}: timestamp
│
├── comments/
│   └── {postId}/
│       └── {commentId}/
│           ├── id: string
│           ├── postId: string
│           ├── userId: string
│           ├── username: string
│           ├── text: string
│           └── createdAt: timestamp
│
├── followers/
│   └── {userId}/
│       └── {followerId}: timestamp
│
├── following/
│   └── {userId}/
│       └── {followingId}: timestamp
│
├── followRequests/
│   └── {toUserId}/
│       └── {fromUserId}/
│           ├── status: "pending" | "accepted" | "rejected"
│           └── createdAt: timestamp
│
├── chats/
│   └── {chatId}/  (combination of user IDs, sorted)
│       ├── user1Id: string
│       ├── user2Id: string
│       ├── lastMessage: string
│       ├── lastMessageTime: timestamp
│       └── unreadCount_user1: number
│       └── unreadCount_user2: number
│
└── messages/
    └── {chatId}/
        └── {messageId}/
            ├── id: string
            ├── senderId: string
            ├── receiverId: string
            ├── type: "text" | "image" | "post"
            ├── content: string (text or base64)
            ├── postId: string (if type is "post")
            ├── createdAt: timestamp
            ├── editedAt: timestamp
            ├── isEdited: boolean
            └── isDeleted: boolean
```

## 🎯 Key Design Decisions:

### 1. **Feed Algorithm** (User A follows B and C):
- Query `following/{userA}` to get list of followed users [B, C]
- Query `posts/` where `userId in [B, C]` to get their posts
- Query `stories/` where `userId in [B, C]` and `isActive = true`

### 2. **Base64 Storage**:
- All images stored as Base64 strings
- Profile images, post images, story images, message images
- No Firebase Cloud Storage needed (free plan)

### 3. **Chat ID Format**:
- Combine two user IDs in sorted order: `{userId1}_{userId2}`
- Example: User A (id: "abc") chats with User B (id: "xyz") → chatId: "abc_xyz"
- This ensures one chat per user pair

### 4. **Message Editing/Deletion**:
- Store `createdAt` timestamp
- Check if `currentTime - createdAt <= 5 minutes`
- Allow edit/delete only within 5 minutes

### 5. **Denormalization**:
- Store counts (followers, following, likes, comments) for quick display
- Store username and profile image in posts/stories for quick rendering

## 📱 Implementation Mapping:

### Pages and Their Features:

1. **HomeFragment** (fragment_home.xml)
   - Display stories of followed users
   - Display posts of followed users
   - Like posts
   - Comment on posts

2. **SelfProfileFragment** (fragment_self_profile.xml)
   - Display user's own posts
   - Edit profile picture
   - View followers/following

3. **PostImageFragment** (fragment_post_image.xml)
   - Upload new posts with images
   - Add captions

4. **DmActivity** (activity_dm.xml)
   - List of chats

5. **PersonaldmActivity** (activity_personaldm.xml)
   - Send text messages
   - Send images
   - Share posts
   - Edit/delete messages (within 5 min)
   - Voice/video calls

6. **ExploreFragment** (fragment_explore.xml)
   - Discover new users
   - Follow/unfollow users

7. **NotificationYouFragment** (fragment_notification_you.xml)
   - Follow requests
   - Likes on posts
   - Comments on posts

## 🔄 Real-time Updates:
- Use Firebase ValueEventListener for real-time data
- Update counts automatically
- Sync messages in real-time

