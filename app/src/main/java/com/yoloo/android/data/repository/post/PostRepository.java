package com.yoloo.android.data.repository.post;

import com.annimon.stream.Optional;
import com.yoloo.android.data.Response;
import com.yoloo.android.data.model.MediaRealm;
import com.yoloo.android.data.model.PostRealm;
import com.yoloo.android.data.sorter.PostSorter;
import com.yoloo.backend.yolooApi.model.Media;
import com.yoloo.backend.yolooApi.model.Size;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import timber.log.Timber;

public class PostRepository {

  private static PostRepository instance;

  private final PostRemoteDataStore remoteDataStore;
  private final PostDiskDataStore diskDataStore;

  private PostRepository(PostRemoteDataStore remoteDataStore, PostDiskDataStore diskDataStore) {
    this.remoteDataStore = remoteDataStore;
    this.diskDataStore = diskDataStore;
  }

  /**
   * Gets instance.
   *
   * @param remoteDataStore the remote data store
   * @param diskDataStore the disk data store
   * @return the instance
   */
  public static PostRepository getInstance(PostRemoteDataStore remoteDataStore,
      PostDiskDataStore diskDataStore) {
    if (instance == null) {
      instance = new PostRepository(remoteDataStore, diskDataStore);
    }
    return instance;
  }

  /**
   * Gets post.
   *
   * @param postId the post id
   * @return the post
   */
  public Observable<Optional<PostRealm>> getPost(@Nonnull String postId) {
    return Observable.mergeDelayError(
        diskDataStore.get(postId).subscribeOn(Schedulers.io()).toObservable(), remoteDataStore
            .get(postId)
            .doOnSuccess(diskDataStore::add)
            .map(Optional::of)
            .toObservable()
            .subscribeOn(Schedulers.io()));
  }

  /**
   * Add post single.
   *
   * @param post the post
   * @return the single
   */
  public Single<PostRealm> addPost(@Nonnull PostRealm post) {
    return remoteDataStore.add(post).doOnSuccess(diskDataStore::add).subscribeOn(Schedulers.io());
  }

  /**
   * Delete post completable.
   *
   * @param postId the post id
   * @return the completable
   */
  public Completable deletePost(@Nonnull String postId) {
    return remoteDataStore
        .delete(postId)
        .andThen(diskDataStore.delete(postId))
        .subscribeOn(Schedulers.io());
  }

  /**
   * Gets draft.
   *
   * @return the draft
   */
  public Single<PostRealm> getDraft() {
    return diskDataStore
        .get("draft")
        .subscribeOn(Schedulers.io())
        .filter(Optional::isPresent)
        .map(Optional::get)
        .toSingle();
  }

  /**
   * Add draft completable.
   *
   * @param draft the draft
   * @return the completable
   */
  public Completable addDraft(@Nonnull PostRealm draft) {
    return Completable.fromAction(() -> diskDataStore.add(draft)).subscribeOn(Schedulers.io());
  }

  /**
   * Delete draft completable.
   *
   * @return the completable
   */
  public Completable deleteDraft() {
    return diskDataStore.delete("draft").subscribeOn(Schedulers.io());
  }

  /**
   * List by feed observable.
   *
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByFeed(@Nullable String cursor, int limit) {
    Timber.d("listByFeed()");
    Observable<Response<List<PostRealm>>> remoteObservable = remoteDataStore
        .listByFeed(cursor, limit)
        .doOnNext(response -> diskDataStore.addAll(response.getData()))
        .subscribeOn(Schedulers.io())
        .doOnNext(response -> Timber.d("Response: %s", response.getData()));

    Observable<Response<List<PostRealm>>> diskObservable =
        diskDataStore.listByFeed().subscribeOn(Schedulers.io());

    return diskObservable.flatMap(response -> {
      if (response.getData().isEmpty()) {
        return remoteObservable;
      }

      return Observable.just(response);
    });
  }

  /**
   * List by bounty observable.
   *
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByBounty(@Nullable String cursor, int limit) {
    //return remoteDataStore.listByBounty(cursor, limit).subscribeOn(Schedulers.io());
    return diskDataStore.listByBounty().subscribeOn(Schedulers.io());
  }

  /**
   * List by group observable.
   *
   * @param groupId the group id
   * @param sorter the sorter
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByGroup(@Nonnull String groupId,
      @Nonnull PostSorter sorter, @Nullable String cursor, int limit) {
    return remoteDataStore.listByGroup(groupId, sorter, cursor, limit).subscribeOn(Schedulers.io());
  }

  /**
   * List by tags observable.
   *
   * @param tagNames the tag names
   * @param sorter the sorter
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByTags(@Nonnull String tagNames,
      @Nonnull PostSorter sorter, @Nullable String cursor, int limit) {
    return remoteDataStore.listByTags(tagNames, sorter, cursor, limit).subscribeOn(Schedulers.io());
  }

  /**
   * List by user observable.
   *
   * @param userId the user id
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByUser(@Nonnull String userId,
      @Nullable String cursor, int limit) {
    return remoteDataStore.listByUser(userId, cursor, limit).subscribeOn(Schedulers.io());
  }

  /**
   * List by bookmarked observable.
   *
   * @param cursor the cursor
   * @param limit the limit
   * @return the observable
   */
  public Observable<Response<List<PostRealm>>> listByBookmarked(@Nullable String cursor,
      int limit) {
    return Observable.mergeDelayError(
        diskDataStore.listByBookmarkedPosts().subscribeOn(Schedulers.io()), remoteDataStore
            .listByBookmarked(cursor, limit)
            .doOnNext(response -> diskDataStore.addAll(response.getData()))
            .subscribeOn(Schedulers.io()));
  }

