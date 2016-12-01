package com.yoloo.backend.bookmark;

import com.googlecode.objectify.Key;
import com.yoloo.backend.account.Account;
import com.yoloo.backend.question.Question;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(staticName = "newInstance")
public class BookmarkService {

    public Bookmark create(Key<Question> questionKey, Key<Account> parentKey) {
        return Bookmark.builder()
                .id(questionKey.toWebSafeString())
                .parentUserKey(parentKey)
                .build();
    }
}