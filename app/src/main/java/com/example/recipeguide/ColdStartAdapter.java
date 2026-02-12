/*
package com.example.recipeguide;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.motion.widget.MotionLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.List;

import Model.Recipe;

public class ColdStartAdapter extends RecyclerView.Adapter<ColdStartAdapter.VH> {

    public interface OnCardSwipedListener {
        void onLiked(String recipeId);
        void onDisliked(String recipeId);
        void onAllCardsProcessed();
    }

    private final List<Recipe> items;
    private final OnCardSwipedListener listener;
    private final Context context;
    private final RecyclerView recycler;

    public ColdStartAdapter(List<Recipe> items, OnCardSwipedListener listener, Context context, RecyclerView recycler) {
        this.items = items;
        this.listener = listener;
        this.context = context;
        this.recycler = recycler;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe_card, parent, false);
        // Убедимся, что ширина карточки = ширине RecyclerView (match_parent)
        ViewGroup.LayoutParams lp = v.getLayoutParams();
        if (lp != null) lp.width = parent.getWidth() > 0 ? parent.getWidth() : ViewGroup.LayoutParams.MATCH_PARENT;
        Log.d("CS", "onCreateViewHolder parentWidth=" + parent.getWidth());
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        holder.bind(items.get(position));
        // Устанавливаем ширину itemView равной ширине RecyclerView
        recycler.post(() -> {
            int parentWidth = recycler.getWidth();
            if (parentWidth > 0 && holder.itemView.getLayoutParams().width != parentWidth) {
                holder.itemView.getLayoutParams().width = parentWidth;
                holder.itemView.requestLayout();
            }
        });
        Log.d("CS", "onBind pos=" + position + " itemCount=" + getItemCount());

    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public Recipe removeAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        Recipe r = items.remove(position);
        notifyItemRemoved(position);
        if (items.isEmpty() && listener != null) listener.onAllCardsProcessed();
        return r;
    }

    class VH extends RecyclerView.ViewHolder {
        MotionLayout motion;
        ImageView image;
        TextView title;
        ImageView ivLike, ivDislike;

        VH(@NonNull View itemView) {
            super(itemView);
            motion = itemView.findViewById(R.id.motion_card);
            image = itemView.findViewById(R.id.iv_recipe_image);
            title = itemView.findViewById(R.id.tv_recipe_title);
            ivLike = itemView.findViewById(R.id.iv_like);
            ivDislike = itemView.findViewById(R.id.iv_dislike);

            // Слушаем завершение перехода, чтобы удалить карточку и уведомить слушателя
            motion.setTransitionListener(new MotionLayout.TransitionListener() {
                @Override public void onTransitionStarted(MotionLayout motionLayout, int startId, int endId) {}
                @Override public void onTransitionChange(MotionLayout motionLayout, int startId, int endId, float progress) {
                    // можно анимировать иконки по прогрессу, но это уже в MotionScene
                }
                @Override public void onTransitionCompleted(MotionLayout motionLayout, int currentId) {
                    // Определяем, какой переход завершился по currentId
                    int adapterPos = getAdapterPosition();
                    // currentId будет равен id конечного ConstraintSet (end_like или end_dislike)
                    if (currentId == R.id.end_like || currentId == R.id.end_dislike) {
                        // Сохраняем id до удаления
                        Recipe removed = null;
                        if (adapterPos >= 0 && adapterPos < items.size()) {
                            removed = items.get(adapterPos);
                        }
                        // Удаляем элемент и уведомляем слушателя
                        int posToRemove = getAdapterPosition();
                        if (posToRemove >= 0 && posToRemove < items.size()) {
                            Recipe r = removeAt(posToRemove);
                            if (r != null && listener != null) {
                                if (currentId == R.id.end_like) listener.onLiked(r.getId());
                                else listener.onDisliked(r.getId());
                            }
                        } else {
                            // Если позиция некорректна — просто notifyDataSetChanged
                            notifyDataSetChanged();
                        }
                    }
                }
                @Override public void onTransitionTrigger(MotionLayout motionLayout, int triggerId, boolean positive, float progress) {}
            });
        }

        void bind(Recipe r) {
            SharedPreferences sharedPreferences = context.getSharedPreferences("MODE", Context.MODE_PRIVATE);

            if(sharedPreferences.getBoolean("language", false)){
                title.setText(r.getName() != null ? r.getName() : "");
            }else{
                title.setText(r.getName_en() != null ? r.getName_en() : "");
            }

            String imagePath = r.getImage();
            if (imagePath != null) {
                File imgFile = new File("/data/data/com.example.recipeguide/files/" + imagePath + ".jpg");
                if (imgFile.exists()) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    image.setImageBitmap(bitmap); // Устанавливаем изображение в ImageView
                }else {
                    Glide.with(image.getContext())
                            .load(imagePath)
                            .diskCacheStrategy(DiskCacheStrategy.ALL) // Загружаем из кеша, если интернета нет
                            .into(image);
                }
            } else {
                // Устанавливаем изображение-заглушку, если данных нет
                image.setImageResource(R.drawable.dumplings);
            }

            // Сбрасываем состояние MotionLayout в начальное
            motion.post(() -> {
                motion.transitionToStart(); // гарантированно вернёт в стартовое состояние
                motion.setProgress(0f);
            });

        }
    }
}
*/
