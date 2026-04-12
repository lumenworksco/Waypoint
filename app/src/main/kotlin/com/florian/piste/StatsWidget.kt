package com.florian.piste

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

/**
 * Home-screen widget showing today's runs and vertical descended.
 * Updates every 30 minutes (system) and on app launch.
 */
class StatsWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidget(context, manager, id) }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = android.content.ComponentName(context, StatsWidget::class.java)
            val ids = manager.getAppWidgetIds(component)
            ids.forEach { id -> updateWidget(context, manager, id) }
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, id: Int) {
            val store = WaypointStore(context)
            val tracks = store.loadTracks()
            val imperial = store.loadSetting("distance_unit", "METRIC") == "IMPERIAL"
            val widgetMode = store.loadSetting("widget_mode", "Today")

            var runs = 0
            var vert = 0.0
            val source = if (widgetMode == "Season") tracks else {
                val dayStart = startOfDay()
                tracks.filter { it.startTime >= dayStart }
            }
            for (t in source) {
                val s = computeTrackStatsCached(t)
                runs += s.runCount
                vert += s.verticalDescended ?: 0.0
            }

            val views = RemoteViews(context.packageName, R.layout.stats_widget)
            views.setTextViewText(R.id.widget_runs, runs.toString())
            views.setTextViewText(R.id.widget_vertical, formatVertical(vert, imperial))

            // Tap → open the app
            val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            if (launch != null) {
                val pending = PendingIntent.getActivity(
                    context, 0, launch,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                views.setOnClickPendingIntent(R.id.widget_root, pending)
            }

            manager.updateAppWidget(id, views)
        }
    }
}
