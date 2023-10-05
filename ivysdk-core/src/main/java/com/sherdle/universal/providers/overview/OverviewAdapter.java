package com.sherdle.universal.providers.overview;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.adsfall.R;
import com.sherdle.universal.drawer.NavItem;
import com.sherdle.universal.util.Helper;
import com.sherdle.universal.util.InfiniteRecyclerViewAdapter;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * This file is part of the Universal template
 * For license information, please check the LICENSE
 * file in the root of this project
 *
 * @author Sherdle
 * Copyright 2019
 */

public class OverviewAdapter extends InfiniteRecyclerViewAdapter {
  private static final int TEXT_TYPE = 0;
  private static final int IMAGE_TYPE = 1;

  private final List<NavItem> data;
  private final Context context;
  private final OnOverViewClick callback;

  private int number;

  public OverviewAdapter(List<NavItem> data, Context context, OnOverViewClick click) {
    super(context, null);
    this.data = data;
    this.context = context;
    this.callback = click;
  }

  @Override
  protected int getViewType(int position) {
    if (position >= 0 && position < data.size()) {
      if (data.get(position).getCategoryImageUrl() != null && !data.get(position).getCategoryImageUrl().isEmpty())
        return IMAGE_TYPE;
      else
        return TEXT_TYPE;
    }
    return super.getItemViewType(position);
  }

  @Override
  protected RecyclerView.ViewHolder getViewHolder(ViewGroup parent, int viewType) {
    View itemView;
    if (viewType == TEXT_TYPE)
      return new TextViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_overview_card_text, parent, false));
    else if (viewType == IMAGE_TYPE)
      return new ImageViewHolder(LayoutInflater.from(parent.getContext())
        .inflate(R.layout.fragment_overview_card_image, parent, false));

    return null;
  }

  @Override
  protected void doBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
    holder.itemView.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        callback.onOverViewSelected(data.get(holder.getAdapterPosition()));
      }
    });

    if (holder instanceof TextViewHolder) {

      ((TextViewHolder) holder).title.setText(data.get(position).getText(context));
      ((TextViewHolder) holder).background.setBackgroundResource(randomGradientResource());

    } else if (holder instanceof ImageViewHolder) {

      Picasso.get()
        .load(data.get(position).getCategoryImageUrl())
        .placeholder(R.color.black_more_translucent)
        .into(((ImageViewHolder) holder).image);
      ((ImageViewHolder) holder).title.setText(data.get(position).getText(context));
    }
  }

  @Override
  protected int getCount() {
    return data.size();
  }

  private class ImageViewHolder extends RecyclerView.ViewHolder {
    public TextView title;
    public ImageView image;

    public View itemView;

    private ImageViewHolder(View itemView) {
      super(itemView);
      this.itemView = itemView;

      title = itemView.findViewById(R.id.title);
      image = itemView.findViewById(R.id.image);
    }
  }

  private class TextViewHolder extends RecyclerView.ViewHolder {
    public TextView title;
    public View background;

    public View itemView;

    private TextViewHolder(View itemView) {
      super(itemView);
      this.itemView = itemView;

      background = itemView.findViewById(R.id.background);
      title = itemView.findViewById(R.id.title);
    }
  }

  private int randomGradientResource() {
    number += 1;
    if (number == 6) number = 1;

    return Helper.getGradient(number);
  }

  public interface OnOverViewClick {
    void onOverViewSelected(NavItem item);
  }


}