# CS501-Project-DealTracker

DealTracker is an intelligent Android application designed to help users make informed purchasing decisions by comparing real-time prices from multiple e-commerce platforms. Users can search for products, view live price updates, subscribe to price-drop alerts, and receive personalized shopping recommendations based on browsing habits. The app leverages Kotlin, Jetpack Compose, and Node.js backend to deliver a modern, responsive, and delightful shopping experience.



## Build & Run Instructions

#### Prerequisites

- Node.js 16+
- Python 3.8+
- MySQL 8.0
- Android Studio Narwhal 3
- RapidAPI Key
- Git

#### 1. Android Setup

```
git clone https://github.com/buChloeCLY/CS501-Project-DealTracker.git
cd CS501-Project-DealTracker
```

- Open DealTracker/ in Android Studio.
- Sync Gradle and wait for dependencies to resolve.
- For physical device testing, replace localhost with your computer's actual IP in:
  - Node.js Backend Client: data/remote/repository/RetrofitClient.kt
  - Flask Backend Client: data/remote/RetrofitClient.kt

#### 2. Node.js Backend Setup

Used to serve product data and platform prices.

```
cd backend
npm init -y
npm install express mysql2 axios cors dotenv node-cron
node server.js
```

Expected Output

```
Database connected successfully
Server running on http://localhost:8080
Daily update scheduled at 3:00 AM EST
```

Import Initial Data

```
curl -X POST http://localhost:8080/api/admin/import-initial
```

#### 3. Flask Backend Setup

Used to serve mock historical price data for detail screen.

```
cd backend
pip install flask flask-cors pymysql
python app.py
```

Expected Output

```
* Running on http://0.0.0.0:5001
```

#### 4. Run Application

Run `app` in Android Studio.



## Current Features

#### Architecture & Backend

- Clean architecture following the MVVM pattern for scalability and maintainability.
- Jetpack navigation enables seamless transitions across all screens.
- ViewModel + StateFlow used for reactive, lifecycle-aware state management.
- RapidAPI integration obtains real-time data from Node.js and Flask backend services.
- Automated price monitoring updates product data daily.

#### Application Features

#### 1. Home Screen

- Intuitive navigation shows four main screens.
- Top search bar implemented in UI.
- Category browsing with intuitive navigation to the Deals screen.
- “Deals of the Day” section showcasing featured sample products.

#### 2. Deals Screen

- Fetches real-time product data from the Node.js backend API.
- Supports filtering products by price and rating.
- Periodic data updates ensure the latest deals.
- Compare button navigates to detail screen.

#### 3. Detail Screen

- Displays comprehensive product details.
- Historical price charts and trend analysis via Flask API.
- Seamless navigation to e-commerce platforms.
- Allows users to add products to their wishlist for target price tracking.

#### 4. Lists Screen

- Wishlist management allows adding, removing and browsing specific products.
- Set target prices for automatic notifications.

#### 5. Profile Screen

- Manage user account information and preferences.
- Access wishlist and browsing history using mock data.
- Font size adjustment and dark mode toggle interfaces implemented in UI.



## Tech Stack

#### Frontend (Android)

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose + Material 3
- **Architecture:** MVVM with Clean Architecture
- **Networking:** Retrofit 2 + OkHttp
- **Image Loading:** Coil
- **Async:** Kotlin Coroutines + Flow

#### Backend
- **Primary:** Node.js + Express.js
- **Testing (Legacy):** Flask (Python) - for initial database connection testing
- **Data Source:** RapidAPI (Real-Time Amazon Data API)
- **Scheduler:** node-cron

#### Database
- **Platform:** AWS RDS MySQL
- **Host:** database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com
- **Engine:** MySQL 8.0

