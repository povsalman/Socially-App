# **Socially – Android Social Networking App**

Socially is a full-featured social networking Android application built using **Kotlin** and **Firebase**.  
The project demonstrates modern mobile-app development practices and implements essential social-media features such as user authentication, posting, following, real-time interactions, and messaging.

---

## **🌟 Core Features**

### **1. User Authentication**
- Secure login and registration using Firebase Authentication  
- Email + Password based signup  
- Automatic session management  

---

### **2. User Profiles**
- Upload profile picture  
- Edit name, bio, and personal details  
- View own and other users’ profiles  

---

### **3. Social Feed**
- Real-time feed showing posts from followed users  
- Like and unlike posts  
- Comment on posts  
- View detailed post screen  

---

### **4. Stories (Optional Feature)**
- Upload temporary image/video stories  
- 24-hour expiry system  
- Story viewer tracking  

---

### **5. Photo Sharing**
- Upload images from gallery or camera  
- Firebase Storage integration  
- Smooth image rendering with Glide/Coil  

---

### **6. Followers System**
- Follow / Unfollow users  
- Dedicated followers and following lists  
- Dynamic feed updates based on followed accounts  

---

### **7. Real-Time Direct Messaging**
- One-to-one chat using Firestore / Realtime Database  
- Read receipts (Seen status)  
- Timestamps for messages  

---

### **8. Notifications**
Real-time push notifications for:
- Likes  
- Comments  
- New followers  
- Messages  

---

## **📱 Tech Stack**

### **Frontend**
- **Kotlin (Android)**  
- Jetpack Components:  
  - ViewModel  
  - LiveData / StateFlow  
  - Navigation Component  
- RecyclerView  
- Image loading: Glide / Coil  

### **Backend**
- **Firebase Authentication**  
- **Firestore / Realtime Database**  
- **Firebase Storage**  
- **Firebase Cloud Messaging (FCM)**  

---

## **📂 Project Structure**

```

app/
├── data/
│   ├── models/         # User, Post, Message, Story data classes
│   ├── repository/     # Firebase CRUD logic
│
├── ui/
│   ├── auth/           # Login & Signup
│   ├── feed/           # Home feed & posts
│   ├── profile/        # Profile & edit profile
│   ├── chat/           # Direct messages
│   └── story/          # Stories (optional)
│
├── utils/              # Helpers, extensions, constants
├── viewmodel/          # MVVM ViewModels

```

---

## **⚙️ Setup Instructions**

1. Clone the repository:
   ```bash
   git clone https://github.com/<your-repo>/socially.git
  

2. Open the project in **Android Studio**.

3. Connect Firebase:

   ```
   Tools → Firebase → Connect to Firebase
   ```

4. Enable Firebase services:

   * Authentication
   * Firestore / Realtime Database
   * Cloud Storage
   * Cloud Messaging

5. Run the project on an emulator or physical device.

---

## **📝 Assignment Notes**

This project demonstrates:

* Practical use of Firebase in Android applications
* Clean and structured Kotlin implementation
* MVVM architecture
* Real-time social platform features
* Cloud-based data storage and synchronization

---


