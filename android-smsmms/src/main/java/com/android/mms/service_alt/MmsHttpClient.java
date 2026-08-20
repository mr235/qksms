/*
 * Copyright (C) 2015 Jacob Klinker
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.mms.service_alt;

import android.content.Context;
import android.text.TextUtils;
import com.android.mms.service_alt.exception.MmsHttpException;
import okhttp3.ConnectionPool;
import okhttp3.ConnectionSpec;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import timber.log.Timber;

import javax.net.SocketFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MMS HTTP client for sending and downloading MMS messages
 */
public class MmsHttpClient {

    public static final String METHOD_POST = "POST";
    public static final String METHOD_GET = "GET";

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";
    private static final String HEADER_USER_AGENT = "User-Agent";

    // The "Accept" header value
    private static final String HEADER_VALUE_ACCEPT =
            "*/*, application/vnd.wap.mms-message, application/vnd.wap.sic";
    // The "Content-Type" header value
    private static final String HEADER_VALUE_CONTENT_TYPE_WITH_CHARSET =
            "application/vnd.wap.mms-message; charset=utf-8";
    private static final String HEADER_VALUE_CONTENT_TYPE_WITHOUT_CHARSET =
            "application/vnd.wap.mms-message";

    private final Context mContext;
    private final SocketFactory mSocketFactory;
    private final MmsNetworkManager mHostResolver;
    private final ConnectionPool mConnectionPool;

    /**
     * Constructor
     *
     * @param context The Context object
     * @param socketFactory The socket factory for creating an OKHttp client
     * @param hostResolver The host name resolver for creating an OKHttp client
     * @param connectionPool The connection pool for creating an OKHttp client
     */
    public MmsHttpClient(Context context, SocketFactory socketFactory, MmsNetworkManager hostResolver,
            ConnectionPool connectionPool) {
        mContext = context;
        mSocketFactory = socketFactory;
        mHostResolver = hostResolver;
        mConnectionPool = connectionPool;
    }

    /**
     * Execute an MMS HTTP request, either a POST (sending) or a GET (downloading)
     *
     * @param urlString The request URL, for sending it is usually the MMSC, and for downloading
     *                  it is the message URL
     * @param pdu For POST (sending) only, the PDU to send
     * @param method HTTP method, POST for sending and GET for downloading
     * @param isProxySet Is there a proxy for the MMSC
     * @param proxyHost The proxy host
     * @param proxyPort The proxy port
     * @param mmsConfig The MMS config to use
     * @return The HTTP response body
     * @throws MmsHttpException For any failures
     */
    public byte[] execute(String urlString, byte[] pdu, String method, boolean isProxySet,
            String proxyHost, int proxyPort, MmsConfig.Overridden mmsConfig)
            throws MmsHttpException {
        Timber.d("HTTP: " + method + " " + urlString
                + (isProxySet ? (", proxy=" + proxyHost + ":" + proxyPort) : "")
                + ", PDU size=" + (pdu != null ? pdu.length : 0));
        checkMethod(method);
        try {
            Proxy proxy = null;
            if (isProxySet) {
                proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
            }
            final URL url = new URL(urlString);
            final OkHttpClient okHttpClient = buildClient(url, proxy, mmsConfig);

            // ------- COMMON HEADERS ---------
            final Request.Builder requestBuilder = new Request.Builder().url(url);
            // Header: Accept
            requestBuilder.header(HEADER_ACCEPT, HEADER_VALUE_ACCEPT);
            // Header: Accept-Language
            requestBuilder.header(
                    HEADER_ACCEPT_LANGUAGE, getCurrentAcceptLanguage(Locale.getDefault()));
            // Header: User-Agent
            final String userAgent = mmsConfig.getUserAgent();
            Timber.i("HTTP: User-Agent=" + userAgent);
            requestBuilder.header(HEADER_USER_AGENT, userAgent);
            // Header: x-wap-profile
            final String uaProfUrlTagName = mmsConfig.getUaProfTagName();
            final String uaProfUrl = mmsConfig.getUaProfUrl();
            if (uaProfUrl != null) {
                Timber.i("HTTP: UaProfUrl=" + uaProfUrl);
                requestBuilder.header(uaProfUrlTagName, uaProfUrl);
            }
            // Add extra headers specified by mms_config.xml's httpparams
            addExtraHeaders(requestBuilder, mmsConfig);
            // Different stuff for GET and POST
            if (METHOD_POST.equals(method)) {
                if (pdu == null || pdu.length < 1) {
                    Timber.e("HTTP: empty pdu");
                    throw new MmsHttpException(0/*statusCode*/, "Sending empty PDU");
                }
                final String contentType = mmsConfig.getSupportHttpCharsetHeader()
                        ? HEADER_VALUE_CONTENT_TYPE_WITH_CHARSET
                        : HEADER_VALUE_CONTENT_TYPE_WITHOUT_CHARSET;
                requestBuilder.header(HEADER_CONTENT_TYPE, contentType);
                requestBuilder.post(RequestBody.create(pdu, MediaType.parse(contentType)));
            } else if (METHOD_GET.equals(method)) {
                requestBuilder.get();
            }

            final Request request = requestBuilder.build();
            logHttpHeaders(request.headers().toMultimap());

            try (Response response = okHttpClient.newCall(request).execute()) {
                final int responseCode = response.code();
                final String responseMessage = response.message();
                Timber.d("HTTP: " + responseCode + " " + responseMessage);
                logHttpHeaders(response.headers().toMultimap());
                if (responseCode / 100 != 2) {
                    throw new MmsHttpException(responseCode, responseMessage);
                }
                final ResponseBody body = response.body();
                final byte[] responseBody = body != null ? body.bytes() : new byte[0];
                Timber.d("HTTP: response size=" + responseBody.length);
                return responseBody;
            }
        } catch (MalformedURLException e) {
            Timber.e(e, "HTTP: invalid URL " + urlString);
            throw new MmsHttpException(0/*statusCode*/, "Invalid URL " + urlString, e);
        } catch (IOException e) {
            Timber.e(e, "HTTP: IO failure");
            throw new MmsHttpException(0/*statusCode*/, e);
        }
    }

