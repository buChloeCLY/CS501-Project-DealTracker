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

#### 2. Node.js Backend Setup

Used to serve product data and platform prices.

```
cd backend
npm init -y
npm install express mysql2 axios cors dotenv node-cron openai
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

#### 3. Run Application

Run `app` in Android Studio.


## Current Features

#### Architecture & Backend

- Clean architecture following the MVVM pattern for scalability and maintainability.
- Jetpack navigation enables seamless transitions across all screens.
- ViewModel + StateFlow used for reactive, lifecycle-aware state management.
- RapidAPI integration obtains real-time data from Node.js and Flask backend services.
- Automated price monitoring updates product data daily.

#### Application Features
| Feature                          |  Notes                                                       |
|----------------------------------|-------------------------------------------------------------|
| Multi-platform price integration |  Amazon, Walmart and eBay |
| Wishlist API                     |  Alerts endpoint implemented                                 |
| Cron daily price update          |  One scheduled job  is designed to do it                     |
| User login & register            |  SHA-256 password hash                                       |
| Historical price chart           |  Use Canvas                                     | 
| Basic MVVM                       |  Architecture implemented for scalability                    |
| Price Comparison                 | Supports multiple platforms; more platforms to be added     |
| Sensor Integration               |   Sensors data collected and processed                        |

#### 1. Home Screen

- Intuitive navigation shows four main screens.
- Top search bar implemented in UI.
- Category browsing with intuitive navigation to the Deals screen.
- "Deals of the Day" section showcasing featured sample products.
![home.png](home.png)

#### 2. Deals Screen

- Fetches real-time product data from the Node.js backend API.
- Supports filtering products by price and rating.
- Periodic data updates ensure the latest deals.
- Compare button navigates to detail screen.
![deals.png](delas.png)

#### 3. Detail Screen

- Displays comprehensive product details.
- Historical price charts and trend analysis via Flask API.
- Seamless navigation to e-commerce platforms.
- Allows users to add products to their wishlist for target price tracking.
![detail.png](detail.png)

#### 4. Lists Screen

- Wishlist management allows adding, removing and browsing specific products.
- Set target prices for automatic notifications.
![list.png](list.png)

#### 5. Profile Screen

- Manage user account information and preferences.
- Access wishlist and browsing history using mock data.
- Font size adjustment and dark mode toggle interfaces implemented in UI.
- Log out
![profile.png](profile.png)

#### 6. Login and Register Screen

- Input account and password to login
- Register new account
![register.png](register.png)

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
- **Data Source:** RapidAPI (Real-Time Amazon Data API)
- **Scheduler:** Everyday it will update new prices from different platforms

#### Database
- **Platform:** AWS RDS MySQL
- **Host:** database-1.cjw0amswcib4.us-east-2.rds.amazonaws.com
- **Engine:** MySQL 8.0


## Testing and debugging
### API debugging
I first added log statements in the code and inspected both the Android Logcat output and the backend server logs.
![img1.png](img1.png)
Then I used the RapidAPI console and Postman to manually test each endpoint to confirm that the backend was parsing and returning fields correctly.
![img2.png](img2.png)
Based on these experiments, I drew conclusions from the responses: for example, an HTTP status code `429` indicates that our free quota on RapidAPI has been exhausted and the service stops responding; a timeout error shows that the API is too slow, in which case we should consider removing or relaxing the timeout limit.

### Deep Link debugging
```
adb shell am start -a android.intent.action.VIEW -d "dealtracker://product/3"
```
run command like this, then the app will open the link for the product whose pid is 3.

### Backend debugging
My Deals page started returning a 500 error, and Logcat showed the following messages:

```text
2025-12-02 10:21:14.158 15681-15716 ProductRepository       com.example.dealtracker              E  API Error: 500 - Internal Server Error
2025-12-02 10:21:14.225 15681-15681 DealsViewModel          com.example.dealtracker              E  Failed to load products: API Error: 500 - Internal Server Error
```

This indicated a backend issue. After checking the server logs, I found the following error:

```text
Get products error: Error: read ECONNRESET
    at PromisePool.query (C:\Users\JOY\Desktop\CS501\project\CS501-Project-DealTracker\backend\node_modules\mysql2\lib\promise\pool.js:36:22)       
    at C:\Users\JOY\Desktop\CS501\project\CS501-Project-DealTracker\backend\server.js:900:39
    ...
  code: 'ECONNRESET',
  errno: -4077,
  sql: undefined,
  sqlState: undefined,
  sqlMessage: undefined
}
```

`ECONNRESET` means that the database connection was reset, which usually happens when a query takes too long and the database times out. In my case, the `/api/products` route was performing additional joins on the `price` table for every product, making the query too complex and slow. Since the `products` table already stores the current lowest price for each item, I simplified the endpoint by removing the extra `price` lookups, which resolved the timeout and the 500 error.


## commit, code quality
- all commits of these days are under branch "final", with notes to explain the changes

- Follow project coding standards and use linting/formatting tools (e.g., Kotlin style guidelines).

- Use defensive programming: handle errors, timeouts, and nullability safely.


## AI use
- AI tools are be used to speed up development
- All AI-generated code are reviewed, tested, and verified by the developer.
