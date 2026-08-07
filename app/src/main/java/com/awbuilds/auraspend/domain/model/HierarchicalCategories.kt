package com.awbuilds.auraspend.domain.model

/**
 * Hierarchical category structure for improved organization
 * Example: Food -> Restaurant, Grocery, Cafe
 */
val hierarchicalCategoryMap = mapOf(
    "Food & Dining" to listOf(
        "Food Delivery",
        "Restaurants",
        "Groceries",
        "Cafes & Bakery",
        "Fast Food",
        "Meal Subscriptions"
    ),
    "Transport" to listOf(
        "Auto Fuel",
        "Cab Services",
        "Public Transit",
        "Auto Rickshaw",
        "Parking",
        "Tolls",
        "Flights",
        "Trains",
        "Buses"
    ),
    "Shopping" to listOf(
        "E-commerce",
        "Fashion Retail",
        "Beauty & Cosmetics",
        "Electronics",
        "Home & Furniture",
        "Sports & Outdoors",
        "Jewelry",
        "Books"
    ),
    "Bills & Utilities" to listOf(
        "Electricity Bill",
        "Water Bill",
        "Gas Bill",
        "Internet Bill",
        "Mobile Recharge",
        "DTH/TV",
        "Rent"
    ),
    "Entertainment" to listOf(
        "Streaming Video",
        "Streaming Audio/Music",
        "Gaming",
        "Movies & Events",
        "Sports"
    ),
    "Healthcare" to listOf(
        "Medical Consultations",
        "Pharmacy",
        "Gym & Fitness",
        "Dental",
        "Diagnostics",
        "Mental Health"
    ),
    "Education" to listOf(
        "Online Courses",
        "Tuitions",
        "Books & Materials",
        "College/School Fees",
        "Professional Certifications"
    ),
    "Subscriptions" to listOf(
        "SaaS & Software",
        "Cloud Services",
        "Memberships",
        "Premium Services"
    ),
    "Travel & Accommodation" to listOf(
        "Hotels & Stays",
        "Flight Bookings",
        "Train Tickets",
        "Bus Tickets",
        "Tour Packages"
    ),
    "Personal Care" to listOf(
        "Haircut & Salon",
        "Skincare",
        "Personal Grooming"
    ),
    "Transfers" to listOf(
        "Bank Transfers",
        "UPI Transfers",
        "Peer-to-Peer",
        "Loan Repayment"
    ),
    "Salary & Income" to listOf(
        "Monthly Salary",
        "Bonus",
        "Freelance Income",
        "Investment Returns"
    )
)
