# Contacts App

This is an Android Contacts App with integrated camera functionality, built using Room and Firestore databases.
The app allows users to manage their contacts, sync them across devices, and view contacts shared by other users.


### Features
1) Add, Edit, and Delete Contacts: Users can add, edit, and delete contacts, which are stored locally using Room and synchronized with Firestore for cloud storage.
2) Camera Integration: Users can take pictures or select images from their gallery to use as contact photos.
3) Contact Synchronization: Contacts are automatically synchronized across devices using Firestore, allowing all users to see each other's contacts.
4) Search Functionality: Users can easily search through their contacts using the built-in search feature.
5) Real-time Updates: The app provides real-time updates of contact information across all connected devices.
6) Data Backup: Contacts are stored in Firestore, providing a reliable backup solution.



### Technology Stack:
1) Android Studio: Development environment for building the app.
2) Kotlin: Primary programming language used for app development.
3) Room Database: Local database solution used for offline storage of contact information.
4) Firestore: Cloud database used for storing and synchronizing contacts across devices.
5) Glide & Picasso: Libraries used for image loading and caching.
6) UCrop: Library for cropping images selected from the gallery or taken using the camera.
7) LiveData & ViewModel: Android Architecture Components used for managing UI-related data in a lifecycle-conscious way.
8) ConstraintLayout: Used for creating responsive and adaptive UI designs.


### Installation:
1) Clone the repository to your local machine: 
 ```
git clone https://github.com/ZOZORIZ/Common_Contacts_App
```
2) Open the project in Android Studio.
3) Build and run the app on an emulator or a physical device.


### Usage
1) Adding Contacts: Click on the "Add Contact" button to create a new contact. You can enter the contact's name, phone number, email, and optionally add a photo using the camera or gallery.
2) Editing Contacts: Click on an existing contact to edit its details. You can update any information, including the photo.
3) Deleting Contacts: Swipe a contact left or right to delete it. The contact will be removed from both the local Room database and Firestore.
4) Viewing Contacts: All contacts, including those shared by other users, are visible in the main contact list.
5) Synchronization: Contacts are automatically synchronized with Firestore, allowing you to access them on any device connected to your account.


### Contributing
Contributions are welcome! Please follow these steps to contribute:

1) Fork the repository.
2) Create a new branch: 
  ```
git checkout -b feature/YourFeatureName
```
3) Make your changes and commit them:
 ```
git commit -m 'Add some feature'
```
4) Push to the branch:
 ```
git push origin feature/YourFeatureName
```
5) Create a pull request.

### Contact
For any questions or feedback, please reach out to Noah Cherian Jacob