  public Observable<Response<List<PostRealm>>> listByTrendingBlogPosts(@Nullable String cursor,
      int limit) {
    /*Observable<Response<List<PostRealm>>> diskObservable =
        diskDataStore.listByTrendingBlogPosts().subscribeOn(Schedulers.io());

    Observable<Response<List<PostRealm>>> remoteObservable =
        remoteDataStore.listByTrendingBlogs(cursor, limit)
            .doOnNext(response -> diskDataStore.addTrendingBlogs(response.getData()))
            .subscribeOn(Schedulers.io());*/

    Size thumb = new Size().setUrl(
        "https://s3.amazonaws.com/fathom_media/cache/c7/a5/c7a5ece97078e394379c9648067e6ac5.jpg");

    Size mini = new Size().setUrl(
        "https://s3.amazonaws.com/fathom_media/cache/c7/a5/c7a5ece97078e394379c9648067e6ac5.jpg");

    Size low = new Size().setUrl(
        "https://s3.amazonaws.com/fathom_media/cache/c7/a5/c7a5ece97078e394379c9648067e6ac5.jpg");

    Size medium = new Size().setUrl(
        "https://s3.amazonaws.com/fathom_media/cache/c7/a5/c7a5ece97078e394379c9648067e6ac5.jpg");

    Size large = new Size().setUrl(
        "https://s3.amazonaws.com/fathom_media/cache/c7/a5/c7a5ece97078e394379c9648067e6ac5.jpg");

    List<Size> sizes = new ArrayList<>();
    sizes.add(thumb);
    sizes.add(mini);
    sizes.add(low);
    sizes.add(medium);
    sizes.add(large);

    Media dto = new Media().setSizes(sizes);

    MediaRealm media = new MediaRealm(dto);

    PostRealm p1 = new PostRealm()
        .setId(UUID.randomUUID().toString())
        .setUsername("krialix")
        .setOwnerId(UUID.randomUUID().toString())
        .setAvatarUrl(
            "https://lh3.googleusercontent.com/Yd1ER2rR_nOU__3NctmXtHCPtPnhMydcvr8jykZSAhB_lSGdJsjTLw8f0sOTPfMqH-51ndZ_-f3MiWsx8lmn_we5=s150-c")
        .setTitle("Your Collage Degree is Worthless")
        .setContent("This is not a judgement, it is just a fact that I faced.")
        .addMedia(media)
        .setCreated(new Date());

    PostRealm p2 = new PostRealm()
        .setId(UUID.randomUUID().toString())
        .setUsername("krialix")
        .setOwnerId(UUID.randomUUID().toString())
        .setAvatarUrl(
            "https://lh3.googleusercontent.com/Yd1ER2rR_nOU__3NctmXtHCPtPnhMydcvr8jykZSAhB_lSGdJsjTLw8f0sOTPfMqH-51ndZ_-f3MiWsx8lmn_we5=s150-c")
        .setTitle("An Alternative history of 'Third Eye Blind'")
        .setContent("This is not a judgement, it is just a fact that I faced.")
        .addMedia(media)
        .setCreated(new Date());

    PostRealm p3 = new PostRealm()
        .setId(UUID.randomUUID().toString())
        .setUsername("krialix")
        .setOwnerId(UUID.randomUUID().toString())
        .setAvatarUrl(
            "https://lh3.googleusercontent.com/Yd1ER2rR_nOU__3NctmXtHCPtPnhMydcvr8jykZSAhB_lSGdJsjTLw8f0sOTPfMqH-51ndZ_-f3MiWsx8lmn_we5=s150-c")
        .setTitle("Almanya'da dikkat edilmedi gereken 3 şey")
        .setContent("This is not a judgement, it is just a fact that I faced.")
        .addMedia(media)
        .setCreated(new Date());

    List<PostRealm> posts = new ArrayList<>();
    posts.add(p1);
    posts.add(p2);
    posts.add(p3);

    return Observable.just(Response.create(posts, null));

    //return Observable.mergeDelayError(diskObservable, remoteObservable);
  }

  /**
   * Vote post completable.
   *
   * @param postId the post id
   * @param direction the direction
   * @return the completable
   */
  public Completable votePost(@Nonnull String postId, int direction) {
    return remoteDataStore
        .vote(postId, direction)
        .andThen(diskDataStore.vote(postId, direction).subscribeOn(Schedulers.io()));
  }

  /**
   * Bookmark post completable.
   *
   * @param postId the post id
   * @return the completable
   */
  public Completable bookmarkPost(@Nonnull String postId) {
    return remoteDataStore
        .bookmark(postId)
        .andThen(diskDataStore.bookmark(postId).subscribeOn(Schedulers.io()));
  }

  /**
   * Un bookmark post completable.
   *
   * @param postId the post id
   * @return the completable
   */
  public Completable unBookmarkPost(@Nonnull String postId) {
    return remoteDataStore
        .unbookmark(postId)
        .andThen(diskDataStore.unBookmark(postId))
        .subscribeOn(Schedulers.io());
  }
}
