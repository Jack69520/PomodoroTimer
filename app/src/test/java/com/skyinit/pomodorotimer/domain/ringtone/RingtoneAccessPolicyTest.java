package com.skyinit.pomodorotimer.domain.ringtone;

import android.Manifest;
import android.content.Context;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class RingtoneAccessPolicyTest {

    private Context context;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        RingtoneAccessPromptGate.resetForTests();
        Shadows.shadowOf(context.getPackageManager())
                .denyPermission(Manifest.permission.READ_MEDIA_AUDIO);
    }

    @Test
    public void defaultPreference_notDegraded() {
        RingtoneAccessSnapshot snap = RingtoneAccessPolicy.evaluate(context, "default");
        assertEquals(RingtoneAccessSnapshot.Option.DEFAULT, snap.option);
        assertFalse(snap.degraded);
        assertEquals("default", snap.syncKey);
        assertNotNull(snap.effectiveUri);
    }

    @Test
    public void silentPreference_nullEffectiveUri() {
        RingtoneAccessSnapshot snap = RingtoneAccessPolicy.evaluate(context, "silent");
        assertEquals(RingtoneAccessSnapshot.Option.SILENT, snap.option);
        assertFalse(snap.degraded);
        assertNull(snap.effectiveUri);
        assertEquals("silent", snap.syncKey);
    }

    @Test
    public void customWithoutPermission_degradedFallback() {
        String custom = "content://media/external/audio/media/42";
        RingtoneAccessSnapshot snap = RingtoneAccessPolicy.evaluate(context, custom);
        assertEquals(RingtoneAccessSnapshot.Option.CUSTOM, snap.option);
        assertTrue(snap.degraded);
        assertFalse(snap.hasAudioReadAccess);
        assertEquals(custom + "|fallback", snap.syncKey);
        Uri expectedDefault = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        assertEquals(expectedDefault, snap.effectiveUri);
    }

    @Test
    public void customWithPermission_usesPreferenceUri() {
        Shadows.shadowOf(context.getPackageManager())
                .grantPermission(context.getPackageName(), Manifest.permission.READ_MEDIA_AUDIO);
        String custom = "content://media/external/audio/media/99";
        RingtoneAccessSnapshot snap = RingtoneAccessPolicy.evaluate(context, custom);
        assertFalse(snap.degraded);
        assertTrue(snap.hasAudioReadAccess);
        assertEquals(Uri.parse(custom), snap.effectiveUri);
        assertEquals(custom + "|ok", snap.syncKey);
    }

    @Test
    public void syncKeyChangesWhenPermissionFlips() {
        String custom = "content://media/external/audio/media/7";
        String deniedKey = RingtoneAccessPolicy.effectiveSyncKey(context, custom);
        Shadows.shadowOf(context.getPackageManager())
                .grantPermission(context.getPackageName(), Manifest.permission.READ_MEDIA_AUDIO);
        String grantedKey = RingtoneAccessPolicy.effectiveSyncKey(context, custom);
        assertEquals(custom + "|fallback", deniedKey);
        assertEquals(custom + "|ok", grantedKey);
        assertTrue(!deniedKey.equals(grantedKey));
    }

    @Test
    public void invalidCustomUri_fallsBackToDefaultWhenGranted() {
        Shadows.shadowOf(context.getPackageManager())
                .grantPermission(context.getPackageName(), Manifest.permission.READ_MEDIA_AUDIO);
        RingtoneAccessSnapshot snap = RingtoneAccessPolicy.evaluate(context, "not-a-uri");
        assertEquals(RingtoneAccessSnapshot.Option.CUSTOM, snap.option);
        assertFalse(snap.degraded);
        assertEquals("not-a-uri|invalid", snap.syncKey);
        assertEquals(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), snap.effectiveUri);
    }

    @Test
    @Config(sdk = 30)
    public void api30_usesMediaFilesKind() {
        assertEquals(
                com.skyinit.pomodorotimer.util.SettingsPermissionHelper.Kind.MEDIA_FILES,
                RingtoneAccessPolicy.audioReadKind());
    }

    @Test
    public void api34_usesMusicAudioKind() {
        assertEquals(
                com.skyinit.pomodorotimer.util.SettingsPermissionHelper.Kind.MUSIC_AUDIO,
                RingtoneAccessPolicy.audioReadKind());
    }

    @Test
    public void promptGate_claimsOncePerProcess() {
        assertTrue(RingtoneAccessPromptGate.tryClaimColdStartPrompt());
        assertFalse(RingtoneAccessPromptGate.tryClaimColdStartPrompt());
        assertTrue(RingtoneAccessPromptGate.wasColdStartPrompted());
    }
}
