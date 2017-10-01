/*
 * Copyright (c) 2016 Aitor Viana Sanchez
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software
 * is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.aitorvs.android.fingerlock;

import android.annotation.TargetApi;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Log;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

@TargetApi(Build.VERSION_CODES.M)
final class Key {
    private static final String TAG = Key.class.getSimpleName();
    private final KeyGenerator keyGenerator;
    private final Cipher cipher;
    private final KeyStore keyStore;
    private final String keyName;

    public Key(@NonNull String keyName) {
        try {

            this.keyStore = KeyStore.getInstance("AndroidKeyStore");
            this.keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            this.keyName = keyName;

        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
            throw new RuntimeException("Failed to get the keyGenerator", e);
        } catch (KeyStoreException e) {
            throw new RuntimeException("Failed to init keyStore", e);
        }

        try {
            this.cipher = Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                    + KeyProperties.BLOCK_MODE_CBC + "/"
                    + KeyProperties.ENCRYPTION_PADDING_PKCS7);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException("Failed to get an instance of Cipher", e);
        }
    }

    public String key() {
        return this.keyName;
    }

    /**
     * Returns whether the key is still valid or the user needs to validate the key prior to
     * authenticate.
     * It simply needs to attempt to encrypt any data with the key that was created earlier.
     * If the data can be encrypted with the key, then the user has logged in within our timeout
     * period. If not, an exception is thrown and it’s time to confirm credentials
     *
     * @return <code>true</code> when key is valid
     * @throws NullKeyException when the key has not been created
     */
    public boolean isKeyValid() throws NullKeyException {

        if (BuildConfig.DEBUG) Log.d(TAG, "initCipher with key " + keyName);

        try {
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(keyName, null /* password */);
            if (secretKey == null) {
                // the key has not been created. Notify so that it can be created for the first
                // time
                throw new NullKeyException();
            }
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);

            // the user has recently authenticated, we get here.
            return true;

        } catch (InvalidKeyException e) {
            return false;
        } catch (KeyStoreException e) {
            throw new RuntimeException("KeyStore not initialized", e);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algorithm for recovering the key cannot be found", e);
        } catch (UnrecoverableKeyException e) {
            throw new RuntimeException("Key cannot be recovered", e);
        } catch (CertificateException | IOException e) {
            throw new RuntimeException("KeyStore load error", e);
        }
    }

    public boolean recreateKey() {
        try {
            keyStore.load(null);


            // Set the alias of the entry in Android KeyStore where the key will appear
            // and the constrains (purposes) in the constructor of the Builder
            keyGenerator.init(new KeyGenParameterSpec.Builder(keyName,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    // Require the user to authenticate with a fingerprint to authorize every use
                    // of the key
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());

            keyGenerator.generateKey();

            Log.d(TAG, String.format("Key \"%s\" recreated", keyName));

            return true;

        } catch (IllegalArgumentException | IOException | NoSuchAlgorithmException | CertificateException | InvalidAlgorithmParameterException e) {
            Log.e(TAG, "recreateKey: ", e);
            return false;
        }
    }

    @Override
    public String toString() {
        return "Key{" +
                "keyName=" + keyName +
                "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Key)) return false;
        Key other = (Key) o;
        return keyName.equals(other.keyName)
                && keyStore == other.keyStore
                && keyGenerator == other.keyGenerator
                && cipher == other.cipher;
    }
}
