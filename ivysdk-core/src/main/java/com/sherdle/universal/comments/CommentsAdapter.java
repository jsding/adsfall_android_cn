package com.sherdle.universal.comments;

import android.content.Context;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import com.adsfall.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class CommentsAdapter extends ArrayAdapter<Comment> {

  private final Context context;
  private final int type;

  public CommentsAdapter(Context context, List<Comment> objects, int type) {
    super(context, 0, objects);
    this.context = context;
    this.type = type;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    final Comment comment = getItem(position);
    CommentViewHolder viewHolder;

    if (convertView == null) {
      convertView = LayoutInflater.from(getContext()).inflate(R.layout.activity_comments_row, parent, false);
      viewHolder = new CommentViewHolder();
      viewHolder.ivProfilePhoto = convertView.findViewById(R.id.ivProfilePhoto);
      viewHolder.tvUsername = convertView.findViewById(R.id.tvUsername);
      viewHolder.tvComment = convertView.findViewById(R.id.tvComment);
      viewHolder.ratingBar = convertView.findViewById(R.id.rating);
      convertView.setTag(viewHolder);
    } else {
      viewHolder = (CommentViewHolder) convertView.getTag();
    }

    viewHolder.ivProfilePhoto.setImageDrawable(null);
    if (comment.profileUrl != null && !comment.profileUrl.isEmpty()) {
      viewHolder.ivProfilePhoto.setVisibility(View.VISIBLE);

      Picasso.get().load(comment.profileUrl).into(viewHolder.ivProfilePhoto);
    } else {
      viewHolder.ivProfilePhoto.setVisibility(View.GONE);
    }

    if (comment.username != null) {
      viewHolder.tvUsername.setText(comment.username);
      viewHolder.tvUsername.setVisibility(View.VISIBLE);
    } else {
      viewHolder.tvUsername.setVisibility(View.GONE);
    }

    if (comment.rating > 0) {
      viewHolder.ratingBar.setVisibility(View.VISIBLE);
      viewHolder.ratingBar.setRating(comment.rating);
    } else {
      viewHolder.ratingBar.setVisibility(View.GONE);
    }

    viewHolder.tvComment.setText(Html.fromHtml(comment.text.replaceAll("<img.+?>", "")));

    if (type == CommentsActivity.WORDPRESS_JETPACK || type == CommentsActivity.WORDPRESS_JSON || type == CommentsActivity.WORDPRESS_REST) {
      LinearLayout lineView = convertView.findViewById(R.id.lineView);

      lineView.removeAllViews();
      for (int i = 0; i < comment.linesCount; i++) {
        View line = View.inflate(context, R.layout.activity_comment_sub, null);
        lineView.addView(line);
      }
    }

    return convertView;
  }

  public class CommentViewHolder {
    ImageView ivProfilePhoto;
    TextView tvUsername;
    TextView tvComment;
    RatingBar ratingBar;
  }

}
