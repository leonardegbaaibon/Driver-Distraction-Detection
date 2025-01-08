# Driver Distraction Detection - Android App

Welcome to **Driver Distraction Detection**! This Android app is designed to detect distractions while driving by monitoring phone usage and using acoustic signals to determine the type of material the phone is placed on. The app aims to reduce road accidents by providing real-time feedback to drivers when distractions are detected.

---

## 🚗 Key Features
- **Phone Usage Detection**: The app tracks when the phone is being used during driving, such as texting, calling, or browsing apps. It will notify the driver if any usage is detected during driving.
- **Acoustic Signal Analysis**: The app uses acoustic signals to detect the type of material the phone is placed on (e.g., seat, dashboard, or other surfaces). This helps in identifying if the phone is likely being used while driving or if it is safely stored.
- **Real-Time Notifications**: Whenever a distraction is detected (whether from phone usage or acoustic signals), the app sends real-time notifications to alert the driver.
- **Background Monitoring**: The app continuously monitors phone usage and acoustic signals in the background while the driver is on the road.
- **Distraction Logs**: Track and view logs of detected distractions to help users monitor their driving habits.

---

## 📱 Platform
- **Android (Mobile)**: This app is designed for Android smartphones, compatible with Android versions 6.0 (API 23) and above.

---

## 🛠️ Technologies Used
- **Programming Language**: Kotlin
- **Acoustic Signal Processing**: Audio API for capturing and processing acoustic signals
- **Phone Usage Detection**: UsageStatsManager API to track app usage during driving
- **Background Services**: Services to monitor phone usage and acoustic signals in the background
- **UI**: Android UI components for displaying notifications and logs

---

## 📑 App Flow

1. **Launch Screen**: Upon launching the app, it asks for necessary permissions (phone usage access, microphone access for acoustic signal detection).
2. **Monitoring Screen**: Once permissions are granted, the app continuously monitors phone usage and acoustic signals while driving.
3. **Distraction Detection**: The app detects if the phone is being used (e.g., sending a text or making a call) or if the acoustic signal suggests that the phone is placed on a distraction-prone surface.
4. **Notifications**: If a distraction is detected, the app sends real-time notifications to the driver, urging them to focus on the road.
5. **Log Screen**: View a log of past distractions, including the type of distraction and when it occurred.

---

## 💻 Getting Started

1. **Clone the repository**:
    ```bash
    git clone https://github.com/yourusername/driver-distraction-detection.git
    ```

2. **Install dependencies**:
    - Ensure you have Android Studio installed and set up on your system.
    - Open the project in Android Studio.
    - Sync the project to download dependencies.

3. **Run the app**:
    - Connect your Android device or use an emulator.
    - Build and run the app from Android Studio.

---

## 📝 Permissions

The app requires the following permissions to function properly:
- **Phone Usage Access**: To track phone usage during driving.
- **Microphone Access**: To detect and analyze acoustic signals from the phone's surroundings.
- **Background Service**: To run monitoring services in the background.

---

## 💡 Future Enhancements
- **Machine Learning**: Integrate machine learning models to better detect specific types of distractions (e.g., voice, text, or app usage patterns).
- **Integration with Car Bluetooth**: Detect whether the phone is connected to the car's Bluetooth system, indicating that the user may be in a vehicle.
- **UI/UX Improvements**: Enhance the user interface with more intuitive navigation and real-time feedback mechanisms.
- **Location-based Alerts**: Add geofencing features to notify users when driving in high-risk areas (e.g., school zones, busy intersections).

---

## 💬 Let's Connect

If you have any feedback, suggestions, or questions, feel free to reach out!

- **GitHub**: [@Leonardegbaaibon](https://github.com/leonardegbaaibon)
- **LinkedIn**: [Leonard Egbaaibon](https://linkedin.com/in/legbaaibon@gmail.com)
- **Email**: your-legbaaibon@gmail.com

---

## 🚀 Contribute

If you'd like to contribute to the development of the **Driver Distraction Detection** app, feel free to fork the repository and create a pull request with your improvements.

---
---

Thank you for checking out **Driver Distraction Detection**—helping drivers stay focused and safe on the road by detecting distractions through acoustic signals and phone usage!
