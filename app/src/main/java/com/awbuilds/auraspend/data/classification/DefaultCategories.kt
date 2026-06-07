package com.awbuilds.auraspend.data.classification

import com.awbuilds.auraspend.domain.model.Category
import com.awbuilds.auraspend.ui.theme.*

val defaultCategories = listOf(
    Category(id = "cat_food", name = "Food & Dining", icon = "restaurant", color = 0xFFE53935.toInt(), isDefault = true),
    Category(id = "cat_transport", name = "Transport", icon = "directions_car", color = 0xFF1E88E5.toInt(), isDefault = true),
    Category(id = "cat_shopping", name = "Shopping", icon = "shopping_bag", color = 0xFF8E24AA.toInt(), isDefault = true),
    Category(id = "cat_bills", name = "Bills & Utilities", icon = "receipt_long", color = 0xFFFF8F00.toInt(), isDefault = true),
    Category(id = "cat_entertainment", name = "Entertainment", icon = "movie", color = 0xFF00ACC1.toInt(), isDefault = true),
    Category(id = "cat_healthcare", name = "Healthcare", icon = "local_hospital", color = 0xFFE53935.toInt(), isDefault = true),
    Category(id = "cat_education", name = "Education", icon = "school", color = 0xFF5E35B1.toInt(), isDefault = true),
    Category(id = "cat_salary", name = "Salary", icon = "account_balance", color = 0xFF43A047.toInt(), isDefault = true),
    Category(id = "cat_subscription", name = "Subscriptions", icon = "subscriptions", color = 0xFF6D4C41.toInt(), isDefault = true),
    Category(id = "cat_transfer", name = "Transfer", icon = "swap_horiz", color = 0xFF546E7A.toInt(), isDefault = true),
    Category(id = "cat_grocery", name = "Grocery", icon = "local_grocery_store", color = 0xFF2E7D32.toInt(), isDefault = true),
    Category(id = "cat_other", name = "Other", icon = "category", color = 0xFF757575.toInt(), isDefault = true)
)

val categoryKeywordMap: Map<String, String> = mapOf(
    // Food & Dining
    "swiggy" to "cat_food",
    "zomato" to "cat_food",
    "dominos" to "cat_food",
    "pizza hut" to "cat_food",
    "mcdonald" to "cat_food",
    "kfc" to "cat_food",
    "burger king" to "cat_food",
    "subway" to "cat_food",
    "dining" to "cat_food",
    "restaurant" to "cat_food",
    "cafe" to "cat_food",
    "food" to "cat_food",
    "eat" to "cat_food",
    "hotel" to "cat_food",
    "dhaba" to "cat_food",
    "tiffin" to "cat_food",
    "lunch" to "cat_food",
    "dinner" to "cat_food",
    "breakfast" to "cat_food",
    "zepto" to "cat_grocery",
    "blinkit" to "cat_grocery",
    "instamart" to "cat_grocery",
    "grocery" to "cat_grocery",
    "bigbasket" to "cat_grocery",
    "vegetable" to "cat_grocery",
    "milk" to "cat_grocery",
    "provision" to "cat_grocery",

    // Transport
    "uber" to "cat_transport",
    "ola" to "cat_transport",
    "rapido" to "cat_transport",
    "metro" to "cat_transport",
    "petrol" to "cat_transport",
    "fuel" to "cat_transport",
    "indian oil" to "cat_transport",
    "bharat petroleum" to "cat_transport",
    "hp petrol" to "cat_transport",
    "bus" to "cat_transport",
    "taxi" to "cat_transport",
    "cab" to "cat_transport",
    "parking" to "cat_transport",
    "toll" to "cat_transport",
    "railway" to "cat_transport",
    "irctc" to "cat_transport",
    "flight" to "cat_transport",
    "goibibo" to "cat_transport",
    "makemytrip" to "cat_transport",

    // Shopping
    "amazon" to "cat_shopping",
    "flipkart" to "cat_shopping",
    "myntra" to "cat_shopping",
    "ajio" to "cat_shopping",
    "meesho" to "cat_shopping",
    "nykaa" to "cat_shopping",
    "tatacliq" to "cat_shopping",
    "shopping" to "cat_shopping",
    "mall" to "cat_shopping",
    "clothing" to "cat_shopping",
    "electronics" to "cat_shopping",

    // Bills & Utilities
    "electricity" to "cat_bills",
    "water" to "cat_bills",
    "gas" to "cat_bills",
    "broadband" to "cat_bills",
    "wifi" to "cat_bills",
    "recharge" to "cat_bills",
    "mobile" to "cat_bills",
    "airtel" to "cat_bills",
    "jio" to "cat_bills",
    "vi" to "cat_bills",
    "bill" to "cat_bills",
    "rent" to "cat_bills",
    "maintenance" to "cat_bills",

    // Entertainment
    "netflix" to "cat_entertainment",
    "prime video" to "cat_entertainment",
    "amazon prime" to "cat_entertainment",
    "hotstar" to "cat_entertainment",
    "disney" to "cat_entertainment",
    "sony liv" to "cat_entertainment",
    "zee5" to "cat_entertainment",
    "youtube" to "cat_entertainment",
    "spotify" to "cat_entertainment",
    "gaana" to "cat_entertainment",
    "bookmyshow" to "cat_entertainment",
    "movie" to "cat_entertainment",
    "cinema" to "cat_entertainment",
    "games" to "cat_entertainment",
    "playstation" to "cat_entertainment",

    // Healthcare
    "hospital" to "cat_healthcare",
    "doctor" to "cat_healthcare",
    "clinic" to "cat_healthcare",
    "pharmacy" to "cat_healthcare",
    "medical" to "cat_healthcare",
    "medicine" to "cat_healthcare",
    "health" to "cat_healthcare",
    "dentist" to "cat_healthcare",
    "diagnostic" to "cat_healthcare",
    "practo" to "cat_healthcare",
    "1mg" to "cat_healthcare",
    "apollo" to "cat_healthcare",

    // Education
    "udemy" to "cat_education",
    "coursera" to "cat_education",
    "unacademy" to "cat_education",
    "byju" to "cat_education",
    "vedantu" to "cat_education",
    "college" to "cat_education",
    "school" to "cat_education",
    "tuition" to "cat_education",
    "fee" to "cat_education",
    "course" to "cat_education",
    "book" to "cat_education",

    // Salary
    "salary" to "cat_salary",
    "credit" to "cat_salary",
    "payroll" to "cat_salary",
    "wages" to "cat_salary",
    "income" to "cat_salary",

    // Subscriptions
    "subscription" to "cat_subscription",
    "renew" to "cat_subscription",
    "icloud" to "cat_subscription",
    "google one" to "cat_subscription",
    "dropbox" to "cat_subscription",
    "office 365" to "cat_subscription",

    // Transfer
    "transfer" to "cat_transfer",
    "neft" to "cat_transfer",
    "imps" to "cat_transfer",
    "rtgs" to "cat_transfer",
    "upi" to "cat_transfer",
    "to self" to "cat_transfer"
)

fun getCategoryIdForKeyword(input: String): String? {
    val lower = input.lowercase()
    return categoryKeywordMap.entries.firstOrNull { (key, _) ->
        lower.contains(key)
    }?.value
}
