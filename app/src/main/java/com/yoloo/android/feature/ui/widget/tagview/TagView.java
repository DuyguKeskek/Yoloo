package com.yoloo.android.feature.ui.widget.tagview;

import android.animation.LayoutTransition;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.yoloo.android.R;
import io.reactivex.Observable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import timber.log.Timber;

/**
 * Created by greenfrvr
 */
public class TagView extends LinearLayout {

  public static final int GRAVITY_LEFT = Gravity.START;
  public static final int GRAVITY_RIGHT = Gravity.END;
  public static final int GRAVITY_CENTER = Gravity.CENTER;

  public static final int MODE_WRAP = 0;
  public static final int MODE_STRETCH = 1;
  public static final int MODE_EQUAL = 2;

  public static final int DISTRIBUTION_NONE = -1;
  public static final int DISTRIBUTION_LEFT = 0;
  public static final int DISTRIBUTION_MIDDLE = 1;
  public static final int DISTRIBUTION_RIGHT = 2;
  public static final int DISTRIBUTION_RANDOM = 3;

  public static final int ELLIPSIZE_START = 0;
  public static final int ELLIPSIZE_MIDDLE = 1;
  public static final int ELLIPSIZE_END = 2;
  public static final int ELLIPSIZE_MARQUEE = 3;

  private static final SparseArray<TextUtils.TruncateAt> ellipsizeList = new SparseArray<>(4);

  static {
    ellipsizeList.put(ELLIPSIZE_START, TextUtils.TruncateAt.START);
    ellipsizeList.put(ELLIPSIZE_MIDDLE, TextUtils.TruncateAt.MIDDLE);
    ellipsizeList.put(ELLIPSIZE_END, TextUtils.TruncateAt.END);
    ellipsizeList.put(ELLIPSIZE_MARQUEE, TextUtils.TruncateAt.MARQUEE);
  }

