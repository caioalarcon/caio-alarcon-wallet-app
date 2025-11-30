package com.example.carteiradepagamentos.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.carteiradepagamentos.domain.model.Contact
import com.example.carteiradepagamentos.domain.service.Notifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val TRANSFER_CHANNEL_ID = "transfers"

@Singleton
class AndroidNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : Notifier {

    override fun notifyTransferSuccess(contact: Contact, amountInCents: Long) {
        createNotificationChannelIfNeeded()

        val reais = amountInCents / 100
        val cents = amountInCents % 100
        val amountText = "R$ %d,%02d".format(reais, cents)

        val builder = NotificationCompat.Builder(context, TRANSFER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Transferência realizada")
            .setContentText("Você enviou %s para %s".format(amountText, contact.name))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        with(NotificationManagerCompat.from(context)) {
            notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), builder.build())
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Transferências"
            val descriptionText = "Notificações de transferências realizadas"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(TRANSFER_CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
