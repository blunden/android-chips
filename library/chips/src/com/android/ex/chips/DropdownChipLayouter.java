package com.android.ex.chips;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.StateListDrawable;
import android.net.Uri;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.text.TextUtils;
import android.text.util.Rfc822Tokenizer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.ex.chips.Queries.Query;
import com.android.ex.chips.ResultAnimationDrawable.STATE;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that inflates and binds the views in the dropdown list from
 * RecipientEditTextView.
 */
public class DropdownChipLayouter {
    /**
     * The type of adapter that is requesting a chip layout.
     */
    public enum AdapterType {
        BASE_RECIPIENT,
        RECIPIENT_ALTERNATES,
        SINGLE_RECIPIENT
    }

    public interface ChipDeleteListener {
        void onChipDelete();
    }

    public interface ChipSuggestionsListener {
        void onAddSuggestionRequest(RecipientEntry entry, View v);
        void onDeleteSuggestionRequest(RecipientEntry entry, View v);
    }

    public interface ChipAnimationCallback {
        void onAnimationEnded();
    }

    private final OnClickListener mSuggestionClickListener = new OnClickListener() {
        @Override
        public void onClick(View v) {
            View parent = (View) v.getParent().getParent();
            RecipientEntry entry = (RecipientEntry) v.getTag();

            if (v.getId() == R.id.chip_suggested_contact_add) {
                mSuggestionsListener.onAddSuggestionRequest(entry, parent);
            } else if (v.getId() == R.id.chip_suggested_contact_delete) {
                mSuggestionsListener.onDeleteSuggestionRequest(entry, parent);
            }
        }
    };

    private final LayoutInflater mInflater;
    private final Context mContext;
    private ChipDeleteListener mDeleteListener;
    private ChipSuggestionsListener mSuggestionsListener;
    private Query mQuery;

    public DropdownChipLayouter(LayoutInflater inflater, Context context) {
        mInflater = inflater;
        mContext = context;
    }

    public void setQuery(Query query) {
        mQuery = query;
    }

    public void setDeleteListener(ChipDeleteListener listener) {
        mDeleteListener = listener;
    }

    public void setSuggestionsListener(ChipSuggestionsListener listener) {
        mSuggestionsListener = listener;
    }

