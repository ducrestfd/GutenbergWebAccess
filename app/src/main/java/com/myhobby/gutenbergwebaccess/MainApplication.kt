package com.myhobby.gutenbergwebaccess

/*
Gutenberg Access's raison d'être is to provide simple access to
the Gutenberg Project website of 70,000 plus books to both
sighted and blind users.  It is provided without charge under the
agpl-3.0 license.

    Copyright (C) 2026 Frank D. Ducrest

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published
by the Free Software Foundation, either version 3 of the License, or
any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import android.app.Application
import androidx.room.Room
import com.myhobby.gutenbergwebaccess.data.databases.AudioLocationDatabase
import com.myhobby.gutenbergwebaccess.data.databases.ScrollLocationDatabase


/**
 * Custom [Application] class for the Gutenberg Access application.
 *
 * This class is instantiated when the application process is created. It is used here to
 * initialize and provide application-wide singleton instances of Room databases:
 * [ScrollLocationDatabase] and [AudioLocationDatabase]. These databases are accessible
 * statically via the `companion object`.
 *
 * The databases are built during the `onCreate` lifecycle method of the application.
 *
 * To use this custom Application class, it must be declared in the `android:name`
 * attribute of the `<application>` tag in the `AndroidManifest.xml` file.
 * Example: `<application android:name=".MainApplication" ... >`
 */
class MainApplication : Application() {

    /**
     * Companion object to hold static instances of the databases.
     *
     * This allows other parts of the application (like ViewModels or Repositories)
     * to access the database instances without needing a direct reference to the
     * Application context or performing dependency injection for these specific singletons.
     */
    companion object {

        /**
         * Singleton instance of [ScrollLocationDatabase].
         * It is initialized in `onCreate()` of the [MainApplication].
         * This database stores the scroll positions for books.
         */
        lateinit var scrollLocationDatabase : ScrollLocationDatabase

        /**
         * Singleton instance of [AudioLocationDatabase].
         * It is initialized in `onCreate()` of the [MainApplication].
         * This database stores the audio playback (sentence index) positions for books.
         */
        lateinit var audioLocationDatabase : AudioLocationDatabase



    }

    /**
     * Called when the application is starting, before any other objects have been created.
     *
     * This method initializes the [scrollLocationDatabase] and [audioLocationDatabase]
     * Room database instances. These instances are then available throughout the application
     * via the `companion object`.
     */
    override fun onCreate() {
        super.onCreate()

        // Initialize the ScrollLocationDatabase.
        // `applicationContext` is used to ensure the database lives as long as the application.
        // `ScrollLocationDatabase.NAME` is a constant string defining the database file name.
        scrollLocationDatabase = Room.databaseBuilder(
            applicationContext,
            ScrollLocationDatabase::class.java,
            ScrollLocationDatabase.NAME
        )
            .fallbackToDestructiveMigration()
            .build()

        // Initialize the AudioLocationDatabase.
        // `AudioLocationDatabase.NAME` is a constant string defining the database file name.
        audioLocationDatabase = Room.databaseBuilder(
            applicationContext,
            AudioLocationDatabase::class.java,
            AudioLocationDatabase.NAME
        )
            .fallbackToDestructiveMigration()
            .build()
    }

}