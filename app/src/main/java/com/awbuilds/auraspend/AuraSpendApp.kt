package com.awbuilds.auraspend

import android.app.Application
import com.awbuilds.auraspend.data.classification.AutoClassificationWorker
import com.awbuilds.auraspend.data.classification.defaultCategories
import com.awbuilds.auraspend.data.local.AppDatabase
import com.awbuilds.auraspend.data.local.entities.CategoryEntity
import com.awbuilds.auraspend.data.repository.TransactionRepositoryImpl
import com.awbuilds.auraspend.domain.repository.TransactionRepository
import com.awbuilds.auraspend.domain.usecase.ClassifyMessageUseCase
import com.awbuilds.auraspend.domain.usecase.SaveTransactionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AuraSpendApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var transactionRepository: TransactionRepository
        private set

    lateinit var classifyMessageUseCase: ClassifyMessageUseCase
        private set

    lateinit var saveTransactionUseCase: SaveTransactionUseCase
        private set

    lateinit var driveSyncManager: com.awbuilds.auraspend.data.remote.DriveSyncManager
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)

        transactionRepository = TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            subscriptionDao = database.subscriptionDao()
        )

        classifyMessageUseCase = ClassifyMessageUseCase()
        saveTransactionUseCase = SaveTransactionUseCase(transactionRepository)

        driveSyncManager = com.awbuilds.auraspend.data.remote.DriveSyncManager(this)

        AutoClassificationWorker.schedule(this)

        applicationScope.launch {
            seedDefaultCategories()
        }
    }

    private suspend fun seedDefaultCategories() {
        val existing = database.categoryDao().getAllCategories().first()
        if (existing.isEmpty()) {
            defaultCategories.forEach { category ->
                database.categoryDao().insertCategory(
                    CategoryEntity(
                        id = category.id,
                        name = category.name,
                        icon = category.icon,
                        color = category.color,
                        isDefault = category.isDefault
                    )
                )
            }
        }
    }
}
