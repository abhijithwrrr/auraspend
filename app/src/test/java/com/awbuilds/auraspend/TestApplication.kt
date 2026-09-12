package com.awbuilds.auraspend

import android.app.Application

/**
 * Plain application for JVM (Robolectric) tests.
 *
 * Using this instead of [AuraSpendApp] keeps WorkManager, Room and the LLM
 * runtime out of unit tests — screenshot tests only need real resources.
 */
class TestApplication : Application()
