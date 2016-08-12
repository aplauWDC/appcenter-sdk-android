package avalanche.core;

import android.content.Context;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import avalanche.core.channel.AvalancheChannel;
import avalanche.core.utils.StorageHelper;

import static avalanche.core.utils.PrefStorageConstants.KEY_ENABLED;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@SuppressWarnings("unused")
@RunWith(PowerMockRunner.class)
@PrepareForTest(StorageHelper.PreferencesStorage.class)
public class AbstractAvalancheFeatureTest {

    private AbstractAvalancheFeature feature;

    @Before
    public void setUp() {
        feature = new AbstractAvalancheFeature() {
            @Override
            protected String getGroupName() {
                return "group_test";
            }
        };

        /* First call to avalanche.isInstanceEnabled shall return true, initial state. */
        mockStatic(StorageHelper.PreferencesStorage.class);
        final String key = KEY_ENABLED + "_group_test";
        when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(true);

        /* Then simulate further changes to state. */
        PowerMockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {

                /* Whenever the new state is persisted, make further calls return the new state. */
                boolean enabled = (Boolean) invocation.getArguments()[1];
                when(StorageHelper.PreferencesStorage.getBoolean(key, true)).thenReturn(enabled);
                return null;
            }
        }).when(StorageHelper.PreferencesStorage.class);
        StorageHelper.PreferencesStorage.putBoolean(eq(key), anyBoolean());
    }

    @Test
    public void onActivityCreated() {
        feature.onActivityCreated(null, null);
    }

    @Test
    public void onActivityStarted() {
        feature.onActivityStarted(null);
    }

    @Test
    public void onActivityResumed() {
        feature.onActivityResumed(null);
    }

    @Test
    public void onActivityPaused() {
        feature.onActivityPaused(null);
    }

    @Test
    public void onActivityStopped() {
        feature.onActivityStopped(null);
    }

    @Test
    public void onActivitySaveInstanceState() {
        feature.onActivitySaveInstanceState(null, null);
    }

    @Test
    public void onActivityDestroyed() {
        feature.onActivityDestroyed(null);
    }

    @Test
    public void enabled() {
        Assert.assertTrue(feature.isInstanceEnabled());
        feature.setInstanceEnabled(false);
        Assert.assertFalse(feature.isInstanceEnabled());
    }

    @Test
    public void getLogFactories() {
        Assert.assertNull(null, feature.getLogFactories());
    }

    @Test
    public void onChannelReady() {
        AvalancheChannel channel = mock(AvalancheChannel.class);
        feature.onChannelReady(mock(Context.class), channel);
        Assert.assertSame(channel, feature.mChannel);
    }

    @Test
    public void getGroupName() {
        Assert.assertEquals("group_test", feature.getGroupName());
    }
}