    /**
     * Build an OkHttpClient configured for MMS transport
     *
     * TODO: The following code is borrowed from android.net.Network.openConnection
     * Once that method supports proxy, we should use that instead
     * Also we should remove the associated HostResolver and ConnectionPool from
     * MmsNetworkManager
     *
     * @param url The URL to connect to
     * @param proxy The proxy to use
     * @param mmsConfig The MMS config to use
     * @return The configured OkHttpClient
     * @throws MalformedURLException If URL protocol is unsupported
     */
    private OkHttpClient buildClient(URL url, final Proxy proxy, MmsConfig.Overridden mmsConfig)
            throws MalformedURLException {
        final String protocol = url.getProtocol();
        if (!protocol.equals("http") && !protocol.equals("https")) {
            throw new MalformedURLException("Invalid URL or unrecognized protocol " + protocol);
        }
        final int timeout = mmsConfig.getHttpSocketTimeout();
        final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .followRedirects(false)
                .protocols(Collections.singletonList(Protocol.HTTP_1_1))
                .connectionSpecs(Arrays.asList(ConnectionSpec.CLEARTEXT, ConnectionSpec.MODERN_TLS))
                .connectionPool(mConnectionPool)
                .dns(mHostResolver)
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .writeTimeout(timeout, TimeUnit.MILLISECONDS);
        if (proxy != null) {
            builder.proxy(proxy);
        }
        if (protocol.equals("http")) {
            builder.socketFactory(mSocketFactory);
        }
        return builder.build();
    }