  private final LayoutParams rowLayoutParams =
      new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  private final LayoutParams itemLayoutParams =
      new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
  private final FrameLayout.LayoutParams itemFrameParams =
      new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT);

  private LayoutTransition layoutTransition;

  private List<TagsClickListener> clickListeners;

  private List<TagsSelectListener> selectListeners;

  private List<Float> widthList;

  private List<TagItem> data;

  private Multimap<Integer, TagItem> viewMap;

  private SortState sortState = SortState.initState();

  private ColorStateList itemTextColorStateList;

  private int itemMargin;

  private int itemPaddingLeft;
  private int itemPaddingRight;
  private int itemPaddingTop;
  private int itemPaddingBottom;

  private int itemDrawablePadding;

  private int minItemWidth;
  private int maxItemWidth;

  private int itemTextGravity;

  private int itemTextEllipsize;

  private float itemTextSize;

  private int rowMargin;

  private int rowGravity;

  private int rowDistribution;

  private int rowMode;

  private int rowCount;

  private int backgroundDrawable;
  private int foregroundDrawable;

  private int leftDrawable;

  private int leftSelectedDrawable;

  private int rightDrawable;

  private int rightSelectedDrawable;

  private Typeface typeface;

  private float totalItemsWidth;

  private boolean isInSelectMode;

  private boolean isDynamic;

  private DataTransform transformer = DefaultTransform.create();

  private DataSelector selector = DefaultSelector.create();

  private final ViewTreeObserver.OnPreDrawListener preDrawListener =
      new ViewTreeObserver.OnPreDrawListener() {
        @Override
        public boolean onPreDraw() {
          if (isPrepared()) {
            wrap();
            sort();
            draw();
            getViewTreeObserver().removeOnPreDrawListener(preDrawListener);
          }
          return true;
        }
      };

  private Resources resources;

  public TagView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);
    setGravity(Gravity.CENTER_HORIZONTAL);

    resources = getResources();

    init(attrs);
    prepareLayoutParams();
    prepareLayoutTransition();

    widthList = new ArrayList<>();
    data = new ArrayList<>();
  }

  /**
   * Method defines data as simple {@link String} array. Using this method makes not
   * possible to use {@link android.text.Spannable} for representing items label.
   *
   * @param list {@link String} array representing data collection.
   */
  public <T> void setData(@NonNull List<T> list) {
    widthList.clear();
    data.clear();

    if (list.isEmpty()) {
      removeAllViews();
      return;
    }

    for (T item : list) {
      data.add(TagItem.create(item));
    }
    getViewTreeObserver().addOnPreDrawListener(preDrawListener);
  }

  /**
   * Method defines data as an array of custom data model. Using this method allow you
   * to use {@link android.text.Spannable} for representing items label.
   *
   * @param list Array of user defined objects representing data collection.
   * @param transformer Implementation of {@link DataTransform} or {@link DataStateTransform}
   * interface. Can be used for building label from several custom data model fields or to prepare
   * {@link android.text.Spannable} label representation.
   * @param <T> Custom data model
   */
  public <T> void setData(@NonNull List<T> list, @NonNull DataTransform<T> transformer) {
    this.transformer = transformer;
    setData(list);
  }

  /**
   * Method defines data as an array of custom data model. Using this method allow you
   * to use {@link android.text.Spannable} for representing items label and define which items
   * should be preselected items.
   *
   * @param list Array of user defined objects representing data collection.
   * @param transformer Implementation of {@link DataTransform} or {@link DataStateTransform}
   * interface. Can be used for building label from several custom data model fields or to prepare
   * {@link android.text.Spannable} label representation.
   * @param selector Implementation of {@link DataSelector} interface. Can be used to preselect some
   * items.
   * @param <T> Custom data model
   */
  public <T> void setData(@NonNull List<T> list, @NonNull DataTransform<T> transformer,
      @NonNull DataSelector<T> selector) {
    this.selector = selector;
    setData(list, transformer);
  }

  /**
   * Dynamically adds new item to a widget.
   *
   * @param item Object representing new item to be added
   * @param <T> Custom data model class
   * @return true if item was added successfully, false otherwise (for example the item has been
   * already presented)
   */
  public <T> boolean addItem(@NonNull T item) {
    if (!isDynamic) return false;

    TagItem tagItem = TagItem.create(item);
    if (viewMap != null && viewMap.values().contains(tagItem)) return false;

    if (viewMap != null) {
      data.addAll(viewMap.values());
      viewMap.clear();
    }
    data.add(tagItem);

    getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    return true;
  }

  /**
   * Dynamically removes given item from a widget if it is already presented in a widget.
   *
   * @param item Object representing item to be removed
   * @param <T> Custom data model class
   * @return true if item was removed successfully, false otherwise (for example there was no item
   * to remove)
   */
  public <T> boolean removeItem(@NonNull T item) {
    if (!isDynamic || viewMap == null || viewMap.isEmpty()) return false;

    TagItem<T> tagItem = TagItem.create(item);
    if (!viewMap.values().contains(tagItem)) return false;

    data.addAll(viewMap.values());
    data.remove(tagItem);
    if (data.isEmpty()) removeAllViews();
    viewMap.clear();

    getViewTreeObserver().addOnPreDrawListener(preDrawListener);
    return true;
  }

  /**
   * @param transformer Implementation of {@link DataTransform} or {@link DataStateTransform}
   * interface. Can be used for building label from several custom data model fields or to prepare
   * {@link android.text.Spannable} label representation.
   * @param <T> Custom data model
   */
  public <T> void setTransformer(@NonNull DataTransform<T> transformer) {
    this.transformer = transformer;
  }

  /**
   * @return List of selected items. Consists of objects corresponding to custom data model defined
   * by setData() method
   */
  public <T> List<T> getSelectedItems() {
    return Observable.fromIterable(viewMap.values())
        .filter(item -> item.isSelected)
        .map(item -> (T) item.data)
        .toList()
        .blockingGet();
  }

  public <T> Observable<List<T>> getSelectedItemsObservable() {
    if (viewMap == null) return Observable.empty();
    return Observable.fromIterable(viewMap.values())
        .filter(item -> item.isSelected)
        .map(item -> (T) item.data)
        .toList()
        .toObservable();
  }

  /**
   * Adding single item click listener
   *
   * @param listener {@link TagsClickListener}
   */
  public void addOnTagClickListener(TagsClickListener listener) {
    if (clickListeners == null) {
      clickListeners = new ArrayList<>();
    }
    clickListeners.add(listener);
  }

  /**
   * Removing single item click listener
   *
   * @param listener {@link TagsClickListener}
   */
  public void removeOnTagClickListener(TagsClickListener listener) {
    if (clickListeners != null) {
      clickListeners.remove(listener);
    }
  }

  /**
   * Adding selection items listener
   *
   * @param listener {@link TagsSelectListener}
   */
  public void addOnTagSelectListener(TagsSelectListener listener) {
    if (selectListeners == null) {
      selectListeners = new ArrayList<>();
    }
    selectListeners.add(listener);
  }

  /**
   * Removing selection items listener
   *
   * @param listener {@link TagsSelectListener}
   */
  public void removeOnTagSelectListener(TagsSelectListener listener) {
    if (selectListeners != null) {
      selectListeners.remove(listener);
    }
  }

  /**
   * Removing all defined listeners
   */
  public void removeListeners() {
    if (clickListeners != null) {
      clickListeners.clear();
    }

    if (selectListeners != null) {
      selectListeners.clear();
    }
  }

  public void setItemMargin(int itemMargin) {
    this.itemMargin = itemMargin;
  }

  public void setItemMarginRes(@DimenRes int marginRes) {
    this.itemMargin = resources.getDimensionPixelOffset(marginRes);
  }

  public void setItemPadding(int left, int right, int top, int bottom) {
    this.itemPaddingLeft = left;
    this.itemPaddingRight = right;
    this.itemPaddingTop = top;
    this.itemPaddingBottom = bottom;
  }

  public void setItemPaddingRes(@DimenRes int left, @DimenRes int right, @DimenRes int top,
      @DimenRes int bottom) {
    this.itemPaddingLeft = resources.getDimensionPixelOffset(left);
    this.itemPaddingRight = resources.getDimensionPixelOffset(right);
    this.itemPaddingTop = resources.getDimensionPixelOffset(top);
    this.itemPaddingBottom = resources.getDimensionPixelOffset(bottom);
  }

  public void setMinItemWidth(int minWidth) {
    this.minItemWidth = minWidth;
  }

  public void setMinItemWidthRes(@DimenRes int minWidth) {
    this.minItemWidth = resources.getDimensionPixelOffset(minWidth);
  }

  public void setMaxItemWidth(int maxWidth) {
    this.maxItemWidth = maxWidth;
  }

  public void setMaxItemWidthRes(@DimenRes int maxWidth) {
    this.maxItemWidth = resources.getDimensionPixelOffset(maxWidth);
  }

  public void setItemTextColor(int textColor) {
    this.itemTextColorStateList = ColorStateList.valueOf(textColor);
  }

  public void setItemTextColorRes(@ColorRes int textColor) {
    int colorValue = ContextCompat.getColor(getContext(), textColor);
    this.itemTextColorStateList = ColorStateList.valueOf(colorValue);
  }

  public void setItemTextColorStateList(ColorStateList stateList) {
    this.itemTextColorStateList = stateList;
  }

  public void setItemTextColorStateListRes(@ColorRes int colorStateRes) {
    this.itemTextColorStateList = ContextCompat.getColorStateList(getContext(), colorStateRes);
  }

  public void setItemTextGravity(int itemTextGravity) {
    this.itemTextGravity = itemTextGravity;
  }

  public void setItemTextSize(float textSize) {
    this.itemTextSize = textSize;
  }

  public void setItemTextSizeRes(@DimenRes int textSize) {
    this.itemTextSize = resources.getDimension(textSize);
  }

  public void setRowMargin(int rowMargin) {
    this.rowMargin = rowMargin;
  }

  public void setRowMarginRes(@DimenRes int rowMargin) {
    this.rowMargin = resources.getDimensionPixelOffset(rowMargin);
  }

  public void setRowGravity(@GravityMode int rowGravity) {
    this.rowGravity = rowGravity;
  }

  public void setRowMode(@StretchMode int rowMode) {
    this.rowMode = rowMode;
  }

  public void setRowDistribution(@RowDistribution int rowDistribution) {
    this.rowDistribution = rowDistribution;
  }

  public void setRowCount(int count) {
    if (count >= 0) this.rowCount = count;
  }

  public void setBackgroundDrawable(@DrawableRes int backgroundDrawable) {
    this.backgroundDrawable = backgroundDrawable;
  }

  public void setBackgroundColor(@ColorRes int backgroundDrawable) {
    this.backgroundDrawable = backgroundDrawable;
  }

  public void setForegroundDrawable(@DrawableRes int foregroundDrawable) {
    this.foregroundDrawable = foregroundDrawable;
  }

  public void setLeftDrawable(@DrawableRes int drawableRes) {
    this.leftDrawable = drawableRes;
  }

  public void setLeftSelectedDrawable(@DrawableRes int drawableRes) {
    this.leftSelectedDrawable = drawableRes;
  }

  public void setRightDrawable(@DrawableRes int drawableRes) {
    this.rightDrawable = drawableRes;
  }

  public void setRightSelectedDrawable(@DrawableRes int drawableRes) {
    this.rightSelectedDrawable = drawableRes;
  }

  public void setInSelectMode(boolean selectMode) {
    isInSelectMode = selectMode;
  }

  public void setDynamicMode(boolean dynamicMode) {
    isDynamic = dynamicMode;
  }

  public void setTypeface(Typeface typeface) {
    this.typeface = typeface;
  }

  public void setEllipsize(@Ellipsize int ellipsizeMode) {
    itemTextEllipsize = ellipsizeMode;
  }

  private void init(AttributeSet attrs) {
    TypedArray a =
        getContext().getTheme().obtainStyledAttributes(attrs, R.styleable.TagView, 0, 0);

    try {
      itemMargin = a.getDimensionPixelOffset(R.styleable.TagView_tagMargin,
          resources.getDimensionPixelOffset(R.dimen.default_item_margin));
      itemPaddingLeft = a.getDimensionPixelOffset(R.styleable.TagView_tagPaddingLeft,
          resources.getDimensionPixelOffset(R.dimen.default_item_padding));
      itemPaddingRight = a.getDimensionPixelOffset(R.styleable.TagView_tagPaddingRight,
          resources.getDimensionPixelOffset(R.dimen.default_item_padding));
      itemPaddingTop = a.getDimensionPixelOffset(R.styleable.TagView_tagPaddingTop,
          resources.getDimensionPixelOffset(R.dimen.default_item_padding));
      itemPaddingBottom = a.getDimensionPixelOffset(R.styleable.TagView_tagPaddingBottom,
          resources.getDimensionPixelOffset(R.dimen.default_item_padding));
      itemDrawablePadding =
          a.getDimensionPixelOffset(R.styleable.TagView_tagDrawablePadding, 0);
      minItemWidth = a.getDimensionPixelOffset(R.styleable.TagView_tagMinWidth,
          resources.getDimensionPixelOffset(R.dimen.min_item_width));
      maxItemWidth = a.getDimensionPixelOffset(R.styleable.TagView_tagMaxWidth,
          resources.getDimensionPixelOffset(R.dimen.min_item_width));
      rowMargin = a.getDimensionPixelOffset(R.styleable.TagView_rowMargin,
          resources.getDimensionPixelOffset(R.dimen.default_row_margin));
      itemTextSize = a.getDimension(R.styleable.TagView_tagTextSize,
          resources.getDimension(R.dimen.default_text_size));

      itemTextGravity = a.getInt(R.styleable.TagView_tagTextGravity, Gravity.CENTER);
      itemTextEllipsize = a.getInt(R.styleable.TagView_tagEllipsize, ELLIPSIZE_END);
      rowGravity = a.getInt(R.styleable.TagView_rowGravity, Gravity.CENTER);
      rowDistribution = a.getInt(R.styleable.TagView_rowDistribution, DISTRIBUTION_RANDOM);
      rowMode = a.getInt(R.styleable.TagView_rowMode, MODE_WRAP);
      rowCount = a.getInt(R.styleable.TagView_rowsQuantity, 0);

      backgroundDrawable = a.getResourceId(R.styleable.TagView_tagBackground, 0);
      foregroundDrawable = a.getResourceId(R.styleable.TagView_tagForeground, 0);
      leftDrawable = a.getResourceId(R.styleable.TagView_tagDrawableLeft, 0);
      leftSelectedDrawable = a.getResourceId(R.styleable.TagView_tagSelectedDrawableLeft, 0);
      rightDrawable = a.getResourceId(R.styleable.TagView_tagDrawableRight, 0);
      rightSelectedDrawable = a.getResourceId(R.styleable.TagView_tagSelectedDrawableRight, 0);

      itemTextColorStateList = a.getColorStateList(R.styleable.TagView_tagTextColor);
      if (itemTextColorStateList == null) {
        itemTextColorStateList = ColorStateList.valueOf(Color.BLACK);
      }

      isInSelectMode = a.getBoolean(R.styleable.TagView_selectionMode, false);
      isDynamic = a.getBoolean(R.styleable.TagView_dynamicMode, false);
    } finally {
      a.recycle();
    }
  }

  private void prepareLayoutParams() {
    itemFrameParams.gravity = itemTextGravity;

    itemLayoutParams.leftMargin = itemMargin;
    itemLayoutParams.rightMargin = itemMargin;
    itemLayoutParams.weight = rowMode > 0 ? 1 : 0;
    if (MODE_EQUAL == rowMode) {
      itemLayoutParams.width = 0;
    }

    rowLayoutParams.topMargin = rowMargin;
    rowLayoutParams.bottomMargin = rowMargin;
  }

  private void prepareLayoutTransition() {
    if (isDynamic) {
      layoutTransition = new LayoutTransition();
      layoutTransition.setStagger(LayoutTransition.APPEARING, 250);
      layoutTransition.setAnimateParentHierarchy(false);
    }
  }

  private boolean isPrepared() {
    return getViewWidth() > 0 || rowCount > 0;
  }

  private void wrap() {
    if (data == null || data.isEmpty()) return;
    widthList.clear();
    totalItemsWidth = 0;

    for (TagItem item : data) {
      wrapItem(item);
      widthList.add(item.width);
      totalItemsWidth += item.width;
    }

    //Collections.sort(data);
    //Collections.sort(widthList, Collections.reverseOrder());
  }

  private void wrapItem(TagItem item) {
    View view = inflateItemView(item);

    TextView itemView = (TextView) view.findViewById(R.id.text);
    itemView.setText(transformer.prepare(item.data));
    decorateItemTextView(itemView);

    float width = itemView.getMeasuredWidth() + drawableMetrics(itemView) + totalOffset();
    width = Math.max(width, minItemWidth);
    width = Math.min(width, getViewWidth() - 2 * totalOffset());
    item.view = view;
    item.width = width;
    setItemPreselected(item);
  }

  private void setItemPreselected(TagItem item) {
    if (isInSelectMode) {
      item.isSelected = selector.preselect(item.data);
      item.decorateText(transformer);
      item.displaySelection(leftDrawable, leftSelectedDrawable, rightDrawable,
          rightSelectedDrawable);
    }
  }

  private void sort() {
    if (data == null || data.isEmpty()) return;

    evaluateRowsQuantity();

    final int[] rowWidths = new int[sortState.totalRows()];
    viewMap = ArrayListMultimap.create(sortState.totalRows(), data.size());

    sortingLoop(0, sortState.rowCount, rowWidths, true);

    if (sortState.hasExtraRows) {
      sortingLoop(sortState.rowCount, sortState.totalRows(), rowWidths, false);
      sortState.release();
    }
  }

  private boolean extraCondition() {
    return !(sortState.hasExtraRows && data.size() == sortState.extraCount);
  }

  private void sortingLoop(int start, int end, int[] widths, boolean hasExtra) {
    while (!data.isEmpty() && (!hasExtra || extraCondition())) {
      iteration:

      for (int i = start; i < end; i++) {
        Iterator<TagItem> iterator = data.iterator();

        while (iterator.hasNext()) {
          TagItem item = iterator.next();
          if (rowCount > 0 || widths[i] + item.width <= getViewWidth()) {
            widths[i] += item.width;
            viewMap.put(i, item);
            iterator.remove();

            if (hasExtra) continue iteration;
          }
        }
      }
    }
  }

  private void draw() {
    if (viewMap == null || viewMap.isEmpty()) return;
    removeAllViews();

    List<Integer> keys = new ArrayList<>(viewMap.keySet());
    Collections.sort(keys);

    for (Integer key : keys) {
      ViewGroup rowLayout = getRowLayout(viewMap.get(key).size());
      addView(rowLayout);
      applyDistribution(viewMap.get(key));
      rowLayout.setLayoutTransition(layoutTransition);

      for (TagItem item : viewMap.get(key)) {
        releaseParent(item.view);
        rowLayout.addView(item.view, itemLayoutParams);
      }
    }
    keys.clear();
  }

  private void releaseParent(View child) {
    ViewGroup parent = (ViewGroup) child.getParent();
    if (parent != null) {
      parent.removeView(child);
    }
  }

  private void applyDistribution(Collection<TagItem> list) {
    switch (rowDistribution) {
      case DISTRIBUTION_LEFT:
        Collections.sort((List) list);
        break;
      case DISTRIBUTION_MIDDLE:
        SortUtil.symmetricSort((List) list);
        break;
      case DISTRIBUTION_RIGHT:
        Collections.sort((List) list, Collections.reverseOrder());
        break;
      case DISTRIBUTION_RANDOM:
        Collections.shuffle((List) list);
        break;
    }
  }

  private ViewGroup getRowLayout(int weightSum) {
    LinearLayout rowLayout = new LinearLayout(getContext());
    rowLayout.setLayoutParams(rowLayoutParams);
    rowLayout.setOrientation(HORIZONTAL);
    rowLayout.setGravity(rowGravity);
    rowLayout.setWeightSum(weightSum);
    return rowLayout;
  }

  private int getViewWidth() {
    return getWidth() - getPaddingLeft() - getPaddingRight();
  }

  private int totalOffset() {
    return itemPaddingLeft + itemPaddingRight + 2 * itemMargin;
  }

  private int drawableMetrics(TextView textView) {
    int drawablesWidth = 0;
    Drawable[] drawables = textView.getCompoundDrawables();
    drawablesWidth +=
        drawables[0] != null ? drawables[0].getIntrinsicWidth() + itemDrawablePadding : 0;
    drawablesWidth +=
        drawables[2] != null ? drawables[2].getIntrinsicWidth() + itemDrawablePadding : 0;
    return drawablesWidth;
  }

  private void evaluateRowsQuantity() {
    if (widthList == null || widthList.isEmpty()) return;

    if (rowCount > 0) {
      sortState.preserveState(rowCount);
      return;
    }

    int rows = (int) Math.ceil(totalItemsWidth / getViewWidth());
    int[] rowsWidth = new int[rows];
    int iterationLimit = rows + widthList.size();
    int counter = 0;

    sortState.preserveState(rows);
    while (!widthList.isEmpty()) {
      rowIteration:
      for (int i = 0; i < rows; i++) {
        if (counter > iterationLimit) {
          sortState.preserveState(rows, true, widthList.size());
          widthList.clear();
          return;
        }

        counter++;
        for (Float item : widthList) {
          if (rowsWidth[i] + item <= getViewWidth()) {
            rowsWidth[i] += item;
            widthList.remove(item);
            continue rowIteration;
          }
        }
      }
    }
  }

  private View inflateItemView(final TagItem item) {
    ViewGroup itemLayout =
        (ViewGroup) LayoutInflater.from(getContext()).inflate(R.layout.tag_layout, this, false);
    itemLayout.setBackgroundResource(backgroundDrawable);
    itemLayout.setPadding(itemPaddingLeft, itemPaddingTop, itemPaddingRight, itemPaddingBottom);
    itemLayout.setMinimumWidth(minItemWidth);
    try {
      if (foregroundDrawable != 0) {
        ((FrameLayout) itemLayout).setForeground(
            ContextCompat.getDrawable(getContext(), foregroundDrawable));
      }
    } catch (Exception e) {
      Timber.e(e);
    }

    itemLayout.setOnClickListener(v -> {
      if (isInSelectMode) {
        handleSelection(item);
      } else {
        handleClick(item);
      }
    });
    return itemLayout;
  }

  private void decorateItemTextView(TextView textView) {
    textView.setTextColor(itemTextColorStateList);
    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, itemTextSize);
    textView.setCompoundDrawablePadding(itemDrawablePadding);
    textView.setCompoundDrawablesWithIntrinsicBounds(leftDrawable, 0, rightDrawable, 0);
    textView.setEllipsize(ellipsizeList.get(itemTextEllipsize));
    if (maxItemWidth > 0) textView.setMaxWidth(maxItemWidth);
    if (typeface != null) textView.setTypeface(typeface);

    textView.setLayoutParams(itemFrameParams);
    textView.measure(0, 0);
  }

  private void handleSelection(TagItem item) {
    item.select(leftDrawable, leftSelectedDrawable, rightDrawable, rightSelectedDrawable);
    item.decorateText(transformer);

    if (selectListeners != null) {
      for (TagsSelectListener listener : selectListeners) {
        listener.onItemSelected(item.data, item.isSelected);
      }
    }
  }

  private void handleClick(TagItem item) {
    if (clickListeners != null) {
      for (TagsClickListener listener : clickListeners) {
        listener.onItemClicked(item.data);
      }
    }
  }

  @IntDef({GRAVITY_LEFT, GRAVITY_CENTER, GRAVITY_RIGHT})
  @Retention(RetentionPolicy.SOURCE)
  public @interface GravityMode {
  }

  @IntDef({MODE_WRAP, MODE_STRETCH, MODE_EQUAL})
  @Retention(RetentionPolicy.SOURCE)
  public @interface StretchMode {
  }

  @IntDef({DISTRIBUTION_NONE, DISTRIBUTION_LEFT, DISTRIBUTION_MIDDLE, DISTRIBUTION_RIGHT,
      DISTRIBUTION_RANDOM})
  @Retention(RetentionPolicy.SOURCE)
  public @interface RowDistribution {
  }

  @IntDef({ELLIPSIZE_START, ELLIPSIZE_MIDDLE, ELLIPSIZE_END, ELLIPSIZE_MARQUEE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Ellipsize {
  }

  /**
   * Listener used to handle item click events.
   */
  public interface TagsClickListener {
    void onItemClicked(Object item);
  }

  /**
   * Listener used to handle item selection events.
   */
  public interface TagsSelectListener {
    void onItemSelected(Object item, boolean selected);
  }

  /**
   * Prepare the formatting and appearance of data to be displayed on each item. As it returns
   * {@link CharSequence}, item text can be represented as a {@link android.text.SpannableString}.
   * Avoid using spans which may produce item width change (such as {@link
   * android.text.style.BulletSpan} or {@link android.text.style.RelativeSizeSpan})
   */
  public interface DataTransform<T> {
    CharSequence prepare(T item);
  }

  /**
   * Prepare the formatting and appearance of data to be displayed on each item, for both selected
   * and non-selected state. As it returns {@link CharSequence}, item text can be represented as a
   * {@link android.text.SpannableString}. Avoid using spans which may produce item width change
   * (such as {@link android.text.style.BulletSpan} or {@link android.text.style.RelativeSizeSpan})
   */
  public interface DataStateTransform<T> extends DataTransform<T> {
    CharSequence prepareSelected(T item);
  }

  /**
   * Allows to define whether item should be preselected or not. Returning true (or false) for exact
   * item will cause initial state of this item to be set to selected (or unselected).
   */
  public interface DataSelector<T> {
    boolean preselect(T item);
  }

  private static class SortState {
    boolean hasExtraRows = false;
    int extraCount = 0;
    int rowCount = 0;

    static SortState initState() {
      return new SortState();
    }

    int totalRows() {
      return (hasExtraRows ? extraCount : 0) + rowCount;
    }

    void preserveState(int rowCount, boolean needsExtraRow, int extraCount) {
      this.rowCount = rowCount;
      this.hasExtraRows = needsExtraRow;
      this.extraCount = extraCount;
    }

    void preserveState(int rowCount) {
      preserveState(rowCount, false, 0);
    }

    void release() {
      preserveState(0);
    }
  }
}