    void animateSuggestion(View v, final ChipAnimationCallback cb,
            STATE state, final boolean backToView) {

        View icons = v.findViewById(R.id.chips_recipients_icons_layout);
        final ImageView action = (ImageView) v.findViewById(R.id.chip_recipients_action_layout);

        int color = mContext.getResources().getColor(R.color.chip_suggestion_action);

        final ResultAnimationDrawable dw = new ResultAnimationDrawable();
        dw.setColors(color, color);
        dw.setState(state);
        dw.setInterpolation(0f);
        dw.setDuration(250);
        action.setImageDrawable(dw);

        List<Animator> animators = new ArrayList<>();

        // Fade the icons layout
        Animator fadeOut1 = ObjectAnimator.ofFloat(icons, "alpha", 1.0f, 0.0f);
        fadeOut1.setDuration(150);
        fadeOut1.addListener(new AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                action.setVisibility(View.GONE);
                action.setAlpha(1f);
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                action.setVisibility(View.VISIBLE);
                dw.start(); // 250 milliseconds
            }

            @Override
            public void onAnimationCancel(Animator animation) {
            }
        });
        animators.add(fadeOut1);

        if (backToView) {
            // Fade out action
            Animator fadeOut2 = ObjectAnimator.ofFloat(action, "alpha", 1.0f, 0.0f);
            fadeOut2.setDuration(150);
            fadeOut2.setStartDelay(650);
            animators.add(fadeOut2);

            // Fade in to icons
            Animator fadeIn = ObjectAnimator.ofFloat(icons, "alpha", 0.0f, 1.0f);
            fadeIn.setDuration(150);
            fadeIn.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                    action.setVisibility(View.GONE);
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cb != null) {
                        cb.onAnimationEnded();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            animators.add(fadeIn);

        } else {
            // End animation (don't return to icons layout)
            Animator end = ObjectAnimator.ofFloat(action, "alpha", 1.0f, 1.0f);
            end.setDuration(150);
            end.setStartDelay(650);
            end.addListener(new AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    if (cb != null) {
                        cb.onAnimationEnded();
                    }
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }
            });
            animators.add(end);
        }

        AnimatorSet set = new AnimatorSet();
        set.setInterpolator(new LinearInterpolator());
        set.playSequentially(animators);
        set.start();
    }


    /**
     * Layouts and binds recipient information to the view. If convertView is null, inflates a new
     * view with getItemLaytout().
     *
     * @param convertView The view to bind information to.
     * @param parent The parent to bind the view to if we inflate a new view.
     * @param entry The recipient entry to get information from.
     * @param position The position in the list.
     * @param type The adapter type that is requesting the bind.
     * @param constraint The constraint typed in the auto complete view.
     *
     * @return A view ready to be shown in the drop down list.
     */
    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position,
        AdapterType type, String constraint) {
        return bindView(convertView, parent, entry, position, type, constraint, null);
    }

    /**
     * See {@link #bindView(View, ViewGroup, RecipientEntry, int, AdapterType, String)}
     * @param deleteDrawable
     */
    public View bindView(View convertView, ViewGroup parent, RecipientEntry entry, int position,
            AdapterType type, String constraint, StateListDrawable deleteDrawable) {
        // Default to show all the information
        String displayName = entry.getDisplayName();
        String destination = entry.getDestination();
        boolean showImage = true;
        CharSequence destinationType = getDestinationType(entry);

        final View itemView = reuseOrInflateView(convertView, parent, type);

        final ViewHolder viewHolder = new ViewHolder(itemView);

        // Hide some information depending on the entry type and adapter type
        switch (type) {
            case BASE_RECIPIENT:
                if (TextUtils.isEmpty(displayName) || TextUtils.equals(displayName, destination)) {
                    displayName = destination;

                    // We only show the destination for secondary entries, so clear it only for the
                    // first level.
                    if (entry.isFirstLevel()) {
                        destination = null;
                    }
                }

                if (!entry.isFirstLevel()) {
                    displayName = null;
                    showImage = false;
                }

                // For BASE_RECIPIENT set all top dividers except for the first one to be GONE.
                if (viewHolder.topDivider != null) {
                    viewHolder.topDivider.setVisibility(position == 0 ? View.VISIBLE : View.GONE);
                }
                break;
            case RECIPIENT_ALTERNATES:
                if (position != 0) {
                    displayName = null;
                    showImage = false;
                }
                break;
            case SINGLE_RECIPIENT:
                destination = Rfc822Tokenizer.tokenize(entry.getDestination())[0].getAddress();
                destinationType = null;
        }

        // Bind the information to the view
        bindTextToView(displayName, viewHolder.displayNameView);
        bindTextToView(destination, viewHolder.destinationView);
        bindTextToView(destinationType, viewHolder.destinationTypeView);
        if (entry.getDestinationType() == BaseRecipientAdapter.SUGGESTED_ENTRY_DESTINATION_TYPE) {
            if (viewHolder.addSuggestionView != null) {
                viewHolder.addSuggestionView.setTag(entry);
                viewHolder.addSuggestionView.setVisibility(View.VISIBLE);
            }
            if (viewHolder.deleteSuggestionView != null) {
                viewHolder.deleteSuggestionView.setTag(entry);
                viewHolder.deleteSuggestionView.setVisibility(View.VISIBLE);
            }
            viewHolder.imageView.setVisibility(View.GONE);
        } else {
            bindIconToView(showImage, entry, viewHolder.imageView, type);

            if (viewHolder.addSuggestionView != null) {
                viewHolder.addSuggestionView.setVisibility(View.GONE);
            }
            if (viewHolder.deleteSuggestionView != null) {
                viewHolder.deleteSuggestionView.setVisibility(View.GONE);
            }
            viewHolder.imageView.setVisibility(View.VISIBLE);
        }
        bindDrawableToDeleteView(deleteDrawable, viewHolder.deleteView);

        // Revert animations
        if (viewHolder.iconsView != null) {
            viewHolder.iconsView.setAlpha(1.0f);
            viewHolder.iconsView.setVisibility(View.VISIBLE);
        }
        if (viewHolder.actionView != null) {
            viewHolder.actionView.setAlpha(1.0f);
            viewHolder.actionView.setVisibility(View.GONE);
        }

        return itemView;
    }

    /**
     * Returns a new view with {@link #getItemLayoutResId(AdapterType)}.
     */
    public View newView(AdapterType type) {
        return mInflater.inflate(getItemLayoutResId(type), null);
    }

    /**
     * Returns the same view, or inflates a new one if the given view was null.
     */
    protected View reuseOrInflateView(View convertView, ViewGroup parent, AdapterType type) {
        int itemLayout = getItemLayoutResId(type);
        switch (type) {
            case BASE_RECIPIENT:
            case RECIPIENT_ALTERNATES:
                break;
            case SINGLE_RECIPIENT:
                itemLayout = getAlternateItemLayoutResId(type);
                break;
        }
        return convertView != null ? convertView : mInflater.inflate(itemLayout, parent, false);
    }

    /**
     * Binds the text to the given text view. If the text was null, hides the text view.
     */
    protected void bindTextToView(CharSequence text, TextView view) {
        if (view == null) {
            return;
        }

        if (text != null) {
            view.setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    /**
     * Binds the avatar icon to the image view. If we don't want to show the image, hides the
     * image view.
     */
    protected void bindIconToView(boolean showImage, RecipientEntry entry, ImageView view,
        AdapterType type) {
        if (view == null) {
            return;
        }

        if (showImage) {
            switch (type) {
                case BASE_RECIPIENT:
                    byte[] photoBytes = entry.getPhotoBytes();
                    if (photoBytes != null && photoBytes.length > 0) {
                        final Bitmap photo = BitmapFactory.decodeByteArray(photoBytes, 0,
                            photoBytes.length);
                        view.setImageBitmap(photo);
                    } else {
                        view.setImageResource(getDefaultPhotoResId());
                    }
                    break;
                case RECIPIENT_ALTERNATES:
                    Uri thumbnailUri = entry.getPhotoThumbnailUri();
                    if (thumbnailUri != null) {
                        // TODO: see if this needs to be done outside the main thread
                        // as it may be too slow to get immediately.
                        view.setImageURI(thumbnailUri);
                    } else {
                        view.setImageResource(getDefaultPhotoResId());
                    }
                    break;
                case SINGLE_RECIPIENT:
                default:
                    view.setImageResource(getDefaultPhotoResId());
                    break;
            }
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    protected void bindDrawableToDeleteView(final StateListDrawable drawable, ImageView view) {
        if (view == null) {
            return;
        }
        if (drawable == null) {
            view.setVisibility(View.GONE);
        }

        view.setImageDrawable(drawable);
        if (mDeleteListener != null) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (drawable.getCurrent() != null) {
                        mDeleteListener.onChipDelete();
                    }
                }
            });
        }
    }

    protected CharSequence getDestinationType(RecipientEntry entry) {
        return mQuery.getTypeLabel(mContext.getResources(), entry.getDestinationType(),
            entry.getDestinationLabel()).toString().toUpperCase();
    }

    /**
     * Returns a layout id for each item inside auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected @LayoutRes int getItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    /**
     * Returns a layout id for each item inside alternate auto-complete list.
     *
     * Each View must contain two TextViews (for display name and destination) and one ImageView
     * (for photo). Ids for those should be available via {@link #getDisplayNameResId()},
     * {@link #getDestinationResId()}, and {@link #getPhotoResId()}.
     */
    protected @LayoutRes int getAlternateItemLayoutResId(AdapterType type) {
        switch (type) {
            case BASE_RECIPIENT:
                return R.layout.chips_autocomplete_recipient_dropdown_item;
            case RECIPIENT_ALTERNATES:
                return R.layout.chips_recipient_dropdown_item;
            default:
                return R.layout.chips_recipient_dropdown_item;
        }
    }

    /**
     * Returns a resource ID representing an image which should be shown when ther's no relevant
     * photo is available.
     */
    protected @DrawableRes int getDefaultPhotoResId() {
        return R.drawable.ic_contact_picture;
    }

    /**
     * Returns an id for TextView in an item View for showing a display name. By default
     * {@link android.R.id#title} is returned.
     */
    protected @IdRes int getDisplayNameResId() {
        return android.R.id.title;
    }

    /**
     * Returns an id for TextView in an item View for showing a destination
     * (an email address or a phone number).
     * By default {@link android.R.id#text1} is returned.
     */
    protected @IdRes int getDestinationResId() {
        return android.R.id.text1;
    }

    /**
     * Returns an id for TextView in an item View for showing the type of the destination.
     * By default {@link android.R.id#text2} is returned.
     */
    protected @IdRes int getDestinationTypeResId() {
        return android.R.id.text2;
    }

    /**
     * Returns an id for ImageView in an item View for showing photo image for a person. In default
     * {@link android.R.id#icon} is returned.
     */
    protected @IdRes int getPhotoResId() {
        return android.R.id.icon;
    }

    /**
     * Returns an id for ImageView in an item View for showing the delete button. In default
     * {@link android.R.id#icon1} is returned.
     */
    protected @IdRes int getDeleteResId() { return android.R.id.icon1; }

    /**
     * A holder class the view. Uses the getters in DropdownChipLayouter to find the id of the
     * corresponding views.
     */
    protected class ViewHolder {
        public final TextView displayNameView;
        public final TextView destinationView;
        public final TextView destinationTypeView;
        public final ImageView imageView;
        public final ImageView deleteView;
        public final ImageView addSuggestionView;
        public final ImageView deleteSuggestionView;
        public final View iconsView;
        public final View actionView;
        public final View topDivider;

        public ViewHolder(View view) {
            displayNameView = (TextView) view.findViewById(getDisplayNameResId());
            destinationView = (TextView) view.findViewById(getDestinationResId());
            destinationTypeView = (TextView) view.findViewById(getDestinationTypeResId());
            imageView = (ImageView) view.findViewById(getPhotoResId());
            deleteView = (ImageView) view.findViewById(getDeleteResId());
            topDivider = view.findViewById(R.id.chips_recipients_icons_layout);
            addSuggestionView = (ImageView) view.findViewById(R.id.chip_suggested_contact_add);
            if (addSuggestionView != null) {
                addSuggestionView.setOnClickListener(mSuggestionClickListener);
            }
            deleteSuggestionView = (ImageView) view.findViewById(R.id.chip_suggested_contact_delete);
            if (deleteSuggestionView != null) {
                deleteSuggestionView.setOnClickListener(mSuggestionClickListener);
            }
            iconsView = view.findViewById(R.id.chips_recipients_icons_layout);
            actionView = view.findViewById(R.id.chip_recipients_action_layout);
        }
    }
}
