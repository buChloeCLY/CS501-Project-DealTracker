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
| Feature                          | Status      | Notes                                                       |
|----------------------------------|-------------|-------------------------------------------------------------|
| Multi-platform price integration | Completed   | Amazon /Walmart, but more platforms will be added in future |
| Wishlist API                     | Completed   | Alerts endpoint implemented                                 |
| Cron daily price update          | In Progress | Testing scheduled job                                       |
| User login & register            | Completed   | SHA-256 password hash                                       |
| Historical price chart           | Completed     |                                                             | |
| Basic MVVM                       |  Completed     | Architecture implemented for scalability                    |
| Price Comparison                 | Completed      | Supports multiple platforms; more platforms to be added     |
| Sensor Integration               |  Completed     | Sensors data collected and processed                        |
| AI Recommendation                |  Not Implemented | Planned for future release                                  |
| Price Alert / Wishlist           | In Progress    | Alerts endpoint implemented; testing ongoing                |

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
- Log out

#### 6. Login and Register Screen

- Input account and password to login
- Register new account

## API references

### a) User Management (`/api/user/...`)

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/api/user/:uid` | Fetch basic user profile (uid, name, email, gender, timestamps) |
| POST | `/api/user/login` | Log in with email + password (hashed with SHA-256), returns user info on success |
| POST | `/api/user/register` | Register a new user, checks duplicate email, stores hashed password |
| PUT | `/api/user/:uid` | Update user fields (name, email, gender, password) |
| DELETE | `/api/user/:uid` | Delete a user by UID |

---

### b) Price & History APIs

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/api/price/:pid` | Returns the latest price per platform for a product (price, free_shipping, in_stock, link) |
| GET | `/api/history/:pid?days=N` | Returns daily minimum price history for the last N days |
| GET | `/api/products/:pid/lowest-price` | Computes current lowest price across platforms and returns: <br> - `lowestPrice` – minimum price <br> - `platforms` – platforms sharing lowest price <br> - `allPrices` – latest price from every platform |

---

### c) Product Management (`/api/products/...`)

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/api/products` | Returns product list with optional filters: category, search, min_price, max_price, in_stock, free_shipping |
| GET | `/api/products/:pid` | Returns a single product from the products table |

---

### d) Wishlist APIs

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/api/wishlist?uid=UID` | Returns wishlist items for a user: pid, target_price, product info, current_price |
| POST | `/api/wishlist` | Add or update wishlist entry (`uid`, `pid`, `target_price`) |
| DELETE | `/api/wishlist` | Remove wishlist item by `uid` + `pid` |
| GET | `/api/wishlist/alerts?uid=UID` | Returns wishlist items where current lowest price <= target_price for notifications |

---

### e) Admin / Maintenance APIs

| Method | Endpoint | Description |
|--------|---------|-------------|
| POST | `/api/admin/import-initial` | Imports seed products from Amazon, then adds BestBuy/Walmart prices |
| POST | `/api/admin/update-all-prices` | Fetches fresh prices for all products from all platforms |
| POST | `/api/admin/sync-lowest-prices` | Computes the cheapest platform for each product and updates the `products` table |

---

### f) Scheduled Job

- A cron job runs **daily at 3:00 AM (America/New_York)**:
  - Fetches new prices from Amazon / BestBuy / Walmart for all products
  - Inserts new rows into `price` table
  - Calls `syncLowestPrices()` to update the `products` table to the latest lowest prices

---

### g) System / Test Endpoints

| Method | Endpoint | Description |
|--------|---------|-------------|
| GET | `/api/health` | Health check: database connection, API key configuration, platform key status |
| GET | `/api/test/extract-title` | Test short title extraction logic on sample titles |

---

###  Progress We Made

- **Multi-platform price integration**
  - RapidAPI integration for BestBuy and Walmart, in addition to Amazon
  - Multiple price records per product (price, free_shipping, in_stock, link)

- **Smarter Product Modeling**
  - `extractShortTitle()` generates concise, user-friendly short titles
  - Title similarity logic chooses best Walmart match from multiple results

- **Richer Admin Workflows**
  - `/api/admin/import-initial` imports products and attaches multi-platform prices
  - `/api/admin/update-all-prices` refreshes all existing products’ prices
  - `/api/admin/sync-lowest-prices` centralizes cheapest platform calculation

- **Wishlist Support**
  - New `/api/wishlist` and `/api/wishlist/alerts` endpoints
  - Alerts endpoint encapsulates logic for notifications

- **Daily Scheduled Multi-Platform Update**
  - Cron job fetches fresh prices from all platforms
  - Automatically syncs products table to the newest lowest prices



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


## Repo organization
```
├── MainActivity.kt
├── data
│   ├── local
│   │   └── UserPreferences.kt
│   └── remote
│       ├── api
│       │   ├── DatabaseApiService.kt
│       │   ├── PriceApi.kt
│       │   ├── UserApi.kt
│       │   ├── WishListApi.kt
│       │   └── Wishlistapiservice.kt
│       ├── dto
│       │   ├── HistoryPriceDto.kt
│       │   ├── PriceDto.kt
│       │   └── ProductDto.kt
│       └── repository
│           ├── PriceRepositoryImpl.kt
│           ├── ProductRepositoryImpl.kt
│           ├── RetrofitClient.kt
│           ├── UserRepository.kt
│           └── Wishlistrepository.kt
├── domain
│   ├── UserManager.kt
│   ├── model
│   │   ├── Category.kt
│   │   ├── Platform.kt
│   │   ├── PlatformPrice.kt
│   │   ├── PricePoint.kt
│   │   ├── Product.kt
│   │   └── User.kt
│   └── repository
│       ├── PriceRepository.kt
│       └── ProductRepository.kt
└── ui
├── deals
│   ├── DealsScreen.kt
│   └── viewmodel
│       ├── DealsViewModel.kt
│       ├── SortField.kt
│       └── SortOrder.kt
├── detail
│   ├── ProductDetailScreen.kt
│   └── viewmodel
│       ├── HistoryUiState.kt
│       └── ProductViewModel.kt
├── home
│   ├── HomeScreen.kt
│   └── viewmodel
│       └── HomeViewModel.kt
├── navigation
│   ├── BottomNavBar.kt
│   ├── MainNavGraph.kt
│   ├── NavExtensions.kt
│   └── Routes.kt
├── notifications
│   └── NotificationHelper.kt
├── profile
│   ├── AuthViewModel.kt
│   ├── EditProfileScreen.kt
│   ├── HistoryScreen.kt
│   ├── LoginScreen.kt
│   ├── ProfileScreen.kt
│   ├── RegisterScreen.kt
│   └── SettingScreen.kt
├── theme
│   ├── AppColors.kt
│   ├── AppDimens.kt
│   ├── Color.kt
│   ├── Theme.kt
│   └── Type.kt
└── wishlist
├── WishListHolder.kt
├── WishListScreen.kt
└── viewmodel
├── WishListModel.kt
└── WishListViewModel.kt
```
## commit, code quality
- all commits of these days are under branch "demo2"

- Follow project coding standards and use linting/formatting tools (e.g., Kotlin style guidelines).

- Avoid duplicated logic (abort flask backend, merge 2 backends).

- Use defensive programming: handle errors, timeouts, and nullability safely.


## AI use
- AI tools are be used to speed up development
- All AI-generated code are reviewed, tested, and verified by the developer.
