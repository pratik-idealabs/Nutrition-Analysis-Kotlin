# Nutrition Analysis Android App

## Overview
This Android app allows users to capture images of food items, analyze their nutritional content using Google Vision API, and store the results locally using SQLite. Users can view their past nutrition records for reference.

## **Workflow**
1. **User Captures an Image**
   - The app uses the device's camera or allows image selection from storage.
   - The selected image is sent to the **Google Vision API** for analysis.
   
2. **Image Processing and Analysis**
   - The app sends the image to Google Vision API.
   - The API responds with extracted **nutritional details** such as food name, calorie count, and other information.

3. **Displaying Results on the UI**
   - The received nutritional data is displayed to the user in **MainActivity**.

4. **Storing the Nutrition Record in the Database**
   - The app saves the analyzed nutritional data into an **SQLite database** using **DatabaseHelper**.
   - The stored data includes:
     - Food name
     - Calories
     - Other nutritional details

5. **Viewing Past Records**
   - Users can navigate to **ViewRecordsActivity** to access their stored nutrition history.
   - Data is retrieved from **SQLite** and displayed using a **RecyclerView** with **RecordAdapter**.

---
## **File Structure & Functionality**

### **MainActivity.kt**
- The **entry point** of the app.
- Handles:
  - **Image capture** from the camera.
  - **Sending images** to the API.
  - **Receiving and displaying** nutrition details.

### **DatabaseHelper.kt**
- Manages **SQLite database** operations.
- Key functions:
  - `onCreate()`: Initializes database tables.
  - `insertRecord()`: Saves analyzed nutrition data.
  - `getRecords()`: Retrieves stored nutrition records.

### **NutritionRecord.kt**
- A **data model** representing a food entry.
- Fields:
  - Food name
  - Calories
  - Other nutrition details

### **ViewRecordsActivity.kt**
- Displays previously saved nutrition records.
- Uses **RecyclerView** with **RecordAdapter** to format data for UI.

### **RecordAdapter.kt**
- Handles **list formatting** of stored nutrition records.
- Works with **RecyclerView** in **ViewRecordsActivity**.

### **ApiKeys.kt**
- Stores API keys for **Google Vision API**.
- Enables image processing and nutrition analysis.

---
## **UI and Backend Connection**
- **UI components** are defined in XML layout files (`activity_main.xml`, `activity_view_records.xml`).
- **MainActivity** interacts with the backend using network requests to Google Vision API.
- **DatabaseHelper** manages storage and retrieval of nutrition records.

---
## **Database Used**
- **SQLite (Local Database)**
- Used to **store processed nutrition data** for offline access.
- Advantages:
  - Fast and lightweight.
  - Works without internet connection.


