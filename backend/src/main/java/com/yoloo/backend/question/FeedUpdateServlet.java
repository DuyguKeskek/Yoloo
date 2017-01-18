package com.yoloo.backend.question;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.yoloo.backend.account.Account;
import com.yoloo.backend.feed.Feed;
import com.yoloo.backend.feed.FeedItem;
import com.yoloo.backend.follow.Follow;
import io.reactivex.Observable;
import java.io.IOException;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.yoloo.backend.OfyService.ofy;

public class FeedUpdateServlet extends HttpServlet {

  private static final String UPDATE_FEED_QUEUE = "update-feed-queue";
  private static final String URL = "/tasks/update/feed";

  private static final String WEBSAFE_ACCOUNT_ID = "accountId";
  private static final String WEBSAFE_FEED_ITEM_ID = "feedItemId";
  private static final String CREATED = "created";

  public static void addToQueue(String accountId, String feedItemId) {
    Queue queue = QueueFactory.getQueue(UPDATE_FEED_QUEUE);
    queue.addAsync(TaskOptions.Builder
        .withUrl(URL)
        .param(WEBSAFE_ACCOUNT_ID, accountId)
        .param(WEBSAFE_FEED_ITEM_ID, feedItemId));
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp)
      throws ServletException, IOException {

    processRequest(req, resp);
  }

  private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
    final String accountId = req.getParameter(WEBSAFE_ACCOUNT_ID);
    final String feedItemId = req.getParameter(WEBSAFE_FEED_ITEM_ID);

    final Key<Account> accountKey = Key.create(accountId);
    final Key<FeedItem> feedItemKey = Key.create(feedItemId);

    getFollowers(accountKey)
        .map(followerKey -> createFeed(followerKey, feedItemKey))
        .toList()
        .subscribe(feeds -> {
          // Add to user's own feed.
          feeds.add(createFeed(accountKey, feedItemKey));

          ofy().save().entities(feeds).now();
        });
  }

  private Observable<Key<Account>> getFollowers(Key<Account> senderKey) {
    // Get user's followers.
    List<Key<Follow>> followerKeys = ofy().load().type(Follow.class)
        .filter(Follow.FIELD_FOLLOWING_KEY + " =", senderKey)
        .keys().list();

    return Observable.fromIterable(followerKeys).map(Key::<Account>getParent);
  }

  private Feed createFeed(Key<Account> followerKey, Key<FeedItem> feedItemKey) {
    return Feed.builder()
        .parentUserKey(followerKey)
        .feedItemRef(Ref.create(feedItemKey))
        .build();
  }
}
