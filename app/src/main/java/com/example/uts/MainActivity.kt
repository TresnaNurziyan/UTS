package com.example.uts

import android.app.*
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.uts.db.TaskDbHelper


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"
    private lateinit var mHelper: TaskDbHelper
    private lateinit var mTaskListView: ListView
    private var mAdapter: ArrayAdapter<String>? = null
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder : Notification.Builder
    private val channelId = "com.example.notificationapp"
    private val description = "Test Notification"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mTaskListView = findViewById(R.id.list_todo)
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mHelper = TaskDbHelper(this)
        updateUI()
    }

    fun deleteTask(view: View) {
        val parent = view.getParent() as View
        val taskTextView = parent.findViewById<TextView>(R.id.task_title)
        val task = taskTextView.text.toString()
        val db = mHelper.writableDatabase
        db.delete(TaskContract.TaskEntry.TABLE,
                TaskContract.TaskEntry.COL_TASK_TITLE + " = ?",
                arrayOf(task))
        db.close()
        updateUI()

        val intent = Intent(this, LauncherActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this,0,intent, PendingIntent.FLAG_UPDATE_CURRENT)

        val contentView = RemoteViews(packageName,R.layout.notification_layout)
        contentView.setTextViewText(R.id.notif_title, "UTS")
        contentView.setTextViewText(R.id.notif_content, R.id.task_title.toString() + "is  already done!")

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            notificationChannel = NotificationChannel(channelId,description,NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.GREEN
            notificationChannel.enableVibration(false)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this,channelId)
                    .setContent(contentView)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
        } else {
            builder = Notification.Builder(this)
                    .setContent(contentView)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setLargeIcon(BitmapFactory.decodeResource(this.resources,R.mipmap.ic_launcher))
                    .setContentIntent(pendingIntent)
        }
        notificationManager.notify(1234,builder.build())
    }

    private fun updateUI() {
        val taskList = ArrayList<String>()
        val db = mHelper.readableDatabase
        val cursor = db.query(TaskContract.TaskEntry.TABLE,
                arrayOf(TaskContract.TaskEntry._ID, TaskContract.TaskEntry.COL_TASK_TITLE), null, null, null, null, null)
        while (cursor.moveToNext()) {
            val idx = cursor.getColumnIndex(TaskContract.TaskEntry.COL_TASK_TITLE)
            taskList.add(cursor.getString(idx))
        }

        if (mAdapter == null) {
            mAdapter = ArrayAdapter(this,
                    R.layout.item_todo,
                    R.id.task_title,
                    taskList)
            mTaskListView.adapter = mAdapter
        } else {
            mAdapter?.clear()
            mAdapter?.addAll(taskList)
            mAdapter?.notifyDataSetChanged()
        }

        cursor.close()
        db.close()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.action_add_task -> {
                val taskEditText = EditText(this)
                val dialog = AlertDialog.Builder(this)
                        .setTitle("Add a new task")
                        .setMessage("What do you want to do next?")
                        .setView(taskEditText)
                        .setPositiveButton("Add", DialogInterface.OnClickListener { dialog, which ->
                            val task = taskEditText.text.toString()
                            val db = mHelper.getWritableDatabase()
                            val values = ContentValues()
                            values.put(TaskContract.TaskEntry.COL_TASK_TITLE, task)
                            db.insertWithOnConflict(TaskContract.TaskEntry.TABLE,
                                    null,
                                    values,
                                    SQLiteDatabase.CONFLICT_REPLACE)
                            db.close()

                            Log.d(TAG, "Task to add: " + task)
                            updateUI()
                        })
                        .setNegativeButton("Cancel", null)
                        .create()
                dialog.show()
                return true
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }
}