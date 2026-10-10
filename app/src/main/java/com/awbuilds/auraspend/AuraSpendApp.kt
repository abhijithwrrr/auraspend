package com.awbuilds.auraspend

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.awbuilds.auraspend.data.classification.AutoClassificationWorker
import com.awbuilds.auraspend.data.ai.LocalLlmProvider
import com.awbuilds.auraspend.data.ai.ModelDownloadManager
import com.awbuilds.auraspend.data.classification.BackgroundClassificationCoordinator
import com.awbuilds.auraspend.data.classification.ClassificationMemory
import com.awbuilds.auraspend.data.privacy.PrivacyBackfill
import com.awbuilds.auraspend.data.classification.defaultCategories
import com.awbuilds.auraspend.data.classification.MerchantRepository
import com.awbuilds.auraspend.data.local.AppDatabase
import com.awbuilds.auraspend.data.local.BackupCreateManager
import com.awbuilds.auraspend.data.local.BackupRestoreManager
import com.awbuilds.auraspend.data.local.UnrecognizedSmsRepository
import com.awbuilds.auraspend.data.local.entities.CategoryEntity
import com.awbuilds.auraspend.data.remote.DriveSyncManager
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

    lateinit var driveSyncManager: DriveSyncManager
        private set

    /** Applies a Drive backup atomically (see [BackupRestoreManager]). */
    lateinit var backupRestoreManager: BackupRestoreManager
        private set

    /** Builds the JSON payload a Drive backup uploads (see [BackupCreateManager]). */
    lateinit var backupCreateManager: BackupCreateManager
        private set

    /** Shared learned-classification store (merchant -> category) used by the AI pipeline. */
    lateinit var classificationMemory: ClassificationMemory
        private set

    /** Bank messages no parser could read, surfaced rather than silently dropped. */
    lateinit var unrecognizedSms: UnrecognizedSmsRepository
        private set

    private val applicationScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)

        transactionRepository = TransactionRepositoryImpl(
            transactionDao = database.transactionDao(),
            categoryDao = database.categoryDao(),
            budgetDao = database.budgetDao(),
            subscriptionDao = database.subscriptionDao(),
            savingsGoalDao = database.savingsGoalDao()
        )

        classifyMessageUseCase = ClassifyMessageUseCase()
        saveTransactionUseCase = SaveTransactionUseCase(transactionRepository)

        classificationMemory = ClassificationMemory(database.classificationMemoryDao())
        unrecognizedSms = UnrecognizedSmsRepository(database.unrecognizedSmsDao())

        driveSyncManager = DriveSyncManager(this)
        backupRestoreManager = BackupRestoreManager(database)
        backupCreateManager = BackupCreateManager(database)

        LocalLlmProvider.init(this)

        // Startup performance: nothing below blocks the first frame. The model
        // status sync, worker scheduling, merchant CSV parsing and seeding all
        // run on IO; the UI reads defaults until they settle.
        applicationScope.launch {
            ModelDownloadManager.sync(this@AuraSpendApp)
            AutoClassificationWorker.schedule(this@AuraSpendApp)
        }

        // Startup performance: the merchant CSV (~assets I/O + parsing) never blocks the
        // first frame. Keyword classification works without it; the merchant-database
        // upgrade lands within milliseconds on a background thread.
        applicationScope.launch {
            MerchantRepository.initialize(this@AuraSpendApp)
        }
        applicationScope.launch {
            seedDefaultCategories()
        }

        // Background auto-classification: while the app sits in the background but its process
        // is alive (not cleared from recents), keep classifying pending queue rows one-by-one.
        // The moment any activity becomes visible, the loop stops so the device stays smooth.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                BackgroundClassificationCoordinator.stop()
            }

            override fun onStop(owner: LifecycleOwner) {
                BackgroundClassificationCoordinator.start(this@AuraSpendApp)
            }
        })

        // One-time PII scrub of rows written before SensitiveDataMasker existed.
        applicationScope.launch {
            PrivacyBackfill.runIfNeeded(this@AuraSpendApp)
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
