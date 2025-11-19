package com.pocketminder.adapter;

import android.content.Context;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pocketminder.R;
import com.pocketminder.model.ShoppingItem;

import java.util.List;

/**
 * Adapter for shopping list ListView
 */
public class ShoppingListAdapter extends ArrayAdapter<ShoppingItem> {

    private final Context context;
    private final List<ShoppingItem> items;
    private final OnItemCheckedListener checkedListener;
    private final OnItemDeletedListener deletedListener;

    public interface OnItemCheckedListener {
        void onItemChecked(ShoppingItem item, boolean isChecked);
    }

    public interface OnItemDeletedListener {
        void onItemDeleted(ShoppingItem item);
    }

    public ShoppingListAdapter(@NonNull Context context, @NonNull List<ShoppingItem> items,
                               OnItemCheckedListener checkedListener,
                               OnItemDeletedListener deletedListener) {
        super(context, 0, items);
        this.context = context;
        this.items = items;
        this.checkedListener = checkedListener;
        this.deletedListener = deletedListener;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.item_shopping_list, parent, false);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.checkBoxItem);
            holder.tvItemName = convertView.findViewById(R.id.tvItemName);
            holder.tvQuantity = convertView.findViewById(R.id.tvQuantity);
            holder.btnDelete = convertView.findViewById(R.id.btnDelete);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        ShoppingItem item = items.get(position);

        // Set item name
        holder.tvItemName.setText(item.getItemName());

        // Set quantity
        if (item.getQuantity() > 1) {
            holder.tvQuantity.setText("x" + item.getQuantity());
            holder.tvQuantity.setVisibility(View.VISIBLE);
        } else {
            holder.tvQuantity.setVisibility(View.GONE);
        }

        // Set checkbox state
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(item.isPurchased());

        // Apply strikethrough if purchased
        if (item.isPurchased()) {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvItemName.setAlpha(0.5f);
        } else {
            holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvItemName.setAlpha(1.0f);
        }

        // Set checkbox listener
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (checkedListener != null) {
                checkedListener.onItemChecked(item, isChecked);
            }

            // Update UI
            if (isChecked) {
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvItemName.setAlpha(0.5f);
            } else {
                holder.tvItemName.setPaintFlags(holder.tvItemName.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
                holder.tvItemName.setAlpha(1.0f);
            }
        });

        // Set delete button listener
        holder.btnDelete.setOnClickListener(v -> {
            if (deletedListener != null) {
                deletedListener.onItemDeleted(item);
            }
        });

        return convertView;
    }

    private static class ViewHolder {
        CheckBox checkBox;
        TextView tvItemName;
        TextView tvQuantity;
        ImageButton btnDelete;
    }
}
