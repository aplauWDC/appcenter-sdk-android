package com.microsoft.appcenter.utils.storage;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;

import com.microsoft.appcenter.http.ServiceCallback;
import com.microsoft.appcenter.ingestion.models.LogContainer;
import com.microsoft.appcenter.utils.UUIDUtils;
import com.microsoft.appcenter.utils.context.AuthTokenContext;
import com.microsoft.appcenter.utils.context.AuthTokenInfo;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

@SmallTest
@RunWith(AndroidJUnit4.class)
public class AuthTokenTest {

    @Test
    public void authTokenTest() {
        String token = UUIDUtils.randomUUID().toString();
        Date startTime = new Date();
        Date endTime = new Date();
        AuthTokenInfo tokenTest = new AuthTokenInfo(token, startTime, endTime);
        assertEquals(tokenTest.getAuthToken(), token);
        assertEquals(tokenTest.getStartTime(), startTime);
        assertEquals(tokenTest.getEndTime(), endTime);
    }

    @Test
    public void authTokenStorageTest(){
        AuthTokenContext tokenContext = new AuthTokenContext();
        AuthTokenStorage authTokenStorage = mock(AuthTokenStorage.class);
        tokenContext.setStorage(authTokenStorage);
        assertEquals(tokenContext.getStorage(), authTokenStorage);

    }
}