    private static void logHttpHeaders(Map<String, List<String>> headers) {
        final StringBuilder sb = new StringBuilder();
        if (headers != null) {
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                final String key = entry.getKey();
                final List<String> values = entry.getValue();
                if (values != null) {
                    for (String value : values) {
                        sb.append(key).append('=').append(value).append('\n');
                    }
                }
            }
            Timber.v("HTTP: headers\n" + sb.toString());
        }
    }

    private static void checkMethod(String method) throws MmsHttpException {
        if (!METHOD_GET.equals(method) && !METHOD_POST.equals(method)) {
            throw new MmsHttpException(0/*statusCode*/, "Invalid method " + method);
        }
    }

    private static final String ACCEPT_LANG_FOR_US_LOCALE = "en-US";

    /**
     * Return the Accept-Language header.  Use the current locale plus
     * US if we are in a different locale than US.
     * This code copied from the browser's WebSettings.java
     *
     * @return Current AcceptLanguage String.
     */
    public static String getCurrentAcceptLanguage(Locale locale) {
        final StringBuilder buffer = new StringBuilder();
        addLocaleToHttpAcceptLanguage(buffer, locale);

        if (!Locale.US.equals(locale)) {
            if (buffer.length() > 0) {
                buffer.append(", ");
            }
            buffer.append(ACCEPT_LANG_FOR_US_LOCALE);
        }

        return buffer.toString();
    }

    /**
     * Convert obsolete language codes, including Hebrew/Indonesian/Yiddish,
     * to new standard.
     */
    private static String convertObsoleteLanguageCodeToNew(String langCode) {
        if (langCode == null) {
            return null;
        }
        if ("iw".equals(langCode)) {
            // Hebrew
            return "he";
        } else if ("in".equals(langCode)) {
            // Indonesian
            return "id";
        } else if ("ji".equals(langCode)) {
            // Yiddish
            return "yi";
        }
        return langCode;
    }

    private static void addLocaleToHttpAcceptLanguage(StringBuilder builder, Locale locale) {
        final String language = convertObsoleteLanguageCodeToNew(locale.getLanguage());
        if (language != null) {
            builder.append(language);
            final String country = locale.getCountry();
            if (country != null) {
                builder.append("-");
                builder.append(country);
            }
        }
    }

    private static final Pattern MACRO_P = Pattern.compile("##(\\S+)##");
    /**
     * Resolve the macro in HTTP param value text
     * For example, "something##LINE1##something" is resolved to "something9139531419something"
     *
     * @param value The HTTP param value possibly containing macros
     * @return The HTTP param with macro resolved to real value
     */
    private static String resolveMacro(Context context, String value,
            MmsConfig.Overridden mmsConfig) {
        if (TextUtils.isEmpty(value)) {
            return value;
        }
        final Matcher matcher = MACRO_P.matcher(value);
        int nextStart = 0;
        StringBuilder replaced = null;
        while (matcher.find()) {
            if (replaced == null) {
                replaced = new StringBuilder();
            }
            final int matchedStart = matcher.start();
            if (matchedStart > nextStart) {
                replaced.append(value.substring(nextStart, matchedStart));
            }
            final String macro = matcher.group(1);
            final String macroValue = mmsConfig.getHttpParamMacro(context, macro);
            if (macroValue != null) {
                replaced.append(macroValue);
            } else {
                Timber.w("HTTP: invalid macro " + macro);
            }
            nextStart = matcher.end();
        }
        if (replaced != null && nextStart < value.length()) {
            replaced.append(value.substring(nextStart));
        }
        return replaced == null ? value : replaced.toString();
    }

    /**
     * Add extra HTTP headers from mms_config.xml's httpParams, which is a list of key/value
     * pairs separated by "|". Each key/value pair is separated by ":". Value may contain
     * macros like "##LINE1##" or "##NAI##" which is resolved with methods in this class
     *
     * @param connection The HttpURLConnection that we add headers to
     * @param mmsConfig The MmsConfig object
     */
    private void addExtraHeaders(Request.Builder requestBuilder, MmsConfig.Overridden mmsConfig) {
        final String extraHttpParams = mmsConfig.getHttpParams();
        if (!TextUtils.isEmpty(extraHttpParams)) {
            // Parse the parameter list
            String paramList[] = extraHttpParams.split("\\|");
            for (String paramPair : paramList) {
                String splitPair[] = paramPair.split(":", 2);
                if (splitPair.length == 2) {
                    final String name = splitPair[0].trim();
                    final String value = resolveMacro(mContext, splitPair[1].trim(), mmsConfig);
                    if (!TextUtils.isEmpty(name) && !TextUtils.isEmpty(value)) {
                        // Add the header if the param is valid
                        requestBuilder.header(name, value);
                    }
                }
            }
        }
    }
}
