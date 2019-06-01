package com.plweegie.android.rpibleweather

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue


class TemperatureSyncWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val database = FirebaseDatabase.getInstance()

    companion object {
        const val TEMPERATURE_DATA_ID = "temperature_data"
    }

    override fun doWork(): ListenableWorker.Result {
        var result = ListenableWorker.Result.retry()
        val dbReference = database.getReference("temperature").push()
        val input = inputData.getFloat(TEMPERATURE_DATA_ID, 0.0f)

        if (input != 0.0f) {
            dbReference.child("timestamp").setValue(ServerValue.TIMESTAMP)
            dbReference.child("reading").setValue(input).addOnCompleteListener {
                result = ListenableWorker.Result.success()
            }.addOnFailureListener {
                result = ListenableWorker.Result.failure()
            }
        }

        return result
    }
}