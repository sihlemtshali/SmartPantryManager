package com.example.smartpantry;

import android.app.Application;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps one database helper and one serial database queue alive for the lifetime of the app
 * process.
 */
public final class PantryApp extends Application {
  public final ExecutorService databaseQueue = Executors.newSingleThreadExecutor();
  public DatabaseHelper database;

  @Override
  public void onCreate() {
    super.onCreate();
    database = new DatabaseHelper(this);
  }
}
