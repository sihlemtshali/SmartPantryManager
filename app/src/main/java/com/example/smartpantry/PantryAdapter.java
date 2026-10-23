package com.example.smartpantry;

import android.content.Context;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import com.example.smartpantry.core.Ingredient;
import java.time.LocalDate;
import java.util.*;

public final class PantryAdapter extends BaseAdapter {
  private final Context context;
  private final List<Ingredient> items;
  private final boolean alerts;

  public PantryAdapter(Context context, List<Ingredient> items, boolean alerts) {
    this.context = context;
    this.items = items;
    this.alerts = alerts;
  }

  @Override
  public int getCount() {
    return items.size();
  }

  @Override
  public Ingredient getItem(int position) {
    return items.get(position);
  }

  @Override
  public long getItemId(int position) {
    return getItem(position).id;
  }

  @Override
  public boolean hasStableIds() {
    return true;
  }

  private static final class Holder {
    TextView name, amount, expiry;

    Holder(View v) {
      name = v.findViewById(R.id.row_name);
      amount = v.findViewById(R.id.row_amount);
      expiry = v.findViewById(R.id.row_expiry);
    }
  }

  @Override
  public View getView(int position, View recycled, ViewGroup parent) {
    // Reuse row views; resetting every field prevents recycled expiry colours leaking.
    if (recycled == null) {
      recycled = LayoutInflater.from(context).inflate(R.layout.row_pantry, parent, false);
      recycled.setTag(new Holder(recycled));
    }
    Holder h = (Holder) recycled.getTag();
    Ingredient item = getItem(position);
    LocalDate today = LocalDate.now();
    h.name.setText(item.name);
    h.amount.setText(item.amount());
    h.expiry.setTextColor(Color.parseColor("#607060"));
    if (item.expired(today)) {
      h.expiry.setText("Expired " + item.expiry + " · excluded from matching");
      h.expiry.setTextColor(Color.parseColor("#A23333"));
    } else if (item.expiry == null) h.expiry.setText("No expiry date · tap to edit");
    else {
      h.expiry.setText("Expires " + item.expiry + " · tap to edit");
      if (alerts && !item.expiry.isAfter(today.plusDays(3)))
        h.expiry.setTextColor(Color.parseColor("#9A5B13"));
    }
    return recycled;
  }
}
