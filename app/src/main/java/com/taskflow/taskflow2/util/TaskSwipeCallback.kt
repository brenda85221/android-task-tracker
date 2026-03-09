package com.taskflow.taskflow2.util

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.taskflow.taskflow2.R

class TaskSwipeCallback(
    private val onDelete: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int
    ) {
        onDelete(viewHolder.adapterPosition)
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {

        val itemView = viewHolder.itemView

        val paint = Paint()
        paint.color = Color.parseColor("#A1A9A9")//灰色

        val background = RectF(
            itemView.right + dX,
            itemView.top.toFloat(),
            itemView.right.toFloat(),
            itemView.bottom.toFloat()
        )

        c.drawRect(background, paint)

        val icon = ContextCompat.getDrawable(
            recyclerView.context,
            R.drawable.ic_delete
        )

        icon?.let {

            val margin = (itemView.height - it.intrinsicHeight) / 2

            val left = itemView.right - margin - it.intrinsicWidth
            val right = itemView.right - margin
            val top = itemView.top + margin
            val bottom = top + it.intrinsicHeight

            it.setBounds(left, top, right, bottom)
            it.draw(c)
        }

        super.onChildDraw(
            c,
            recyclerView,
            viewHolder,
            dX,
            dY,
            actionState,
            isCurrentlyActive
        )
    }
}