/*
package Model;

import android.view.View;
import androidx.constraintlayout.motion.widget.MotionLayout;

import com.example.recipeguide.R;

public class MotionLayoutHolderHelper {
    public static void startMotionForSwipe(View itemView, int direction) {
        MotionLayout motion = itemView.findViewById(R.id.motion_card);
        if (motion == null) return;
        if (direction == androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
            // Запускаем переход к end_like
            motion.setTransition(R.id.transition_like);
            motion.transitionToEnd();
        } else if (direction == androidx.recyclerview.widget.ItemTouchHelper.LEFT) {
            motion.setTransition(R.id.transition_dislike);
            motion.transitionToEnd();
        }
    }
}

*/
